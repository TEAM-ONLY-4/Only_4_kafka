package com.example.only4_kafka.listener;

import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.sms.SmsSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SmsRequestListener {
    private final SmsSendService smsSendService;

    @KafkaListener(
            topics = "${app.kafka.topics.sms-request}",
            groupId = "${app.kafka.topics.group-id}",
            containerFactory = "smsListenerContainerFactory"

    )
    public void listen(SmsSendRequestEvent message) {
        smsSendService.send(message);
    }
}