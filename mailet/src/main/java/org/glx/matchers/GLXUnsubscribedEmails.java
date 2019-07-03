package org.glx.matchers;

import org.apache.mailet.base.GenericMatcher;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.glx.utils.DatabaseConnection;

public class GLXUnsubscribedEmails extends GenericMatcher {
    private Connection connection;
    private String fromEmail = "";

    public Collection<MailAddress> match(Mail mail) throws MessagingException {
        this.connection = DatabaseConnection.getConnection();
        fromEmail = mail.getSender().toString();
        List<String> emails = new ArrayList<String>();
        Collection<MailAddress> recipients = mail.getRecipients();
        Collection<MailAddress> returnMailAddresses = new ArrayList<MailAddress>();
        for (MailAddress mailAddress : recipients) {
            emails.add(mailAddress.toString());
        }
        String query = "SELECT email FROM glx_subscriptions WHERE email IN (?) AND subscribed = false AND from_email = ?";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(query);
        } catch (SQLException sqlException) {
            log("GLXUnsubscribedEmails: error consulting the unsubscribed emails list");
        }
        try {
            preparedStatement.setString(1, StringUtils.join(emails, "\",\""));
            preparedStatement.setString(2, fromEmail);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String email = resultSet.getString("email");
                returnMailAddresses.add(new MailAddress(email));
            }
        } catch (SQLException sqlException) {
            log("GLXUnsubscribedEmails: error executing the unsubscribed emails list");
        }

        if (returnMailAddresses.isEmpty()) {
            log("GLXUnsubscribedEmails: empty emails list");
            return null;
        } else {
            log("GLXUnsubscribedEmails: total emails " + Integer.toString(returnMailAddresses.size()));
            return returnMailAddresses;
        }
    }
}
