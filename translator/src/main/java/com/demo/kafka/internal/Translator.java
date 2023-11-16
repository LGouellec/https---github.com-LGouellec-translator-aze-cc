package com.demo.kafka.internal;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class Translator {

    private final AdminClient sinkAdminClient;
    private final AdminClient sourceAdminClient;
    private Properties sourceProps;
    private Properties sinkProps;

    static Logger log = Logger.getLogger(Translator.class.getName());

    public Translator() throws Exception {
        this.sourceProps = PropertiesHelper.getSourceProperties();
        this.sinkProps = PropertiesHelper.getSinkProperties();

        this.sourceAdminClient = AdminClient.create(this.sourceProps);
        this.sinkAdminClient = AdminClient.create(this.sinkProps);
    }

    public void translate(String consumerGroup, List<String> topics) throws ExecutionException, InterruptedException {
        Map<TopicPartition, OffsetAndMetadata> consumerGroupOffsets = sourceAdminClient.listConsumerGroupOffsets(consumerGroup)
                .partitionsToOffsetAndMetadata(consumerGroup)
                .get();

        StringBuilder sb = new StringBuilder();
        sb.append("♳ Getting the last offset committed in Azure Event Hubs :").append(System.lineSeparator());
        for(TopicPartition tp : consumerGroupOffsets.keySet())
            sb.append("Topic : ").append(tp.topic())
                    .append("| Partition : ").append(tp.partition())
                    .append("| Offset : ").append(consumerGroupOffsets.get(tp).offset())
                    .append(System.lineSeparator());
        log.info(sb.toString());

        Map<TopicPartition, Long> sourceOffsetsWithTimestamp = getSourceOffsetsWithTimestamp(consumerGroupOffsets, topics);

        Map<TopicPartition, OffsetAndTimestamp> sinkTranslateOffsets = getSinkOffsetFromTimestamp(sourceOffsetsWithTimestamp);
        Map<TopicPartition, OffsetAndMetadata> sinkOffsetsRequest = new HashMap<>();
        sinkTranslateOffsets.forEach((k, v) -> {
            sinkOffsetsRequest.put(k, new OffsetAndMetadata(v.offset()));
        });

        sinkAdminClient.alterConsumerGroupOffsets(consumerGroup, sinkOffsetsRequest)
                .all();
        log.info("⭐️ Consumer groups offsets altered");


        Map<String, Map<TopicPartition, OffsetAndMetadata>> cgOffsetsCC = sinkAdminClient.listConsumerGroupOffsets(consumerGroup).all().get();

        if(cgOffsetsCC.containsKey(consumerGroup)) {
            StringBuilder sbOffsets = new StringBuilder();
            sbOffsets.append("=> Status : Consumer group offset {" + consumerGroup + "} from Confluent Cloud").append(System.lineSeparator());

            Map<TopicPartition, OffsetAndMetadata> lastOffsetsCC =  cgOffsetsCC.get(consumerGroup);
            for(TopicPartition tp : lastOffsetsCC.keySet()) {
                sbOffsets.append("Topic : ").append(tp.topic())
                        .append("| Partition : ").append(tp.partition())
                        .append("| Offset : ").append(lastOffsetsCC.get(tp).offset())
                        .append(System.lineSeparator());
            }
            log.info(sbOffsets.toString());
        }
    }

    private Map<TopicPartition, OffsetAndTimestamp> getSinkOffsetFromTimestamp(Map<TopicPartition, Long> sourceOffsetsWithTimestamp) {
        Properties sinkConsumerProps = createLittleSourceConsumerConfiguration(sinkProps);
        KafkaConsumer<byte[], byte[]> sinkConsumer = new KafkaConsumer<>(sinkConsumerProps);

        Map<TopicPartition, OffsetAndTimestamp> sinkOffset = sinkConsumer.offsetsForTimes(sourceOffsetsWithTimestamp);

        sinkConsumer.close();

        StringBuilder sb = new StringBuilder();
        sb.append("⭐️ Getting offset from timestamp in Confluent Cloud :").append(System.lineSeparator());
        for(TopicPartition tp : sinkOffset.keySet())
            sb.append("Topic : ").append(tp.topic())
                    .append("| Partition : ").append(tp.partition())
                    .append("| Offset : ").append(sinkOffset.get(tp).offset())
                    .append("| Timestamp : ").append(sinkOffset.get(tp).timestamp())
                    .append(System.lineSeparator());
        log.info(sb.toString());

        return sinkOffset;
    }

    public void close(){
        sourceAdminClient.close();
        sinkAdminClient.close();
    }

    private Properties createLittleSourceConsumerConfiguration(Properties basedProperties){
        Properties consumerProps = new Properties();
        consumerProps.putAll(basedProperties);
        consumerProps.setProperty("max.poll.records", "1");
        consumerProps.setProperty("fetch.min.bytes", "1");
        consumerProps.setProperty("fetch.max.bytes", "1048576"); // 1Mb
        consumerProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumerProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        return consumerProps;
    }

    private Map<TopicPartition, Long> getSourceOffsetsWithTimestamp(Map<TopicPartition, OffsetAndMetadata> consumerGroupOffsets, List<String> topics){

        Map<TopicPartition, Long> sourceOffsetsWithTimestamp = new HashMap<>();
        Properties sourceConsumerProps = createLittleSourceConsumerConfiguration(sourceProps);

        KafkaConsumer<byte[], byte[]> sourceConsumer = new KafkaConsumer<>(sourceConsumerProps);

        for(TopicPartition tp : consumerGroupOffsets.keySet()) {
            if(topics.contains(tp.topic())) {
                sourceConsumer.assign(List.of(tp));
                sourceConsumer.seek(tp, consumerGroupOffsets.get(tp).offset());
                ConsumerRecords<byte[], byte[]> records = null;

                do {
                    records = sourceConsumer.poll(Duration.ofMillis(100));
                } while (records.isEmpty());

                sourceOffsetsWithTimestamp.put(tp, records.records(tp).get(0).timestamp());
            }
        }

        sourceConsumer.close();
        return sourceOffsetsWithTimestamp;
    }
}
