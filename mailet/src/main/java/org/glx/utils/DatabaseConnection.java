package org.glx.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    public static Connection getConnection(){
        Properties appProperties = new AppProperties().getAppProperties();
        Properties properties = new Properties();
        Connection connection;
        String url = appProperties.getProperty("database_url");
        properties.setProperty("user", appProperties.getProperty("database_user"));
        properties.setProperty("password", appProperties.getProperty("database_password"));
        properties.setProperty("ssl", appProperties.getProperty("database_ssl"));
        try {
            connection = DriverManager.getConnection(url, properties);
            return connection;
        }
        catch(SQLException sqlException){
            throw new NullPointerException("Error connecting to the database");
        }
    }
}
