package com.example.only4_kafka.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.retry")
public record RetryProperties (

    int emailMaxAttempts,
    int smsMaxAttempts,

    long initialIntervalMs,
    double multiplier,
    long maxIntervalMs
) {}
