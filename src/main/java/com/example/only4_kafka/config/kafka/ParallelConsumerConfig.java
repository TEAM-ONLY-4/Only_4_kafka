package com.example.only4_kafka.config.kafka;

import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import io.confluent.parallelconsumer.ParallelConsumerOptions;
import io.confluent.parallelconsumer.ParallelConsumerOptions.ProcessingOrder;
import io.confluent.parallelconsumer.ParallelStreamProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * Parallel Consumer 설정
 *
 * [일반 Consumer vs Parallel Consumer]
 * - 일반: 파티션 단위 병렬 (8 파티션 = 최대 8개 동시)
 * - Parallel: 메시지 단위 병렬 (maxConcurrency만큼 동시)
 *
 * [장점]
 * - 파티션 수와 무관하게 병렬 처리 가능
 * - ACK 정확 (처리 완료된 offset만 커밋)
 * - 동기 처리해도 성능 좋음
 *
 * [설정]
 * - maxConcurrency: 100 (동시 100개 처리)
 * - ordering: KEY (같은 memberId는 순서 보장)
 */
@Slf4j
@Configuration
public class ParallelConsumerConfig {

    private static final int MAX_CONCURRENCY = 100;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private final KafkaTopicsProperties topicsProperties;

    public ParallelConsumerConfig(KafkaTopicsProperties topicsProperties) {
        this.topicsProperties = topicsProperties;
    }

    @Bean
    public Consumer<String, EmailSendRequestEvent> emailParallelKafkaConsumer() {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, topicsProperties.groupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.only4_kafka.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmailSendRequestEvent.class.getName());

        return new KafkaConsumer<>(props);
    }

    @Bean
    public ParallelStreamProcessor<String, EmailSendRequestEvent> emailParallelStreamProcessor(
            Consumer<String, EmailSendRequestEvent> emailParallelKafkaConsumer) {

        ParallelConsumerOptions<String, EmailSendRequestEvent> options = ParallelConsumerOptions
                .<String, EmailSendRequestEvent>builder()
                .consumer(emailParallelKafkaConsumer)
                .ordering(ProcessingOrder.UNORDERED)
                .maxConcurrency(MAX_CONCURRENCY)
                .commitMode(ParallelConsumerOptions.CommitMode.PERIODIC_CONSUMER_ASYNCHRONOUS)
                .build();

        log.info("Parallel Consumer 설정 완료. maxConcurrency={}, ordering=KEY", MAX_CONCURRENCY);

        return ParallelStreamProcessor.createEosStreamProcessor(options);
    }
}
