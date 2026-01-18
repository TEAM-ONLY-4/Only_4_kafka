package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.constant.KafkaPropertiesConstant;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {

    private final RetryProperties retryProperties;

    // ConsumerFactory: 메시지를 어떻게 읽을지 설정
    @Bean
    public ConsumerFactory<String, EmailSendRequestEvent> emailConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties()); // yml에 적은 kafka 설정들 가져옴 (bootstrap-servers, group-id 등)
        // 메시지 변환 방식 지정
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 보안 설정 / JSON을 아무 클래스로나 변환하면 위험 / 해당 패키지 클래스만 허용 지정
        JsonDeserializer<EmailSendRequestEvent> valueDeserializer =
                new JsonDeserializer<>(EmailSendRequestEvent.class);
        valueDeserializer.addTrustedPackages(KafkaPropertiesConstant.EVENT_PACAKGE);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer); // 설정 다 모아서 Factory 생성
    }

    // @KafkaListener 실행하는 컨테이너 만드는 공장
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailSendRequestEvent>
            emailKafkaListenerContainerFactory(ConsumerFactory<String, EmailSendRequestEvent> emailConsumerFactory) {
        // 위에서 만든 ConsumerFactory 연결 / Concurrent라서 멀티스레드 처리 가능
        ConcurrentKafkaListenerContainerFactory<String, EmailSendRequestEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(emailConsumerFactory);
        factory.setCommonErrorHandler(emailErrorHandler());
        return factory;
    }

    // 이메일 처리 실패 시 재시도 정책 (지수 백오프)
    @Bean
    public DefaultErrorHandler emailErrorHandler() {
        int maxAttempts = retryProperties.emailMaxAttempts();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
        backOff.setInitialInterval(retryProperties.initialIntervalMs());
        backOff.setMultiplier(retryProperties.multiplier());
        backOff.setMaxInterval(retryProperties.maxIntervalMs());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(backOff);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("이메일 처리 재시도. attempt={}, topic={}, offset={}",
                        deliveryAttempt, record.topic(), record.offset())
        );

        return errorHandler;
    }

}
