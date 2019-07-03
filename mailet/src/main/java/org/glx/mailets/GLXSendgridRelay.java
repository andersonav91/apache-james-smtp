package org.glx.mailets;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;
import com.sendgrid.SendGrid;
import com.sendgrid.Email;
import com.sendgrid.Content;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.Method;
import java.io.IOException;
import java.sql.Connection;
import java.util.Collection;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.glx.utils.DatabaseConnection;
import org.glx.utils.EmailStatus;

public class GLXSendgridRelay extends GenericMailet {
    private static final String HTML_BR_TAG = "<br />";
    private SendGrid sendGrid = null;
    private String sendGridEmailFrom = "";
    private Connection connection;
    private int gatewayId = 0;
    private int mailStatusId = 0;
    private ArrayList<String> statusLog = null;

    @Override
    public void init() throws MessagingException {
        sendGrid = new SendGrid(getInitParameter("sendgrid_api_key"));
        sendGridEmailFrom = getInitParameter("sendgrid_email_from");
        gatewayId = Integer.parseInt(getInitParameter("gateway_id"));
    }

    @Override
    public String getMailetInfo() {
        return "GLXSendgridRelay Mailet";
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        this.statusLog = new ArrayList<String>();
        this.mailStatusId = EmailStatus.getRelayedStatus();
        statusLog.add("Relayed");
        this.connection = DatabaseConnection.getConnection();
        String mailName = mail.getName();
        // Each all recipients for the email
        Collection<MailAddress> recipients = mail.getRecipients();
        for (MailAddress mailAddress : recipients) {

            Email from = null;
            MimeMessage message = mail.getMessage();

            String [] headerArray = message.getHeader("X-GLX-FROM");
            if (headerArray != null && headerArray.length > 0) {
                if(headerArray[0] != null){
                    // set email from header
                    from = new Email(headerArray[0]);
                }
                else{
                    from = new Email(sendGridEmailFrom);
                }

            }
            else{
                from = new Email(sendGridEmailFrom);
            }

            String subject = mail.getMessage().getSubject();
            Email to = new Email(mailAddress.toString());

            Content content = null;
            try{
                String emailContent = org.glx.utils.Email.getContent(message, this.getMailetContext());
                if(isHtml(emailContent)){
                    content = new Content("text/html", emailContent);
                }
                else{
                    content = new Content("text/plain", emailContent);
                }
            }
            catch(IOException ex){
                log("GLXSendgridRelay: Error getting the content message " + ex.getMessage());
            }

            com.sendgrid.Mail sendGridMail = new com.sendgrid.Mail(from, subject, to, content);
            sendGridMail.addCustomArg("mail_name", mailName);

            // Make sendgrid request
            Request request = new Request();
            try {
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(sendGridMail.build());
                Response response = sendGrid.api(request);
                log("GLXSendgridRelay: Message from " + mail.getSender().toString() + " to "
                        + mailAddress.toString() + ", response " + Integer.toString(response.getStatusCode()) + " "
                        + response.getBody() + " " + response.getHeaders());

            } catch (IOException ex) {
                log("GLXSendgridRelay: Error sending message from sendgrid " + ex.getMessage());
            }
            this.mailStatusId = EmailStatus.getCreatedStatus();
            // Insert the email
            org.glx.utils.Email.saveMail(
                    this.getMailetContext(),
                    mailAddress.toString(),
                    mail.getName(),
                    gatewayId,
                    sendGridEmailFrom,
                    this.mailStatusId
            );
        }
        // Save log for email
        EmailStatus.saveLog(this.getMailetContext(), statusLog, mail.getName());
        // Set mail as ghost mail
        mail.setState(Mail.GHOST);
    }

    public boolean isHtml(String content){
        Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);
        return htmlPattern.matcher(content).matches();
    }
}