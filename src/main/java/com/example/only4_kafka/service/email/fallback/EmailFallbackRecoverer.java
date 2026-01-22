package com.example.only4_kafka.service.email.fallback;

import com.example.only4_kafka.domain.bill_notification.BillNotificationRepository;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.sms.SmsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

// 이메일 재시도 횟수 끝난 후 실행되어야할 FallbackRecoverer (SMS Producer에 전송)
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailFallbackRecoverer {
    private final SmsKafkaProducer smsKafkaProducer;

    @DltHandler
    public void accept(@Payload EmailSendRequestEvent event,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {

        log.error("이메일 최종 실패 (Retry 소진). SMS 발송 전환. (Topic: {}, Error: {})", topic, errorMessage);

        // SmsEvent로 변환
        SmsSendRequestEvent smsSendRequestEvent = SmsSendRequestEvent.builder()
                .memberId(event.memberId())
                .billId(event.billId())
                .build();

        try {
            smsKafkaProducer.send(smsSendRequestEvent);
            log.info("SMS 발송 요청 완료 (ID: {})", event.billId());
        } catch (Exception e) {
            log.error("SMS 발송 요청 실패 (ID: {})", event.billId(), e);
        }
    }
}
