package com.demo.kafka.internal;

import com.demo.kafka.Application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The type PropertiesHelper is a class that represents the value stored
 * in the properties file named config.properties.
 */
public class PropertiesHelper {

    public static Properties getSourceProperties() throws Exception {
        String fileName = "config-source.properties";

        Properties props = null;
        //try to load the file config.properties
        try (InputStream input = Application.class.getClassLoader().getResourceAsStream(fileName)) {

            props = new Properties();

            if (input == null) {
                throw new Exception("Sorry, unable to find " + fileName);
            }

            //load a properties file from class path, inside static method
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return props;
    }

    public static Properties getSinkProperties() throws Exception {
        String fileName = "config-sink.properties";

        Properties props = null;
        //try to load the file config.properties
        try (InputStream input = Application.class.getClassLoader().getResourceAsStream(fileName)) {

            props = new Properties();

            if (input == null) {
                throw new Exception("Sorry, unable to find " + fileName);
            }

            //load a properties file from class path, inside static method
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return props;
    }

}