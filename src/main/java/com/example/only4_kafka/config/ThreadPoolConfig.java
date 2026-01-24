package com.example.only4_kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 이메일 발송 병렬 처리를 위한 가상 스레드 설정
 *
 * [가상 스레드 (Virtual Thread) - Java 21]
 * - 플랫폼 스레드보다 1000배 가벼움 (~1KB vs ~1MB)
 * - I/O 블로킹 시 자동으로 다른 가상 스레드 실행
 * - 태스크마다 새 가상 스레드 생성
 *
 * [동시 처리 수 제한]
 * - Semaphore로 동시 실행 수 제한 (DB 커넥션 풀, 외부 API 제한 고려)
 * - maxConcurrency 초과 시 대기 (가상 스레드라서 OS 스레드 안 막힘)
 *
 * [주의]
 * - 스레드풀에 위임하면 Kafka ACK 타이밍 불일치
 * - DB 상태 체크로 멱등성 보장 필요 (EmailSendService.checkBillNotification)
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 동시 처리 수 제한
     * - DB 커넥션 풀 크기, 외부 API 제한 등 고려
     * - 이메일 발송은 1초 I/O이므로 100개면 초당 ~100건 처리 가능
     */
    private static final int MAX_CONCURRENCY = 1000;

    /**
     * 동시 실행 수 제한용 Semaphore
     * - acquire(): 허가 획득 (없으면 대기)
     * - release(): 허가 반환
     */
    @Bean
    public Semaphore emailSendSemaphore() {
        return new Semaphore(MAX_CONCURRENCY);
    }

    /**
     * 이메일 발송용 가상 스레드 Executor
     * - 태스크마다 새 가상 스레드 생성
     * - Semaphore와 함께 사용하여 동시 처리 수 제한
     */
    @Bean
    public ExecutorService emailSendExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
//        return Executors.newFixedThreadPool(100);
    }
}
