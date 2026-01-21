package com.example.only4_kafka.service.email;

import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    public void send(EmailSendRequestEvent event) {
        String topic = kafkaTopicsProperties.emailRequest();

        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Email Producer] 발행 실패. topic={}, memberId={}, billId={}, error={}",
                                topic, event.memberId(), event.billId(), ex.getMessage());
                    } else {
                        log.info("[Email Producer] 발행 성공. topic={}, memberId={}, billId={}",
                                topic, event.memberId(), event.billId());
                    }
                });
    }
}
