package com.example.only4_kafka.infrastructure.sms;

import com.example.only4_kafka.global.exception.BusinessException;
import com.example.only4_kafka.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class SmsClient {
    public void send(String phoneNumber, Long billId, String smsBillContent) {
        // 이미 청구서가 발송되었는지 확인하기
        // 문제점 : '아직 발송중'인 경우 체킹 가능? => 발송은 했으나 도착 여부는 모를 때

        // SMS 발송 시도
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
