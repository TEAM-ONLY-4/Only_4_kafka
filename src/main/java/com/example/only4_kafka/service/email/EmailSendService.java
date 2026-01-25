package com.example.only4_kafka.service.email;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.SendStatus;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.infrastructure.MemberDataDecryptor;
import com.example.only4_kafka.infrastructure.email.EmailClient;
import com.example.only4_kafka.repository.dto.BillNotificationRow;
import com.example.only4_kafka.service.BillNotificationWriter;
import com.example.only4_kafka.service.email.dto.EmailInvoiceReadResult;
import com.example.only4_kafka.service.email.dto.EmailInvoiceTemplateDto;
import com.example.only4_kafka.service.email.mapper.EmailInvoiceMapper;
import com.example.only4_kafka.service.email.reader.EmailInvoiceReader;
import com.example.only4_kafka.service.email.util.EmailTemplateRenderer;
import com.example.only4_kafka.service.sms.SmsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailSendService {

    private final EmailInvoiceReader emailInvoiceReader;
    private final EmailInvoiceMapper emailInvoiceMapper;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailClient emailClient;
    private final MemberDataDecryptor memberDataDecryptor;
    private final BillNotificationWriter billNotificationWriter;
    private final RetryProperties retryProperties;
    private final SmsKafkaProducer smsKafkaProducer;
    private final ExecutorService emailSendExecutorService;

    public void send(EmailSendRequestEvent event) {
        // 1. 데이터 조회
        EmailInvoiceReadResult emailInvoiceReadResult = emailInvoiceReader.read(event.memberId(), event.billId());
        BillNotificationRow billNotification = emailInvoiceReadResult.billNotificationRow();

        // 2. 상태 체크 (SENT면 스킵 - 중복 방지)
        if (!checkBillNotification(billNotification)) return;

        // 3. 렌더링
        EmailInvoiceTemplateDto emailInvoiceTemplateDto = emailInvoiceMapper.toDto(emailInvoiceReadResult);
        String htmlContent = emailTemplateRenderer.render(emailInvoiceTemplateDto);

        // 4. SENT로 변경 (이메일 발송 전 선점)
        billNotificationWriter.updateBillNotificationSendStatus(event.billId(), BillChannel.EMAIL, SendStatus.SENT, null);

        // 5. 이메일 정보 추출
        String encryptedEmail = emailInvoiceReadResult.memberBill().memberEmail();
        String memberEmail = memberDataDecryptor.decryptEmail(encryptedEmail);

        // 6. 이메일 발송 비동기 위임 (가상 스레드)
//        emailSendExecutorService.submit(() -> {
//            try {
//                emailClient.send(memberEmail, htmlContent);
//                log.info("[EMAIL_COMPLETE] billId={} 발송 완료", event.billId());
//            } catch (Exception e) {
//                log.error("[EMAIL_FAILED] billId={} 발송 실패. SMS 전환. error={}", event.billId(), e.getMessage());
//                smsKafkaProducer.send(new SmsSendRequestEvent(event.memberId(), event.billId()));
//            }
//        });

        emailSendExecutorService.submit(() -> {
            try {
                emailClient.send(memberEmail, htmlContent);
                log.info("[EMAIL_COMPLETE] billId={} 발송 완료", event.billId());
            } catch (Exception e) {
                log.error("[EMAIL_FAILED] billId={} 발송 실패. SMS 전환. error={}", event.billId(), e.getMessage(), e);

                try {
                    smsKafkaProducer.send(new SmsSendRequestEvent(event.memberId(), event.billId()));
                    log.info("[SMS_FALLBACK_TRIGGERED] billId={} SMS 발행 요청 호출 완료", event.billId());
                } catch (Exception ex) {
                    // ★ 여기 로그가 찍히면 “kafkaTemplate.send()가 호출 시점에 즉시 예외”였던 것
                    log.error("[SMS_FALLBACK_TRIGGER_FAILED_SYNC] billId={} SMS 발행 요청 중 즉시 실패. error={}",
                            event.billId(), ex.getMessage(), ex);
                }
            }
        });


    }

    private boolean checkBillNotification(BillNotificationRow billNotification) {
        if (billNotification.sendStatus() == SendStatus.PENDING || billNotification.sendStatus() == SendStatus.SENT) {
            billNotificationWriter.updateBillNotificationSendStatus(
                    billNotification.billId(), BillChannel.EMAIL, SendStatus.SENDING, LocalDateTime.now());
            return true;
        } else if (billNotification.sendStatus() == SendStatus.SENDING
                && Duration.between(billNotification.processStartTime(), LocalDateTime.now()).getSeconds() >= retryProperties.initialIntervalMs()) {
            billNotificationWriter.updateBillNotificationSendStatus(
                    billNotification.billId(), BillChannel.EMAIL, SendStatus.SENDING, LocalDateTime.now());
            return true;
        } else {
            return false;
        }
    }
}
