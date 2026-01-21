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


    /**
     * @RetryableTopic 설정
     * 1. attempts = "3": 원본 1회 + 재시도 2회 = 총 3번 시도
     * 2. backoff: delay = 1000 (1초), multiplier = 1.0 (고정 딜레이, 지수백오프 X)
     * 3. topicSuffixingStrategy: 토픽 뒤에 -retry-0, -retry-1 등 인덱스 붙임
     * 4. dltStrategy: 실패 시 DLT(Dead Letter Topic)로 보냄
     */
//    @RetryableTopic(
//            attempts = "3",
//            backoff = @Backoff(delay = 1000, multiplier = 1.0),
//            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
//            kafkaTemplate = "kafkaTemplate" // 필요한 경우 지정
//    )
    @KafkaListener(
            topics = "${app.kafka.topics.email-request}",
            groupId = "${app.kafka.topics.group-id}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listen(EmailSendRequestEvent event) {
        emailSendService.send(event);
    }


    @DltHandler
    public void dltHandler(EmailSendRequestEvent event,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           Exception exception) {
        log.error("이메일 최종 실패. SMS 발송 전환 (ID: {}). 원인: {}", event.billId(), exception.getMessage());

        SmsSendRequestEvent smsSendRequestEvent = SmsSendRequestEvent.builder()
                .memberId(event.memberId())
                .billId(event.billId())
                .build();

        try {
            smsKafkaProducer.send(smsSendRequestEvent);
            log.info("SMS 발송 요청 완료 (ID: {})", event.billId());
        } catch (Exception e) {
            log.info("SMS 발송 요청 실패 (ID: {})", event.billId(), e);
        }

        smsKafkaProducer.send(smsSendRequestEvent);
    }
}
