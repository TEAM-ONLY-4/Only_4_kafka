package com.example.only4_kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 이메일 발송 병렬 처리를 위한 스레드풀 설정
 *
 * [왜 별도 스레드풀?]
 * - Kafka 리스너 스레드에서 직접 처리하면 발송 1초 블로킹
 * - 별도 스레드풀에 위임하여 병렬 처리
 *
 * [주의]
 * - 스레드풀에 위임하면 Kafka ErrorHandler 재시도 안 됨
 * - 자체 재시도 로직 필요 (EmailSendExecutor에서 처리)
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 이메일 발송용 스레드풀
     * - fixedThreadPool: 고정 크기 스레드풀
     * - 크기 20: 동시에 20개 이메일 발송 가능
     * - 조정 필요시: DB 커넥션 풀 크기, 외부 API 제한 등 고려
     */
    @Bean
    public ExecutorService emailSendExecutorService() {
        int poolSize = 150;
        int queueCapacity = 10000;

        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
