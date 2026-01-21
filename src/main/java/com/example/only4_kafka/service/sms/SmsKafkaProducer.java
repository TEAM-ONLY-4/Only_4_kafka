package com.example.only4_kafka.service.sms;

import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SmsKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    public void send(SmsSendRequestEvent smsSendRequestEvent) {
        String topic = kafkaTopicsProperties.smsRequest();

        kafkaTemplate.send(topic, smsSendRequestEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[SMS Producer] 발행 실패. topic={}, memberId={}, billId={}, error={}",
                                topic, smsSendRequestEvent.memberId(), smsSendRequestEvent.billId(), ex.getMessage());
                    } else {
                        log.info("[SMS Producer] 발행 성공. topic={}, memberId={}, billId={}",
                                topic, smsSendRequestEvent.memberId(), smsSendRequestEvent.billId());
                    }
                });
    }
}
