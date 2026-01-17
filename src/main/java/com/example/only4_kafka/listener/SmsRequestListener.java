package com.example.only4_kafka.listener;

import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.SmsSendService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SmsRequestListener {
    private final KafkaTopicsProperties topics;
    private final SmsSendService smsSendService;

    public SmsRequestListener(KafkaTopicsProperties topics, SmsSendService smsSendService) {
        this.topics = topics;
        this.smsSendService = smsSendService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.sms-request}",
            groupId = "${app.kafka.topics.group-id}"

            // 노션에 적힌 아래 방법은 에러가 떠서 바꿈
            // A component required a bean named 'kafkaTopicsProperties' that could not be found.
//            topics = "#{@kafkaTopicsProperties.smsRequest}",
//            groupId = "#{@kafkaTopicsProperties.groupId}"
    )
    public void listen(SmsSendRequestEvent message) {
        smsSendService.processSms(message);
    }
}
