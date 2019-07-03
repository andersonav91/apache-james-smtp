package org.glx.mailets;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;
import org.glx.utils.DatabaseConnection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Pattern;
import javax.mail.Authenticator;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Properties;
import java.util.Date;
import javax.mail.Message;

public class GLXGmailRelay extends GenericMailet {

    private static final String HTML_BRJsonFactory_TAG = "<br />";
    private String mailGmailFrom = "";
    private String mailGmailStmp = "";
    private String mailGmailPort = "";
    private String mailGmailPassword = "";
    private Connection connection;
    private int gatewayId = 0;

    @Override
    public void init() throws MessagingException {
        mailGmailFrom = getInitParameter("gmail_email_from");
        mailGmailStmp = getInitParameter("gmail_email_stmp");
        mailGmailPort = getInitParameter("gmail_email_port");
        mailGmailPassword = getInitParameter("gmail_email_password");
        gatewayId = Integer.parseInt(getInitParameter("gateway_id"));
    }

    @Override
    public String getMailetInfo() {
        return "GLXGmailRelay Mailet";
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        this.connection = DatabaseConnection.getConnection();

        // Each all recipients for the email
        Collection<MailAddress> recipients = mail.getRecipients();
        for (MailAddress mailAddress : recipients) {

            Properties props = new Properties();
            props.put("mail.smtp.host", mailGmailStmp); // SMTP Host
            props.put("mail.smtp.port", mailGmailPort); // TLS Port
            props.put("mail.smtp.auth", "true"); // enable authentication
            props.put("mail.smtp.starttls.enable", "true");

            Authenticator auth = new Authenticator() {
                //override the getPasswordAuthentication method
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailGmailFrom, mailGmailPassword);
                }
            };

            Session session = null;
            try{
                session = Session.getInstance(props, auth);
            }catch(SecurityException ex){
                log("GLXGmailRelay: error in signin process " + ex.getMessage());
            }

            try{
                MimeMessage msg = new MimeMessage(session);
                //set message headers
                Content subject = new Content().withData(mail.getMessage().getSubject());
                MimeMessage mimeMessage = mail.getMessage();
                String emailContent = "";
                try{
                    emailContent = getContent(mimeMessage);
                    if(isHtml(emailContent)){
                        msg.setContent(emailContent, "text/HTML; charset=UTF-8");
                        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
                    }
                    else{
                        msg.setText(emailContent, "UTF-8");
                        msg.addHeader("Content-type", "text/plain; charset=UTF-8");
                    }
                }
                catch(IOException ex){
                    log("GLXGmailRelay: Error getting the content message " + ex.getMessage());
                }

                msg.addHeader("format", "flowed");

                msg.addHeader("Content-Transfer-Encoding", "8bit");

                msg.setFrom(new InternetAddress(mailGmailFrom));

                msg.setReplyTo(InternetAddress.parse(mailAddress.toString(), false));

                msg.setSubject(mail.getMessage().getSubject(), "UTF-8");

                msg.setSentDate(new Date());

                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailAddress.toString(), false));

                // Send gmail email
                Transport.send(msg);

                log("GLXGmailRelay: Email Sended");
            }
            catch (Exception ex) {
                log("GLXGmailRelay: Error building the message " + ex.getMessage());
            }

            saveMail(mailAddress.toString(), mail.getName());
        }

        // Set mail as ghost mail
        mail.setState(Mail.GHOST);
    }

    public String getContent(MimePart part) throws MessagingException, IOException{
        String contentType = part.getContentType();
        if (part.getContent() instanceof String) {
            if (part.isMimeType("text/plain")) {
                String content = (String) part.getContent();
                if (! content.equals(null) && ! content.equals("")) {
                    return content;
                }
            } else if (part.isMimeType("text/html")) {
                String content = (String) part.getContent();
                if (! content.equals(null) && ! content.equals("")) {
                    String [] htmlParts = content.split("(?<=</html>)");
                    Document doc;
                    for(String bodyPart : htmlParts){
                        doc = Jsoup.parse(bodyPart);
                        return doc.body().html();
                    }
                    return "";
                }
                return "";
            }
            return "";
        }

        if (part.isMimeType("multipart/mixed")
                || part.isMimeType("multipart/related")) {
            log("GLXSendgridRelay: related");
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            MimeBodyPart firstPart = (MimeBodyPart) multipart.getBodyPart(0);
            return getContent(firstPart);
        } else if (part.isMimeType("multipart/alternative")) {
            log("GLXSendgridRelay: multipart");
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            int count = multipart.getCount();
            for (int index = 0; index < count; index++) {
                MimeBodyPart mimeBodyPart = (MimeBodyPart) multipart.getBodyPart(index);
                if(mimeBodyPart.isMimeType("text/html")){
                    return getContent(mimeBodyPart);
                }
            }
            return "";
        }
        return "";
    }

    public void saveMail(String toEmail, String mailName){
        Calendar calendar = Calendar.getInstance();
        Timestamp sentAt = new Timestamp(calendar.getTime().getTime());

        String query = "INSERT INTO glx_mails (gateway, mail_name, to_email, sent_at, from_email) ";
        query = query.concat("VALUES (?, ?, ?, ?, ?);");

        PreparedStatement preparedStatement = null;
        try{
            preparedStatement = this.connection.prepareStatement(query);
        }
        catch(SQLException sqlException){
            log("GLXSendgridRelay: error creating the insert for the email " + toEmail + " on the gateway " + Integer.toString(gatewayId));
        }
        try{
            preparedStatement.setInt(1, gatewayId);
            preparedStatement.setString(2, mailName);
            preparedStatement.setString(3, toEmail);
            preparedStatement.setTimestamp(4, sentAt);
            preparedStatement.setString(5, mailGmailFrom);
            preparedStatement.executeUpdate();
        }
        catch(SQLException sqlException){
            log("GLXSendgridRelay: error inserting the email " + toEmail + " on the gateway " + Integer.toString(gatewayId));
        }
    }

    public boolean isHtml(String content){
        Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);
        return htmlPattern.matcher(content).matches();
    }
}
