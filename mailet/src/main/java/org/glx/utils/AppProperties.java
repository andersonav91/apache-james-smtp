package org.glx.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppProperties {
    public Properties getAppProperties(){
        Properties appProperties = new Properties();
        InputStream input = null;
        try {
            String filename = "app.properties";
            input = getClass().getClassLoader().getResourceAsStream(filename);
            if (input == null) {
                throw new NullPointerException("Error calling the properties file");
            }
            appProperties.load(input);
            return appProperties;

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
