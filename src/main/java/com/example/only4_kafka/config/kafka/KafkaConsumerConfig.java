package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.constant.KafkaPropertiesConstant;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.failure.SmsFailureHandler;
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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {

    private final RetryProperties retryProperties;
    private final KafkaAppProperties kafkaAppProperties;
    // TopicsProperties는 현재 코드에서 직접 쓰이지 않더라도 구조상 유지하거나 필요시 사용
    private final KafkaTopicsProperties kafkaTopicsProperties; 
    private final SmsFailureHandler smsFailureHandler;

    // --- ConsumerFactory 설정 ---

    @Bean
    public ConsumerFactory<String, EmailSendRequestEvent> emailConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "102400");
        // 기본값 5분(300000) -> 10분(600000) 정도로 넉넉하게
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "600000");

        JsonDeserializer<EmailSendRequestEvent> valueDeserializer = new JsonDeserializer<>(EmailSendRequestEvent.class);
        valueDeserializer.addTrustedPackages(KafkaPropertiesConstant.EVENT_PACKAGE);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConsumerFactory<String, SmsSendRequestEvent> smsConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, "102400");
        // 기본값 5분(300000) -> 10분(600000) 정도로 넉넉하게
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "600000");

        JsonDeserializer<SmsSendRequestEvent> valueDeserializer = new JsonDeserializer<>(SmsSendRequestEvent.class);
        valueDeserializer.addTrustedPackages(KafkaPropertiesConstant.EVENT_PACKAGE);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    // --- ContainerFactory 설정 (여기가 수정됨) ---

    // 1. 파라미터에 KafkaProperties 추가
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailSendRequestEvent> emailKafkaListenerContainerFactory(KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, EmailSendRequestEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 2. 메서드 호출 시 kafkaProperties 전달
        factory.setConsumerFactory(emailConsumerFactory(kafkaProperties));
        factory.setConcurrency(kafkaAppProperties.concurrency());

        // [성능 튜닝 4] AckMode 설정 (중요!)
        // ENABLE_AUTO_COMMIT=false이므로, 리스너가 정상 종료되면 커밋하도록 설정
        // MANUAL_IMMEDIATE: Acknowledgment.acknowledge() 호출 시 즉시 커밋 (안전성 높음)
        // BATCH: poll() 한 뭉텅이 처리가 다 끝나면 한 번에 커밋 (속도 빠름, 실패 시 중복 처리 범위 넓어짐)
        // 이메일 중복 발송 방지가 중요하다면 MANUAL_IMMEDIATE, 속도가 최우선이면 BATCH
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        return factory;
    }

    // 1. 파라미터에 KafkaProperties 추가
    @Bean("smsListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SmsSendRequestEvent> smsFactory(KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, SmsSendRequestEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 2. 메서드 호출 시 kafkaProperties 전달
        factory.setConsumerFactory(smsConsumerFactory(kafkaProperties));
        factory.setConcurrency(kafkaAppProperties.concurrency());

        // [성능 튜닝 4] AckMode 설정 (중요!)
        // ENABLE_AUTO_COMMIT=false이므로, 리스너가 정상 종료되면 커밋하도록 설정
        // MANUAL_IMMEDIATE: Acknowledgment.acknowledge() 호출 시 즉시 커밋 (안전성 높음)
        // BATCH: poll() 한 뭉텅이 처리가 다 끝나면 한 번에 커밋 (속도 빠름, 실패 시 중복 처리 범위 넓어짐)
        // 이메일 중복 발송 방지가 중요하다면 MANUAL_IMMEDIATE, 속도가 최우선이면 BATCH
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        return factory;
    }

    // Retryable Topic으로 변경
//    // 이메일 처리 실패 시 재시도 정책 (지수 백오프)
//    @Bean
//    public DefaultErrorHandler emailErrorHandler(SmsKafkaProducer smsKafkaProducer) {
//        int maxAttempts = retryProperties.emailMaxAttempts();
//        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
//        backOff.setInitialInterval(retryProperties.initialIntervalMs());
//        backOff.setMultiplier(retryProperties.multiplier());
//        backOff.setMaxInterval(retryProperties.maxIntervalMs());
//
//        // 최종 실패 시 SMS 발행하는 recoverer
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, ex) -> {
//            log.error("이메일 최종 실패. SMS 발행 진행. topic={}, offset={}, error={}",
//                    record.topic(), record.offset(), ex.getMessage());
//
//            EmailSendRequestEvent emailEvent = (EmailSendRequestEvent) record.value();
//            SmsSendRequestEvent smsEvent = new SmsSendRequestEvent(emailEvent.memberId(), emailEvent.billId());
//            smsKafkaProducer.send(smsEvent);
//        }, backOff);
//
//        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
//                log.warn("이메일 처리 재시도. attempt={}, topic={}, offset={}",
//                        deliveryAttempt, record.topic(), record.offset())
//        );
//
//        return errorHandler;
//    }

    // Retryable Topic으로 변경
//    // Sms 재시도 정책 설정
//    @Bean
//    public DefaultErrorHandler smsErrorHandler() {
//        // 재시도 정책 생성
//        int maxAttempts = retryProperties.smsMaxAttempts();
//        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(maxAttempts - 1);
//        backOff.setInitialInterval(retryProperties.initialIntervalMs());
//        backOff.setMultiplier(retryProperties.multiplier());
//        backOff.setMaxInterval(retryProperties.maxIntervalMs());
//
//        // ErrorHandler 생성 (Recoverer + BackOff)
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(smsFailureHandler, backOff);
//
//        // 재시도 로그 리스너
//        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
//                log.warn("SMS 처리 재시도. attempt={}, topic={}, error={}",
//                        deliveryAttempt, record.topic(), ex.getMessage())
//        );
//
//        return errorHandler;
//    }
}
