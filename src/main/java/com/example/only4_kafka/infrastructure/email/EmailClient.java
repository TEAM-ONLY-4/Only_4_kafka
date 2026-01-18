package com.example.only4_kafka.infrastructure.email;

import com.example.only4_kafka.constant.EmailConstant;
import com.example.only4_kafka.global.exception.BusinessException;
import com.example.only4_kafka.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

@Slf4j
@Component
public class EmailClient {

    private final Random random = new Random();

    public void send(String to, String htmlContent) {
        simulateDelay();
        simulateRandomFailure();

        log.info("[EMAIL] 발송 성공. to={}, contentLength={}", to, htmlContent.length());
    }

    private void simulateDelay() {
        try {
            Thread.sleep(EmailConstant.SEND_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateRandomFailure() {
        if (random.nextInt(100) < EmailConstant.FAILURE_RATE_PERCENT) {
            log.warn("[EMAIL] 발송 실패 (시뮬레이션)");
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
