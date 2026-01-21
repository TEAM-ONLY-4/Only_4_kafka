package com.example.only4_kafka.listener;

import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.email.EmailSendService;
import com.example.only4_kafka.service.email.SmsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailRequestListener {

    private final EmailSendService emailSendService;
    private final SmsKafkaProducer smsKafkaProducer;

    @KafkaListener(
            topics = "${app.kafka.topics.email-request}",
            groupId = "${app.kafka.topics.group-id}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listen(EmailSendRequestEvent event) {
        emailSendService.send(event);
    }
}
