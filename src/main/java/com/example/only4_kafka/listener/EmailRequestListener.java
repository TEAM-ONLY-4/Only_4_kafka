package com.example.only4_kafka.listener;

import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.service.email.EmailSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * [비활성화됨] - EmailParallelConsumer로 대체
 *
 * 이메일 발송 요청 Kafka 리스너 (기존 방식)
 *
 * [발송만 비동기 처리 방식]
 * 1) Kafka에서 메시지 수신
 * 2) 조회 → 매핑 → 렌더링 (동기, 리스너 스레드에서 실행)
 * 3) 발송만 스레드풀에 위임 (EmailClient.sendAsync)
 * 4) 리스너는 발송 완료를 기다리지 않고 다음 메시지 처리
 */
@Slf4j
@RequiredArgsConstructor
// @Component  // EmailParallelConsumer 사용으로 비활성화
public class EmailRequestListener {

    private final EmailSendService emailSendService;

    @KafkaListener(
            topics = "${app.kafka.topics.email-request}",
            groupId = "${app.kafka.topics.group-id}",
            containerFactory = "emailKafkaListenerContainerFactory"
    )
    public void listen(
            EmailSendRequestEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        // 조회 → 매핑 → 렌더링 (동기) → 발송 위임 (비동기)
        emailSendService.send(event);
    }
}
