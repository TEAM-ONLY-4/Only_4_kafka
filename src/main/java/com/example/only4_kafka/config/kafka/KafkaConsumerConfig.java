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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class KafkaConsumerConfig {

    private final RetryProperties retryProperties;
    // KafkaTemplate은 이제 ProducerConfig에서 관리하므로 여기서 필수는 아니지만, 필요시 사용
    // private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private final SmsFailureHandler smsFailureHandler;

    // --- ConsumerFactory 설정 ---

    @Bean
    public ConsumerFactory<String, EmailSendRequestEvent> emailConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<EmailSendRequestEvent> valueDeserializer = new JsonDeserializer<>(EmailSendRequestEvent.class);
        valueDeserializer.addTrustedPackages(KafkaPropertiesConstant.EVENT_PACKAGE);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    public ConsumerFactory<String, SmsSendRequestEvent> smsConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

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
        factory.setConcurrency(3);

        return factory;
    }

    // 1. 파라미터에 KafkaProperties 추가
    @Bean("smsListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, SmsSendRequestEvent> smsFactory(KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, SmsSendRequestEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 2. 메서드 호출 시 kafkaProperties 전달
        factory.setConsumerFactory(smsConsumerFactory(kafkaProperties));
        factory.setConcurrency(3);

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
