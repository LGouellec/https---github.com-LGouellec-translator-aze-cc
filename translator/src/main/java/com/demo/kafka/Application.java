package com.demo.kafka;

import com.demo.kafka.internal.Translator;

import java.util.Arrays;
import java.util.List;

/**
 * The type Translator.
 */
public class Application {

    /**
     * The entry point of application.
     *
     * @param args the input arguments consumer group, topics
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        String errorStr = "ERROR: You need to declare the first parameter the consumer group, " +
                "the second parameter is the list of topics";

        if (args.length != 2){
            System.out.println(errorStr);
            return;
        }

        Translator translator = new Translator();
        String consumerGroup = args[0];
        List<String> topics = Arrays.stream(args[1].split(",")).toList();
        translator.translate(consumerGroup, topics);
        translator.close();
    }
}