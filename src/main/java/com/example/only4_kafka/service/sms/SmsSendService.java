package com.example.only4_kafka.service.sms;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.SendStatus;
import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.infrastructure.sms.SmsClient;
import com.example.only4_kafka.repository.dto.BillNotificationRow;
import com.example.only4_kafka.service.BillNotificationWriter;
import com.example.only4_kafka.service.sms.dto.SmsInvoiceReadResult;
import com.example.only4_kafka.service.sms.mapper.SmsInvoiceMapper;
import com.example.only4_kafka.service.sms.reader.SmsInvoiceReader;
import com.example.only4_kafka.service.sms.util.SmsTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class SmsSendService {

    private final SmsInvoiceReader smsInvoiceReader;
    private final SmsInvoiceMapper smsInvoiceMapper;
    private final SmsTemplateRenderer smsTemplateRenderer;
    private final SmsClient smsClient;
    private final BillNotificationWriter billNotificationWriter;
    private final RetryProperties retryProperties; // 재시도 BackOff 시간 가져오기 위해 사용

    public void send(SmsSendRequestEvent event) {
        // 1. Data fetch
        SmsInvoiceReadResult readResult = smsInvoiceReader.read(event.billId());
        // 2. 시작 전에 DB 체크 후 발송 상태 변경
        BillNotificationRow billNotification = readResult.billNotificationRow();
        // 재시도가 아닌 남이 선점 중이면 진행 중단
        if(!checkBillNotification(billNotification)) return;

        // 3. Map to template DTO (Decrypts phone number)
        SmsBillDto smsBillDto = smsInvoiceMapper.toDto(readResult);

        // 4. Render Text
        String smsContent = smsTemplateRenderer.render(smsBillDto);

        // 5. update bill status
        billNotificationWriter.updateBillNotificationSendStatus(event.billId(), BillChannel.SMS, SendStatus.SENT, null);

        // 6. Send SMS
        smsClient.send(smsBillDto.phoneNumber(), smsBillDto.billId(), smsContent);
    }

    // BillNotification 상태 확인 후 발송 상태, processStartTime 업데이트
    private boolean checkBillNotification(BillNotificationRow billNotification) {
        // 정상 흐름 & SENT인 채로 재시도 흐름 : SENDING & 선점 시각 현재로
        if(billNotification.sendStatus() == SendStatus.PENDING || billNotification.sendStatus() == SendStatus.SENT) {
            billNotificationWriter.updateBillNotificationSendStatus(billNotification.billId(), BillChannel.SMS, SendStatus.SENDING, LocalDateTime.now());
            return true;
        }

        // SENDING인 채로 재시도 흐름 : 아무 것도 하지 않고 그냥 넘김 (SENDING 인채로)
        else if(billNotification.sendStatus() == SendStatus.SENDING
                && Duration.between(billNotification.processStartTime(), LocalDateTime.now()).getSeconds() >= 10) {

            billNotificationWriter.updateBillNotificationSendStatus(billNotification.billId(), BillChannel.SMS, SendStatus.SENDING, LocalDateTime.now());
            return true;
        }

        // 나머지 경우는 실행 중지
        else return false;
    }
}