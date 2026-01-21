package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    private final KafkaTopicsProperties properties;

    public KafkaTopicConfig(KafkaTopicsProperties properties) {
        this.properties = properties;
    }

    @Bean
    public NewTopic emailRequestTopic() {
        return TopicBuilder.name(properties.emailRequest())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic smsRequestTopic() {
        return TopicBuilder.name(properties.smsRequest())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
