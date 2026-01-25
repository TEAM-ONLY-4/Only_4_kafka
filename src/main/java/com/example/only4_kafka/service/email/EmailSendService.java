package com.example.only4_kafka.service.email;

import com.example.only4_kafka.config.properties.RetryProperties;
import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.infrastructure.MemberDataDecryptor;
import com.example.only4_kafka.infrastructure.email.EmailClient;
import com.example.only4_kafka.repository.dto.BillNotificationRow;
import com.example.only4_kafka.service.BillNotificationWriter;
import com.example.only4_kafka.service.email.dto.EmailInvoiceReadResult;
import com.example.only4_kafka.service.email.dto.EmailInvoiceTemplateDto;
import com.example.only4_kafka.service.email.mapper.EmailInvoiceMapper;
import com.example.only4_kafka.service.email.reader.EmailInvoiceReader;
import com.example.only4_kafka.service.email.util.EmailTemplateRenderer;
import com.example.only4_kafka.service.sms.SmsKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailSendService {

    private final EmailInvoiceReader emailInvoiceReader;
    private final EmailInvoiceMapper emailInvoiceMapper;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailClient emailClient;
    private final MemberDataDecryptor memberDataDecryptor;
    private final BillNotificationWriter billNotificationWriter;
    private final RetryProperties retryProperties;
    private final SmsKafkaProducer smsKafkaProducer;
    private final ExecutorService emailSendExecutorService;

    // ============================================================================
    // [선점 타임아웃 설정]
    // SENDING 상태에서 이 시간(초)이 지나면 타임아웃으로 간주하고 재선점 허용
    // ============================================================================
    private static final int PREEMPT_TIMEOUT_SECONDS = 10;

    // ============================================================================
    // [신규 로직 - 원자적 선점 기반]
    //
    // 기존 문제점:
    //   1. SELECT로 상태 조회 → 상태 체크 → UPDATE (Check-Then-Act 패턴)
    //   2. 조회와 업데이트 사이에 다른 스레드가 개입 가능 (Race Condition)
    //   3. 결과: 같은 bill_notification을 여러 스레드가 동시에 처리 → 중복 발송
    //
    // 해결 방법:
    //   1. 먼저 선점(tryPreempt) 시도 → 원자적 UPDATE로 한 스레드만 성공
    //   2. 선점 실패 시 → 다른 스레드가 처리 중이므로 조용히 종료 (할 일 없음)
    //   3. 선점 성공 시 → 데이터 조회 → 렌더링 → 발송 → 완료 상태 업데이트
    //
    // 흐름도:
    //   Kafka 메시지 수신
    //         │
    //         ▼
    //   tryPreempt(billId) ──── 실패(empty) ───→ return (다른 스레드가 처리 중)
    //         │ 성공
    //         ▼
    //   데이터 조회 (EmailInvoiceReader)
    //         │
    //         ▼
    //   템플릿 렌더링
    //         │
    //         ▼
    //   이메일 발송 (비동기)
    //         ├── 성공 → completeWithSuccess(billId) → SENT
    //         │
    //         └── 실패 → completeWithFailure(billId) → FAILED
    //                    SMS 폴백 발행
    // ============================================================================

    public void send(EmailSendRequestEvent event) {
        Long billId = event.billId();
        Long memberId = event.memberId();

        // [STEP 1] 선점 시도 (가장 먼저!)
        Optional<BillNotificationRow> preemptedOptional = billNotificationWriter.tryPreempt(
                billId,
                BillChannel.EMAIL,
                PREEMPT_TIMEOUT_SECONDS
        );

        if (preemptedOptional.isEmpty()) {
            // 선점 실패: 다른 스레드가 처리 중이거나, 이미 SENT/FAILED 상태
            // → 이 메시지는 처리할 필요 없음, 정상 종료
            log.info("[EMAIL_SKIP] billId={} 선점 실패. 다른 스레드가 처리 중이거나 이미 완료됨", billId);
            return;
        }

        // [STEP 2] 데이터 조회
        // - 선점 성공 후에 조회
        EmailInvoiceReadResult emailInvoiceReadResult = emailInvoiceReader.read(memberId, billId);

        // [STEP 3] 템플릿 렌더링
        EmailInvoiceTemplateDto emailInvoiceTemplateDto = emailInvoiceMapper.toDto(emailInvoiceReadResult);
        String htmlContent = emailTemplateRenderer.render(emailInvoiceTemplateDto);

        // [STEP 4] 이메일 주소 복호화
        String encryptedEmail = emailInvoiceReadResult.memberBill().memberEmail();
        String memberEmail = memberDataDecryptor.decryptEmail(encryptedEmail);

        // [STEP 5] SENT 상태로 변경 (발송 전에!)
        // - 중복 발송 절대 방지 > 유실 허용 (유실은 추적 가능)
        // - 발송 성공 후 SENT 변경 시, 변경 실패하면 재발송 위험
        // - 따라서 SENT 먼저 → 발송 순서가 안전
        billNotificationWriter.completeWithSuccess(billId);

        // [STEP 6] 이메일 발송 (비동기)
        // - 이미 SENT 상태이므로 다른 스레드가 재처리 불가
        emailSendExecutorService.submit(() -> {
            try {
                emailClient.send(memberEmail, htmlContent);
            } catch (Exception e) {
                log.error("[EMAIL_FAILED] billId={} 이메일 발송 실패. SMS 전환. error={}", billId, e.getMessage(), e);
                // SMS 폴백 시도 (FAILED 상태 변경 안 함 - 이미 SENT로 표시됨)
                try {
                    smsKafkaProducer.send(new SmsSendRequestEvent(memberId, billId));
                    log.info("[SMS_FALLBACK_TRIGGERED] billId={} SMS 폴백 발행 완료", billId);
                } catch (Exception ex) {
                    // SMS 발행도 실패한 경우
                    // TODO: DLT(Dead Letter Topic)로 전송하여 수동 처리 필요
                    log.error("[SMS_FALLBACK_FAILED] billId={} SMS 폴백 발행 실패. error={}", billId, ex.getMessage(), ex);
                }
            }
        });
    }
}
