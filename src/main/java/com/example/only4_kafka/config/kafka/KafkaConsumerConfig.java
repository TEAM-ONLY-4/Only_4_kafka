package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.constant.KafkaPropertiesConstant;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.email.SmsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.failure.SmsFailureHandler;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private final SmsFailureHandler smsFailureHandler;

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

    // smsConsumerFactory : KafkaProperties 안 쓰는 방식으로
    @Bean
    public ConsumerFactory<String, SmsSendRequestEvent> smsConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 메시지 항상 처음부터 읽음 (데이터 유실 방지)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 오프셋 자동 커밋 비활성화

        // 보안 설정 / JSON을 아무 클래스로나 변환하면 위험 / 해당 패키지 클래스만 허용 지정
        JsonDeserializer<SmsSendRequestEvent> valueDeserializer =
                new JsonDeserializer<>(SmsSendRequestEvent.class);
        valueDeserializer.addTrustedPackages(KafkaPropertiesConstant.EVENT_PACAKGE);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer); // 설정 다 모아서 Factory 생성
    }

    // @KafkaListener 실행하는 컨테이너 만드는 공장
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailSendRequestEvent>
            emailKafkaListenerContainerFactory(ConsumerFactory<String, EmailSendRequestEvent> emailConsumerFactory,
                                               DefaultErrorHandler emailErrorHandler) {
        // 위에서 만든 ConsumerFactory 연결 / Concurrent라서 멀티스레드 처리 가능
        ConcurrentKafkaListenerContainerFactory<String, EmailSendRequestEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(emailConsumerFactory);
        factory.setCommonErrorHandler(emailErrorHandler);
        return factory;
    }

    // Sms용 ContainerFactory
    @Bean("smsListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SmsSendRequestEvent> smsFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SmsSendRequestEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(smsConsumerFactory());
        factory.setConcurrency(3); // Sms 컨슈머 3개
        factory.setCommonErrorHandler(smsErrorHandler());

        return factory;
    }

    // 이메일 처리 실패 시 재시도 정책 (지수 백오프)
    @Bean
    public DefaultErrorHandler emailErrorHandler(SmsKafkaProducer smsKafkaProducer) {
        int maxAttempts = retryProperties.emailMaxAttempts();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
        backOff.setInitialInterval(retryProperties.initialIntervalMs());
        backOff.setMultiplier(retryProperties.multiplier());
        backOff.setMaxInterval(retryProperties.maxIntervalMs());

        // 최종 실패 시 SMS 발행하는 recoverer
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, ex) -> {
            log.error("이메일 최종 실패. SMS 발행 진행. topic={}, offset={}, error={}",
                    record.topic(), record.offset(), ex.getMessage());

            EmailSendRequestEvent emailEvent = (EmailSendRequestEvent) record.value();
            SmsSendRequestEvent smsEvent = new SmsSendRequestEvent(emailEvent.memberId(), emailEvent.billId());
            smsKafkaProducer.send(smsEvent);
        }, backOff);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("이메일 처리 재시도. attempt={}, topic={}, offset={}",
                        deliveryAttempt, record.topic(), record.offset())
        );

        return errorHandler;
    }

    // Sms 재시도 정책 설정
    @Bean
    public DefaultErrorHandler smsErrorHandler() {
        // 재시도 정책 생성
        int maxAttempts = retryProperties.smsMaxAttempts();
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
        backOff.setInitialInterval(retryProperties.initialIntervalMs());
        backOff.setMultiplier(retryProperties.multiplier());
        backOff.setMaxInterval(retryProperties.maxIntervalMs());

        // ErrorHandler 생성 (Recoverer + BackOff)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(smsFailureHandler, backOff);

        // 재시도 로그 리스너
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("SMS 처리 재시도. attempt={}, topic={}, error={}",
                        deliveryAttempt, record.topic(), ex.getMessage())
        );

        return errorHandler;
    }
}
