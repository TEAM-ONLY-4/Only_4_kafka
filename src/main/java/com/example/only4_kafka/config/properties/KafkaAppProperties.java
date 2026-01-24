package com.example.only4_kafka.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaAppProperties(
        int partitions,
        int concurrency
) {}
