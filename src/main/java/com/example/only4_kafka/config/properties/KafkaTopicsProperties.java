package com.example.only4_kafka.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        String groupId,
        String emailRequest,
        String smsRequest,
        String dlt
) {}