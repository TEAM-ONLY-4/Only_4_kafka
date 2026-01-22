package com.example.only4_kafka.listener;

import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.service.email.EmailSendService;
import com.example.only4_kafka.service.sms.SmsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailRequestListener {

    private final EmailSendService emailSendService;

    @KafkaListener(
            topics = "${app.kafka.topics.email-request}",
            groupId = "${app.kafka.topics.group-id}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listen(EmailSendRequestEvent event,
                       @org.springframework.messaging.handler.annotation.Header(org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.info("[TEST-LOG] Thread: {}, Topic: {}, BillId: {}, Time: {}", 
                Thread.currentThread().getName(), topic, event.billId(), System.currentTimeMillis());

        if (event.memberId() == -999L) {
            throw new RuntimeException("Simulated Failure for Retry Test");
        }
        
        emailSendService.send(event);
    }
}
