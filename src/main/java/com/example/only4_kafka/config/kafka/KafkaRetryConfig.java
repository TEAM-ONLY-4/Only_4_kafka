package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.RetryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaRetryConfig {
    private final RetryProperties retryProperties;
    
    @Value("${app.kafka.topics.email-request}")
    private String emailTopicName;

    // 이메일 재시도 정책
    @Bean
    public RetryTopicConfiguration emailRetryConfiguration(KafkaTemplate<String, Object> template) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .fixedBackOff(retryProperties.initialIntervalMs())
                .maxAttempts(retryProperties.emailMaxAttempts())
                .includeTopic(emailTopicName)
                .create(template);
    }
}
