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
    private final SmsKafkaProducer smsKafkaProducer;  // SMS 전환용 추가

    public void send(EmailSendRequestEvent event) {
        // 1. Data fetch
        EmailInvoiceReadResult emailInvoiceReadResult = emailInvoiceReader.read(event.memberId(), event.billId());

        // 2. ���� �� DB üũ �� �߼� ���� ����
        BillNotificationRow billNotification = emailInvoiceReadResult.billNotificationRow();

        // ��õ��� �ƴ� ���̽��� �� ���� ���̸� �ߴ�
        if (!checkBillNotification(billNotification)) return;

        // 2. Map to template DTO
        EmailInvoiceTemplateDto emailInvoiceTemplateDto = emailInvoiceMapper.toDto(emailInvoiceReadResult);

        // 3. Render HTML
        String htmlContent = emailTemplateRenderer.render(emailInvoiceTemplateDto);

        // 4. update bill status
        billNotificationWriter.updateBillNotificationSendStatus(event.billId(), BillChannel.EMAIL, SendStatus.SENT, null);

        // 5. Send email (비동기)
        // - 조회/매핑/렌더링은 완료, 발송(1초)만 스레드풀에 위임
        // - 최종 실패 시 SMS 전환
        String encryptedEmail = emailInvoiceReadResult.memberBill().memberEmail();
        String memberEmail = memberDataDecryptor.decryptEmail(encryptedEmail);

        emailClient.sendAsync(memberEmail, htmlContent, event.billId(), () -> {
            // 최종 실패 콜백: SMS로 전환
            log.warn("[EmailSendService] 이메일 최종 실패. SMS 전환. billId={}", event.billId());
            smsKafkaProducer.send(new SmsSendRequestEvent(event.memberId(), event.billId()));
        });

        log.info("8) Email 발송 위임 완료. memberId={}, billId={}", event.memberId(), event.billId());
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
