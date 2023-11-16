package com.demo.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The type PropertiesHelper is a class that represents the value stored
 * in the properties file named config.properties.
 */
public class PropertiesHelper {
    /**
     * Gets a Properties object that contains the keys and values defined
     * in the file src/main/resources/config.properties
     *
     * @return a {@link java.util.Properties} object
     * @throws Exception Thrown if the file config.properties is not available
     *                   in the directory src/main/resources
     */
    public static Properties getConsumerProperties() throws Exception {

        String connectionType = System.getenv("CONSUMER_CONNECTION");
        String fileName = "";

        switch(connectionType.toUpperCase()){
            case "AZURE_EVENT_HUB":
                fileName = "config-aze.properties";
                break;
            case "CONFLUENT_CLOUD":
                fileName = "config-ccloud.properties";
                break;
        }

        Properties props = null;
        //try to load the file config.properties
        try (InputStream input = SimpleProducer.class.getClassLoader().getResourceAsStream(fileName)) {

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

    public static Properties getProducerProperties() throws Exception {

        String connectionType = System.getenv("PRODUCER_CONNECTION");
        String fileName = "";

        switch(connectionType.toUpperCase()){
            case "AZURE_EVENT_HUB":
                fileName = "config-aze.properties";
                break;
            case "CONFLUENT_CLOUD":
                fileName = "config-ccloud.properties";
                break;
        }

        Properties props = null;
        //try to load the file config.properties
        try (InputStream input = SimpleProducer.class.getClassLoader().getResourceAsStream(fileName)) {

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