package com.example.only4_kafka.infrastructure.sms;

import com.example.only4_kafka.domain.bill_notification.BillNotificationRepository;
import com.example.only4_kafka.global.exception.BusinessException;
import com.example.only4_kafka.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;


@Slf4j
@RequiredArgsConstructor
@Component
public class SmsClient {
    private final BillNotificationRepository billNotificationRepository;

    public void send(String phoneNumber, Long billId, String smsBillContent) {
        // SMS 발송 시도 : 일단 1% 확률로 실패 처리
        int random = ThreadLocalRandom.current().nextInt(100);

        // 뽑은 숫자가 0이면 실패 (확률 1/100)
        if (random == 0) {
            log.warn("[SMS 발송 실패] (BillId: {}, phone: {})", billId, phoneNumber);

            // SMS 발송 실패 예외처리
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }

        log.info("[SMS 발송 성공] (BillId: {}, phone: {}, smsBillContent: {})", billId, phoneNumber, smsBillContent);
    }
}
