package com.example.only4_kafka.infrastructure.email;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.constant.EmailConstant;
import com.example.only4_kafka.global.exception.BusinessException;
import com.example.only4_kafka.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * 이메일 발송 클라이언트
 *
 * [동기 발송] send() - 호출 스레드에서 1초 대기
 * [비동기 발송] sendAsync() - 스레드풀에 위임, 재시도 포함
 */
@Slf4j
@Component
public class EmailClient {

    private final Random random = new Random();
    private final ExecutorService executorService;
    private final RetryProperties retryProperties;

    public EmailClient(ExecutorService emailSendExecutorService, RetryProperties retryProperties) {
        this.executorService = emailSendExecutorService;
        this.retryProperties = retryProperties;
    }

    /**
     * [동기 발송] - 기존 메서드 유지
     * 호출 스레드에서 1초 대기 (블로킹)
     */
    public void send(String to, String htmlContent) {
        simulateDelay();
        simulateRandomFailure();
        log.info("[EMAIL] 발송 성공. to={}, contentLength={}", to, htmlContent.length());
    }

    /**
     * [비동기 발송] - 발송만 스레드풀에 위임
     * - 조회/매핑/렌더링은 이미 완료된 상태
     * - 발송(1초)만 별도 스레드에서 실행
     * - 자체 재시도 포함 (3회)
     * - 최종 실패 시 콜백 호출
     */
    public void sendAsync(String to, String htmlContent, Long billId, Runnable onFinalFailure) {
        executorService.submit(() -> {
            int maxAttempts = retryProperties.emailMaxAttempts();
            long intervalMs = retryProperties.initialIntervalMs();

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    log.info("[EMAIL] 비동기 발송 시도 {}/{}. to={}, billId={}", attempt, maxAttempts, to, billId);
                    send(to, htmlContent);  // 기존 동기 발송 호출
                    return;  // 성공하면 종료

                } catch (Exception e) {
                    log.warn("[EMAIL] 비동기 발송 실패 {}/{}. billId={}, error={}", attempt, maxAttempts, billId, e.getMessage());

                    if (attempt < maxAttempts) {
                        sleep(intervalMs);
                    }
                }
            }

            // 최종 실패: 콜백 실행 (SMS 전환 등)
            log.error("[EMAIL] 최종 실패. billId={}", billId);
            if (onFinalFailure != null) {
                onFinalFailure.run();
            }
        });
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
