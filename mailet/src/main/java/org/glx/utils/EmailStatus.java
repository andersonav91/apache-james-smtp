package org.glx.utils;

import org.apache.mailet.MailetContext;

import java.sql.*;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Properties;

public class EmailStatus {

    public static int getCreatedStatus(){
        Properties appProperties = new AppProperties().getAppProperties();
        Properties properties = new Properties();
        return Integer.parseInt(appProperties.getProperty("mail_status_created_id"));
    }

    public static int getRelayedStatus(){
        Properties appProperties = new AppProperties().getAppProperties();
        Properties properties = new Properties();
        return Integer.parseInt(appProperties.getProperty("mail_status_relayed_id"));
    }

    public static int getLimitedStatus(){
        Properties appProperties = new AppProperties().getAppProperties();
        Properties properties = new Properties();
        return Integer.parseInt(appProperties.getProperty("mail_status_limited_id"));
    }

    public static int getAllStatus(){
        Properties appProperties = new AppProperties().getAppProperties();
        Properties properties = new Properties();
        return Integer.parseInt(appProperties.getProperty("mail_status_all_id"));
    }

    public static void saveLog(MailetContext mailetContext, ArrayList<String> statusLog, String mailName){
        Connection connection = DatabaseConnection.getConnection();
        String query = "SELECT id FROM glx_mails WHERE mail_name = ?;";
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int mailId = 0;
        try{
            preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        }
        catch(SQLException sqlException){
            mailetContext.log("EmailStatus: error getting the email");
        }

        try{
            preparedStatement.setString(1, mailName);
            resultSet = preparedStatement.executeQuery();
            // num of rows

            int maxEmailsPerDay = 0;
            while (resultSet.next()) {
                mailId = resultSet.getInt("id");
            }
        }
        catch(SQLException sqlException){
            mailetContext.log("EmailStatus: error getting the email id");
        }

        PreparedStatement preparedStatementInsert = null;
        for (String status: statusLog) {
            Calendar calendar = Calendar.getInstance();
            Timestamp now = new Timestamp(calendar.getTime().getTime());
            Timestamp updated = new Timestamp(calendar.getTime().getTime());

            String insertQuery = "INSERT INTO glx_status_logs (mail_id, status_from_id, status_to_id, \"action\", created_at, updated_at) ";
            insertQuery = insertQuery.concat("VALUES (?, ?, ?, ?, ?, ?);");

            int statusToId = 0;
            if(status.equals("Created")){
                statusToId = EmailStatus.getCreatedStatus();
            }
            else if(status.equals("Limited")){
                statusToId = EmailStatus.getLimitedStatus();
            }
            else if(status.equals("Relayed")){
                statusToId = EmailStatus.getRelayedStatus();
            }

            try{
                preparedStatementInsert = connection.prepareStatement(insertQuery);
            }
            catch(SQLException sqlException){
                mailetContext.log("Email: error creating the insert the log for " + mailName + " and the status " + status);
            }
            try{
                preparedStatementInsert.setInt(1, mailId);
                preparedStatementInsert.setInt(2, EmailStatus.getAllStatus());
                preparedStatementInsert.setInt(3, statusToId);
                preparedStatementInsert.setString(4, status);
                preparedStatementInsert.setTimestamp(5, now);
                preparedStatementInsert.setTimestamp(6, updated);
                preparedStatementInsert.executeUpdate();
            }
            catch(SQLException sqlException){
                mailetContext.log("Email: error inserting the log for " + mailName + " and the status " + status);
            }
        }
    }
}
