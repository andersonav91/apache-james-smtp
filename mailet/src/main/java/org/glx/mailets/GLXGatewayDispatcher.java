package org.glx.mailets;

import org.apache.mailet.MailAddress;
import org.glx.utils.DatabaseConnection;
import org.glx.utils.EmailStatus;
import org.apache.mailet.base.GenericMailet;
import javax.mail.MessagingException;
import org.apache.mailet.Mail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.*;

public class GLXGatewayDispatcher extends GenericMailet {
    private int totalGateways;
    private Connection connection;
    private String gateways = "0";
    private int mailStatusId = 0;
    private ArrayList<String> statusLog = null;

    @Override
    public void init() throws MessagingException {
        gateways = getInitParameter("gateways");
        totalGateways = gateways.split(",").length;
    }

    @Override
    public String getMailetInfo() { return "GLXGatewayDispatcher Mailet"; }

    @Override
    public void service(Mail mail) throws MessagingException {
        this.statusLog = new ArrayList<String>();
        this.connection = DatabaseConnection.getConnection();
        Calendar calendar = Calendar.getInstance();
        Date sentAt = new Date(calendar.getTime().getTime());

        String query = "SELECT COUNT(gateway) AS total_emails, gateway FROM glx_mails WHERE DATE(sent_at) = ? AND gateway IN (" + gateways + ") GROUP BY gateway ORDER BY total_emails ASC;";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try{
            preparedStatement = this.connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        }
        catch(SQLException sqlException){
            log("GLXGatewayDispatcher: error consulting the best gateway");
        }
        try{
            preparedStatement.setDate(1, sentAt);
            resultSet = preparedStatement.executeQuery();
            // num of rows
            resultSet.last();
            int totalRows = resultSet.getRow();
            resultSet.beforeFirst();

            if(totalRows < totalGateways){
                mail.setAttribute("currentGateway", getBestGateway(sentAt));
            }
            else{
                int maxEmailsPerDay = 0;
                while (resultSet.next()) {
                    maxEmailsPerDay = Integer.parseInt(getInitParameter("max_emails_per_day_gateway_" + Integer.parseInt(resultSet.getString("gateway"))));
                    if(resultSet.getInt("total_emails") <= maxEmailsPerDay){
                        mail.setAttribute("currentGateway", resultSet.getString("gateway"));
                        break;
                    }
                    else{
                        statusLog.add("Relayed");
                        this.mailStatusId = EmailStatus.getLimitedStatus();
                    }
                }
            }
            Collection<MailAddress> recipients = mail.getRecipients();
            for (MailAddress mailAddress : recipients) {
                if(this.mailStatusId == EmailStatus.getLimitedStatus()){
                    // Insert the email if the limit is passed
                    org.glx.utils.Email.saveMail(
                        this.getMailetContext(),
                        mailAddress.toString(),
                        mail.getName(),
                        0,
                        "",
                        this.mailStatusId
                    );
                    // Save log for email
                    EmailStatus.saveLog(this.getMailetContext(), statusLog, mail.getName());
                }

            }
        }
        catch(SQLException sqlException){
            log("GLXGatewayDispatcher: error executing the query for the best gateway " + sqlException.getMessage());
        }
    }

    private String getBestGateway(Date sentAt){
        String query = "SELECT gateway FROM glx_mails WHERE DATE(sent_at) = ? AND gateway IN (" + gateways + ");";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try{
            preparedStatement = this.connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        }
        catch(SQLException sqlException){
            log("GLXGatewayDispatcher: error consulting the first gateway");
        }

        List<String> existingGateways = new ArrayList<String>();

        try{
            preparedStatement.setDate(1, sentAt);
            resultSet = preparedStatement.executeQuery();
            // num of rows

            int maxEmailsPerDay = 0;
            while (resultSet.next()) {
                existingGateways.add(resultSet.getString("gateway"));
            }
        }
        catch(SQLException sqlException){
            log("GLXGatewayDispatcher: error executing the query for the first gateway " + sqlException.getMessage());
        }

        List<String> currentGateways = Arrays.asList(gateways.split(","));

        for (String gateway : currentGateways) {
            if(! existingGateways.contains(gateway)){
                return gateway;
            }
        }
        return null;
    }
}
