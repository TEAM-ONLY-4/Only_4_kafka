package com.example.only4_kafka.listener;

import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.service.email.EmailSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EmailRequestListener {

    private final EmailSendService emailSendService;

    @KafkaListener(
            topics = "${app.kafka.topics.email-request}",
            groupId = "${app.kafka.topics.group-id}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listen(EmailSendRequestEvent event) {
        emailSendService.send(event);
    }
}
