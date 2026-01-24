package com.example.only4_kafka.listener;

import com.example.only4_kafka.config.properties.KafkaTopicsProperties;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.service.email.EmailSendService;
import io.confluent.parallelconsumer.ParallelStreamProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 이메일 Parallel Consumer
 *
 * [기존 @KafkaListener와 차이점]
 * - 기존: 파티션 단위 병렬 (8개)
 * - Parallel Consumer: 메시지 단위 병렬 (100개)
 *
 * [동작 방식]
 * 1. poll()로 메시지 수신
 * 2. 람다 내에서 전체 처리 (DB조회 + 렌더링 + 발송)
 * 3. 람다 정상 종료 → offset 완료 마킹
 * 4. 주기적으로 완료된 offset 커밋
 *
 * [ACK]
 * - 처리 완료된 메시지만 커밋
 * - 유실/중복 최소화
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class EmailParallelConsumer {

    private final ParallelStreamProcessor<String, EmailSendRequestEvent> processor;
    private final EmailSendService emailSendService;
    private final KafkaTopicsProperties topicsProperties;

    @PostConstruct
    public void start() {
        String topic = topicsProperties.emailRequest();

        processor.subscribe(List.of(topic));
        log.info("[EmailParallelConsumer] 토픽 구독: {}", topic);

        processor.poll(context -> {
            EmailSendRequestEvent event = context.getSingleConsumerRecord().value();
            log.debug("[EmailParallelConsumer] 처리 시작. memberId={}, billId={}",
                    event.memberId(), event.billId());

            // 전체 처리 (DB조회 + 렌더링 + 발송)
            // 동기 처리 → 완료 후 offset 마킹
            emailSendService.send(event);

            log.debug("[EmailParallelConsumer] 처리 완료. memberId={}, billId={}",
                    event.memberId(), event.billId());
        });

        log.info("[EmailParallelConsumer] poll 시작");
    }

    @PreDestroy
    public void stop() {
        log.info("[EmailParallelConsumer] 종료 중...");
        processor.close();
        log.info("[EmailParallelConsumer] 종료 완료");
    }
}
