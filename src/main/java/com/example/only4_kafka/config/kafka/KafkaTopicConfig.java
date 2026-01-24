package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.KafkaAppProperties;
import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private final KafkaTopicsProperties topicsProperties;
    private final KafkaAppProperties appProperties;

    public KafkaTopicConfig(KafkaTopicsProperties topicsProperties, KafkaAppProperties appProperties) {
        this.topicsProperties = topicsProperties;
        this.appProperties = appProperties;
    }

    @Bean
    public NewTopic emailRequestTopic() {
        return TopicBuilder.name(topicsProperties.emailRequest())
                .partitions(appProperties.partitions())
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic smsRequestTopic() {
        return TopicBuilder.name(topicsProperties.smsRequest())
                .partitions(appProperties.partitions())
                .replicas(1)
                .build();
    }
}
