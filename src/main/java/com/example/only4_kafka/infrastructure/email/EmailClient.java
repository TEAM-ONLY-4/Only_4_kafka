package com.example.only4_kafka.infrastructure.email;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.constant.EmailConstant;
import com.example.only4_kafka.global.exception.BusinessException;
import com.example.only4_kafka.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * 이메일 발송 클라이언트
 *
 * [동기 발송] send() - 호출 스레드에서 1초 대기
 * [비동기 발송] sendAsync() - 가상 스레드풀에 위임, Semaphore로 동시 처리 수 제한
 *
 * [가상 스레드 + Semaphore]
 * - 가상 스레드: I/O 블로킹에 효율적, 메모리 가벼움
 * - Semaphore: 동시 처리 수 제한 (DB 커넥션, 외부 API 보호)
 * - 제한 초과 시 대기 (가상 스레드라서 OS 스레드 안 막힘)
 */
@Slf4j
@Component
public class EmailClient {

    private final Random random = new Random();
    private final ExecutorService executorService;
    private final Semaphore semaphore;
    private final RetryProperties retryProperties;

    public EmailClient(ExecutorService emailSendExecutorService,
                       Semaphore emailSendSemaphore,
                       RetryProperties retryProperties) {
        this.executorService = emailSendExecutorService;
        this.semaphore = emailSendSemaphore;
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
     * [비동기 발송] - 가상 스레드풀에 위임
     * - Semaphore로 동시 처리 수 제한 (maxConcurrency=100)
     * - 조회/매핑/렌더링은 이미 완료된 상태
     * - 발송(1초)만 별도 가상 스레드에서 실행
     * - 자체 재시도 포함
     * - 최종 실패 시 콜백 호출
     */
    public void sendAsync(String to, String htmlContent, Long billId, Runnable onFinalFailure) {
        executorService.submit(() -> {
            try {
                // Semaphore로 동시 처리 수 제한
                // - 허가 획득 (없으면 대기, 가상 스레드라서 OS 스레드 안 막힘)
                semaphore.acquire();

                try {
                    int maxAttempts = retryProperties.emailMaxAttempts();
                    long intervalMs = retryProperties.initialIntervalMs();

                    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                        try {
                            send(to, htmlContent);
                            return;  // 성공하면 종료

                        } catch (Exception e) {

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

                } finally {
                    // 반드시 Semaphore 반환
                    semaphore.release();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[EMAIL] Semaphore 대기 중 인터럽트. billId={}", billId);
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
