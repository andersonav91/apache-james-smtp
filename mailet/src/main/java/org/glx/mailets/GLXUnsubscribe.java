package org.glx.mailets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.Calendar;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.base.RFC2822Headers;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.sql.ResultSet;
import com.google.common.base.Optional;
import org.glx.utils.DatabaseConnection;

public class GLXUnsubscribe extends GenericMailet {
    private static final String HTML_BR_TAG = "<br />";
    private static final String CARRIAGE_RETURN = "\r\n";
    private static final Pattern BODY_CLOSING_TAG = Pattern.compile("((?i:</body>))");
    private Connection connection;
    private String htmlMessage = "To stop receiving emails, please click <a href='https://unsubscribe.glx.com/unsubscribe/uuid' target='_blank'>here</a>";
    private String textMessage = "To stop receiving emails, please visit https://unsubscribe.glx.com/unsubscribe/uuid";
    private String currentUuid = "";
    private String fromEmail = "";

    @Override
    public void init() throws MessagingException { }

    @Override
    public String getMailetInfo() {
        return "GLXUnsubscribe Mailet";
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        try {
            MimeMessage message = mail.getMessage();
            UUID uuid = UUID.randomUUID();
            this.currentUuid = uuid.toString();
            fromEmail = mail.getSender().toString();
            // map uuid and save
            this.saveRecords(mail.getRecipients());
            if (attachFooter(message)) {
                log("GLXUnsubscribe: init");
                message.saveChanges();
            } else {
                log("Unable to add footer to mail " + mail.getName());
            }
        } catch (UnsupportedEncodingException e) {
            log("UnsupportedEncoding Unable to add footer to mail "
                    + mail.getName());
        } catch (IOException ioe) {
            throw new MessagingException("Could not read message", ioe);
        }
    }

    private boolean existsEmail(String email){
        String query = "SELECT COUNT(email) AS email_count, uuid FROM glx_subscriptions WHERE email = ? AND from_email = ? GROUP BY uuid";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try{
            preparedStatement = this.connection.prepareStatement(query);
        }
        catch(SQLException sqlException){
            log("GLXUnsubscribe: error consulting the query for the email " + email);
        }
        try{
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, fromEmail);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                if(resultSet.getInt("email_count") > 0){
                    this.currentUuid = resultSet.getString("uuid");
                    log("GLXUnsubscribe: the email " + email + " already exists, " + fromEmail);
                    return true;
                }
            }
        }
        catch(SQLException sqlException){
            log("GLXUnsubscribe: error executing the query for the email " + email);
        }
        log("GLXUnsubscribe: the email " + email + " dont exists");
        return false;
    }

    private void saveRecords(Collection<MailAddress> recipients){
        this.connection = DatabaseConnection.getConnection();
        for (MailAddress mailAddress : recipients) {
            if(this.existsEmail(mailAddress.toString())){
                return;
            }
            String receiver = mailAddress.toString();
            Calendar calendar = Calendar.getInstance();
            Timestamp createdAt = new Timestamp(calendar.getTime().getTime());

            String query = "INSERT INTO glx_subscriptions (subscribed, uuid, email, created_at, unsubscribed_at, from_email) ";
            query = query.concat("VALUES (?, ?, ?, ?, ?, ?);");

            PreparedStatement preparedStatement = null;
            try{
                preparedStatement = this.connection.prepareStatement(query);
            }
            catch(SQLException sqlException){
                log("GLXUnsubscribe: error creating the insert for the email " + receiver);
            }
            try{
                preparedStatement.setBoolean(1,true);
                preparedStatement.setString(2, currentUuid.toString());
                preparedStatement.setString(3, receiver);
                preparedStatement.setTimestamp(4, createdAt);
                preparedStatement.setTimestamp(5, null);
                preparedStatement.setString(6, fromEmail);
                preparedStatement.executeUpdate();
            }
            catch(SQLException sqlException){
                log("GLXUnsubscribe: error inserting the email " + receiver + " " + sqlException.getMessage());
            }
        }
    }

    private boolean attachFooter(MimePart part) throws MessagingException, IOException {
        String contentType = part.getContentType();

        if (part.getContent() instanceof String) {
            Optional<String> content = attachFooterToTextPart(part);
            if (content.isPresent()) {
                String emailContent = content.get();
                part.setContent(emailContent, contentType);
                part.setHeader(RFC2822Headers.CONTENT_TYPE, contentType);
                return true;
            }
        }

        if (part.isMimeType("multipart/mixed")
                || part.isMimeType("multipart/related")) {
            log("GLXUnsubscribe: attachFooterToFirstPart");
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            boolean added = attachFooterToFirstPart(multipart);
            if (added) {
                part.setContent(multipart);
            }
            return added;

        } else if (part.isMimeType("multipart/alternative")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            log("GLXUnsubscribe: attachFooterToAllSubparts");
            boolean added = attachFooterToAllSubparts(multipart);
            if (added) {
                part.setContent(multipart);
            }
            return added;
        }
        //Give up... we won't attach the footer to this MimePart
        return false;
    }

    private String attachFooterToText(String content) throws MessagingException,
            IOException {
        StringBuilder builder = new StringBuilder(content);
        ensureTrailingCarriageReturn(content, builder);
        builder.append(getFooterText());
        return builder.toString();
    }

    private void ensureTrailingCarriageReturn(String content, StringBuilder builder) {
        if (!content.endsWith("\n")) {
            builder.append(CARRIAGE_RETURN);
        }
    }

    private String attachFooterToHTML(String content) throws MessagingException,
            IOException {
        Matcher matcher = BODY_CLOSING_TAG.matcher(content);
        if (!matcher.find()) {
            return content + getFooterHTML();
        }
        int insertionIndex = matcher.start(matcher.groupCount() - 1);
        String returnContent = new StringBuilder()
                .append(content.substring(0, insertionIndex))
                .append(getFooterHTML())
                .append(content.substring(insertionIndex, content.length()))
                .toString();
        return returnContent;
    }

    private Optional<String> attachFooterToTextPart(MimePart part) throws MessagingException, IOException {
        String content = (String) part.getContent();
        log("GLXUnsubscribe: part.IsMimeType " + part.getContentType());
        if (part.isMimeType("text/plain")) {
            return Optional.of(attachFooterToText(content));
        } else if (part.isMimeType("text/html")) {
            return Optional.of(attachFooterToHTML(content));
        }
        return Optional.absent();
    }

    private boolean attachFooterToFirstPart(MimeMultipart multipart) throws MessagingException, IOException {
        MimeBodyPart firstPart = (MimeBodyPart) multipart.getBodyPart(0);
        return attachFooter(firstPart);
    }

    private boolean attachFooterToAllSubparts(MimeMultipart multipart) throws MessagingException, IOException {
        int count = multipart.getCount();
        boolean isFooterAttached = false;
        log("GLXUnsubscribe: count multipart " + Integer.toString(count));
        for (int index = 0; index < count; index++) {
            MimeBodyPart mimeBodyPart = (MimeBodyPart) multipart.getBodyPart(index);
            isFooterAttached |= attachFooter(mimeBodyPart);
        }
        return isFooterAttached;
    }

    private String getFooterText() {
        log("GLXUnsubscribe: getFooterText()");
        return textMessage.replaceAll("uuid", this.currentUuid);
    }

    private String getFooterHTML() {
        String text = htmlMessage;
        log("GLXUnsubscribe: getFooterHTML()");
        return HTML_BR_TAG + text.replaceAll(CARRIAGE_RETURN, HTML_BR_TAG).replaceAll("uuid", this.currentUuid);
    }
}