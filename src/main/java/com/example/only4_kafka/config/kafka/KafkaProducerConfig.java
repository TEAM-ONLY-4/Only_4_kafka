package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import com.example.only4_kafka.config.properties.RetryProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableConfigurationProperties({RetryProperties.class, KafkaTopicsProperties.class})
@Configuration
public class KafkaProducerConfig {

    private final RetryProperties retryProperties;

    public KafkaProducerConfig(RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
        // 직렬화 설정
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(sslBundles));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 멱등성 프로듀서 설정 (중복 없는 전송)
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 리플리카 승인 대기
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // 메시지 순서 보장 성능 최적화
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // 재시도 횟수 (멱등성 보장 위해 무한대)
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryProperties.initialIntervalMs()); // 재시도 사이의 대기 시간

        // 🚀 [성능 튜닝 핵심] 대량 발송 최적화 설정

        // [1] 배치 크기 (기본 16KB -> 32KB ~ 64KB)
        // 한 번에 보낼 트럭의 크기를 키웁니다.
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);

        // [2] 지연 시간 (기본 0ms -> 10~20ms)
        // 트럭이 꽉 차지 않아도 20ms는 기다렸다가 출발합니다. (메시지를 모으는 효과)
        props.put(ProducerConfig.LINGER_MS_CONFIG, 20);

        // [3] 압축 설정 (선택 사항, 대량 데이터 시 네트워크 비용 절감)
        // CPU를 약간 쓰고 네트워크 대역폭을 아낍니다. (snappy, lz4, gzip 등)
        // lz4: 순수 Java 구현 포함으로 Alpine Linux에서도 네이티브 라이브러리 없이 작동
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        // [4] 신뢰성 설정 (all: 모든 리플리카 저장 확인, 1: 리더만 확인)
        // 속도가 중요하면 '1', 데이터 유실 절대 안 되면 'all'
        // props.put(ProducerConfig.ACKS_CONFIG, "all");

        // 서로 다른 프로젝트 간 JSON 통신 시 패키지 에러 방지
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
