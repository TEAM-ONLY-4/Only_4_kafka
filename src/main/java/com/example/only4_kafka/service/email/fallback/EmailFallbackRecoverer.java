package com.example.only4_kafka.service.email.fallback;

import com.example.only4_kafka.domain.bill_notification.BillNotificationRepository;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.service.sms.SmsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

// 이메일 재시도 횟수 끝난 후 실행되어야할 FallbackRecoverer (SMS Producer에 전송)
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailFallbackRecoverer implements ConsumerRecordRecoverer {

    private final SmsKafkaProducer smsKafkaProducer;
    private final BillNotificationRepository billNotificationRepository;

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        // 메시지 꺼내기 (Casting 필요)
        EmailSendRequestEvent event = (EmailSendRequestEvent) record.value();

        log.error("이메일 최종 실패 (Retry 소진). SMS 발송 전환 (ID: {}). 원인: {}", event.billId(), exception.getMessage());

        // SmsEvent로 변환
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
    }
}
