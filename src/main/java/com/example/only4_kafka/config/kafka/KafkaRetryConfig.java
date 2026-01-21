package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.service.email.fallback.EmailFallbackRecoverer;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;

@Configuration
@RequiredArgsConstructor
public class KafkaRetryConfig {
    private final RetryProperties retryProperties;

    @Value("${app.kafka.topics.email-request}")
    private String emailTopicName;
    private final EmailFallbackRecoverer emailFallbackRecoverer; // 이메일 재시도 후 실행되야할 FallbackRecoverer

    // 이메일 재시도 정책
    @Bean
    public RetryTopicConfiguration emailRetryConfiguration(KafkaTemplate<String, Object> template) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .fixedBackOff(retryProperties.initialIntervalMs())
                .maxAttempts(retryProperties.emailMaxAttempts())
                .setTopicSuffixingStrategy(TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
                .doNotConfigureDlt() // DLT 토픽 생성 끄기
                .dltHandlerMethod("emailFallbackRecoverer", "accept")
                .includeTopic(emailTopicName)
                .create(template);
    }
}
