package com.example.only4_kafka.service.failure;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsFailureHandler extends DeadLetterPublishingRecoverer {
    private static final String DLT_TOPIC = "notification.dlt"; // KafkaTopicConfig에서 읽어오는 걸로 변경 예정

    public SmsFailureHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate, (record, ex) -> {
            return new TopicPartition(DLT_TOPIC, -1);
        });
    }
}