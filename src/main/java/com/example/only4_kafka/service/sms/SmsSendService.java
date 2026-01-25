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
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class SmsSendService {

    private final SmsInvoiceReader smsInvoiceReader;
    private final SmsInvoiceMapper smsInvoiceMapper;
    private final SmsTemplateRenderer smsTemplateRenderer;
    private final SmsClient smsClient;
    private final BillNotificationWriter billNotificationWriter;
    private final RetryProperties retryProperties;

    // ============================================================================
    // [선점 타임아웃 설정]
    // SENDING 상태에서 이 시간(초)이 지나면 타임아웃으로 간주하고 재선점 허용
    // TODO: 추후 application.yml로 외부화 권장 (app.kafka.preempt.sms-timeout-seconds)
    // ============================================================================
    private static final int PREEMPT_TIMEOUT_SECONDS = 30;

    // ============================================================================
    // [신규 로직 - 원자적 선점 기반]
    //
    // 흐름도:
    //   Kafka 메시지 수신
    //         ▼
    //   tryPreempt(billId) ──── 실패(empty) ───→ return (다른 스레드가 처리 중)
    //         │ 성공
    //         ▼
    //   데이터 조회 (SmsInvoiceReader.readSmsBillDto)
    //         ▼
    //   전화번호 복호화 (SmsInvoiceMapper)
    //         ▼
    //   템플릿 렌더링
    //         ▼
    //   SENT 상태 변경 (completeWithSuccess)
    //         ▼
    //   SMS 발송 (동기)
    //         └── 실패 시 → 예외 발생 → Kafka 재시도 → 최종 실패 시 DLT
    // ============================================================================

    public void send(SmsSendRequestEvent event) {
        Long billId = event.billId();

        // [STEP 1] 선점 시도 (가장 먼저!)
        Optional<BillNotificationRow> preemptedOptional = billNotificationWriter.tryPreempt(
                billId,
                BillChannel.SMS,
                PREEMPT_TIMEOUT_SECONDS
        );

        if (preemptedOptional.isEmpty()) {
            // 선점 실패: 다른 스레드가 처리 중이거나, 이미 SENT/FAILED 상태
            log.info("[SMS_SKIP] billId={} 선점 실패. 다른 스레드가 처리 중이거나 이미 완료됨", billId);
            return;
        }

        // [STEP 2] 데이터 조회
        SmsBillDto smsBillDto = smsInvoiceReader.readSmsBillDto(billId);

        // [STEP 3] 전화번호 복호화
        SmsBillDto decryptedSmsBillDto = smsInvoiceMapper.toDto(smsBillDto);

        // [STEP 4] 템플릿 렌더링
        String smsContent = smsTemplateRenderer.render(decryptedSmsBillDto);

        // [STEP 5] SENT 상태로 변경 (발송 전에!)
        billNotificationWriter.completeWithSuccess(billId);

        // [STEP 6] SMS 발송 (동기)
        smsClient.send(decryptedSmsBillDto.phoneNumber(), billId, smsContent);
        log.info("[SMS_COMPLETE] billId={} SMS 발송 완료", billId);
    }
}
