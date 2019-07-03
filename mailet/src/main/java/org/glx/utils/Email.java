package org.glx.utils;

import org.apache.mailet.MailetContext;
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

public class Email {

    public static void saveMail(MailetContext mailetContext, String toEmail, String mailName, int gatewayId, String emailFrom, int mailStatusId){
        Connection connection = DatabaseConnection.getConnection();
        Calendar calendar = Calendar.getInstance();
        Timestamp sentAt = new Timestamp(calendar.getTime().getTime());

        String query = "INSERT INTO glx_mails (gateway, mail_name, to_email, sent_at, from_email, status_id) ";
        query = query.concat("VALUES (?, ?, ?, ?, ?, ?);");

        PreparedStatement preparedStatement = null;
        try{
            preparedStatement = connection.prepareStatement(query);
        }
        catch(SQLException sqlException){
            mailetContext.log("Email: error creating the insert for the email " + toEmail + " on the gateway " + Integer.toString(gatewayId));
        }
        try{
            preparedStatement.setInt(1, gatewayId);
            preparedStatement.setString(2, mailName);
            preparedStatement.setString(3, toEmail);
            preparedStatement.setTimestamp(4, sentAt);
            preparedStatement.setString(5, emailFrom);
            preparedStatement.setInt(6, mailStatusId);
            preparedStatement.executeUpdate();
        }
        catch(SQLException sqlException){
            mailetContext.log("Email: error inserting the email " + toEmail + " on the gateway " + Integer.toString(gatewayId));
        }
    }

    public static String getContent(MimePart part, MailetContext mailetContext) throws MessagingException, IOException {
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
            mailetContext.log("GLXSendgridRelay: related");
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            MimeBodyPart firstPart = (MimeBodyPart) multipart.getBodyPart(0);
            return getContent(firstPart, mailetContext);
        } else if (part.isMimeType("multipart/alternative")) {
            mailetContext.log("GLXSendgridRelay: multipart");
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            int count = multipart.getCount();
            for (int index = 0; index < count; index++) {
                MimeBodyPart mimeBodyPart = (MimeBodyPart) multipart.getBodyPart(index);
                if(mimeBodyPart.isMimeType("text/html")){
                    return getContent(mimeBodyPart, mailetContext);
                }
            }
            return "";
        }
        return "";
    }
}
