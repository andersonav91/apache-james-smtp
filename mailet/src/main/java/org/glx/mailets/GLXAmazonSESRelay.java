package org.glx.mailets;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;
import org.glx.utils.DatabaseConnection;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Pattern;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import org.glx.utils.EmailStatus;
import java.sql.*;

public class GLXAmazonSESRelay extends GenericMailet {
    private static final String HTML_BR_TAG = "<br />";
    private String amazonSesEmailFrom = "";
    private Connection connection;
    private int gatewayId = 0;
    private String amazonAccessKey = "";
    private String amazonSecretKey = "";
    private String amazonRegion = "";
    private int mailStatusId = 0;
    private ArrayList<String> statusLog = null;

    @Override
    public void init() throws MessagingException {
        amazonSesEmailFrom = getInitParameter("amazon_ses_email_from");
        amazonAccessKey = getInitParameter("amazon_access_key");
        amazonSecretKey = getInitParameter("amazon_secret_key");
        amazonRegion = getInitParameter("amazon_region");
        gatewayId = Integer.parseInt(getInitParameter("gateway_id"));
    }

    @Override
    public String getMailetInfo() {
        return "GLXAmazonSESRelay Mailet";
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        this.statusLog = new ArrayList<String>();
        this.mailStatusId = EmailStatus.getRelayedStatus();
        statusLog.add("Relayed");
        this.connection = DatabaseConnection.getConnection();
        String mailName = mail.getName();
        String awsSesId = "";
        // Each all recipients for the email
        Collection<MailAddress> recipients = mail.getRecipients();
        for (MailAddress mailAddress : recipients) {
            // Construct an object to contain the recipient address.
            Destination destination = new Destination().withToAddresses(new String[]{mailAddress.toString()});
            // Create the subject and body of the message.
            Content subject = new Content().withData(mail.getMessage().getSubject());
            MimeMessage mimeMessage = mail.getMessage();
            Body body = null;
            try{
                String emailContent = org.glx.utils.Email.getContent(mimeMessage, this.getMailetContext());
                if(isHtml(emailContent)){
                    Content htmlBody = new Content().withData(emailContent);
                    body = new Body().withHtml(htmlBody);
                }
                else{
                    Content textBody = new Content().withData(emailContent);
                    body = new Body().withText(textBody);
                }
            }
            catch(IOException ex){
                log("GLXAmazonSESRelay: Error getting the content message " + ex.getMessage());
            }
            // Create a message with the specified subject and body.
            Message message = new Message().withSubject(subject).withBody(body);

            // Assemble the email.
            SendEmailRequest request = null;

            String [] headerArray = mimeMessage.getHeader("X-GLX-FROM");

            if (headerArray != null && headerArray.length > 0) {
                if(headerArray[0] != null){
                    // set email from header
                    request = new SendEmailRequest().withSource(headerArray[0]).withDestination(destination).withMessage(message);

                }
                else{
                    request = new SendEmailRequest().withSource(amazonSesEmailFrom).withDestination(destination).withMessage(message);
                }

            }
            else{
                request = new SendEmailRequest().withSource(amazonSesEmailFrom).withDestination(destination).withMessage(message);
            }

            request.putCustomRequestHeader("mail_name", mailName);
            request.putCustomQueryParameter("mail_name", mailName);

            try {
                log("GLXAmazonSESRelay: Attempting to send an email through Amazon SES");
                BasicAWSCredentials credentialsProvider = new BasicAWSCredentials(amazonAccessKey, amazonSecretKey);
                // Instantiate an Amazon SES client, which will make the service call with the supplied AWS credentials.
                AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(credentialsProvider))
                        .withRegion(amazonRegion)
                        .build();
                // Send the email.static
                awsSesId = client.sendEmail(request).getMessageId();
            } catch (Exception ex) {
                log("GLXAmazonSESRelay: The email was not sent " + ex.getMessage());
            }

            this.mailStatusId = EmailStatus.getCreatedStatus();
            // Insert the email
            org.glx.utils.Email.saveMail(
                    this.getMailetContext(),
                    mailAddress.toString(),
                    mail.getName(),
                    gatewayId,
                    amazonSesEmailFrom,
                    this.mailStatusId
            );
        }
        // Save log for email
        EmailStatus.saveLog(this.getMailetContext(), statusLog, mail.getName());
        // Save aws ses id
        this.saveAwsSesId(mailName, awsSesId);
        // Set mail as ghost mail
        mail.setState(Mail.GHOST);
    }

    public boolean isHtml(String content){
        Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);
        return htmlPattern.matcher(content).matches();
    }

    public void saveAwsSesId(String mailName, String awsSesId){
        Connection connection = DatabaseConnection.getConnection();
        String query = "SELECT id FROM glx_mails WHERE mail_name = ?;";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int mailId = 0;
        try{
            preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        }
        catch(SQLException sqlException){
            log("GLXAmazonSESRelay: error getting the email");
        }

        try{
            preparedStatement.setString(1, mailName);
            resultSet = preparedStatement.executeQuery();

            int maxEmailsPerDay = 0;
            while (resultSet.next()) {
                mailId = resultSet.getInt("id");
            }
        }
        catch(SQLException sqlException){
            log("GLXAmazonSESRelay: error getting the email id");
        }

        PreparedStatement preparedStatementInsert = null;

        String insertQuery = "INSERT INTO glx_aws_ses_mail_info (ses_id, glx_mail_id, created_at, updated_at) ";
        insertQuery = insertQuery.concat("VALUES (?, ?, ?, ?);");

        Calendar calendar = Calendar.getInstance();
        Timestamp now = new Timestamp(calendar.getTime().getTime());
        Timestamp updated = new Timestamp(calendar.getTime().getTime());

        try{
            preparedStatementInsert = connection.prepareStatement(insertQuery);
        }
        catch(SQLException sqlException){
            log("GLXAmazonSESRelay: error creating the insert the log for " + mailName);
        }
        try{
            preparedStatementInsert.setString(1, awsSesId);
            preparedStatementInsert.setInt(2, mailId);
            preparedStatementInsert.setTimestamp(3, now);
            preparedStatementInsert.setTimestamp(4, updated);
            preparedStatementInsert.executeUpdate();
        }
        catch(SQLException sqlException){
            log("GLXAmazonSESRelay: error inserting the log for " + mailName);
        }
    }
}