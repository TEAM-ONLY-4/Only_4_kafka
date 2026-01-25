package com.example.only4_kafka.service;

import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.SendStatus;
import com.example.only4_kafka.global.exception.BusinessException;
import com.example.only4_kafka.global.exception.ErrorCode;
import com.example.only4_kafka.repository.InvoiceQueryRepository;
import com.example.only4_kafka.repository.dto.BillNotificationRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillNotificationWriter {
    private final InvoiceQueryRepository invoiceQueryRepository;

    /**
     * bill_notification 선점 시도
     * 선점 성공 조건:
     *   - send_status = PENDING (아직 아무도 처리 안 함)
     *   - send_status = SENDING AND 타임아웃 초과 (이전 처리자가 실패하고 복구 못 함)
     * 선점 실패 (empty 반환):
     *   - send_status = SENDING AND 타임아웃 미초과 (다른 스레드가 처리 중)
     *   - send_status = SENT (이미 완료됨)
     *   - send_status = FAILED (이미 실패 처리됨)
     */
    @Transactional
    public Optional<BillNotificationRow> tryPreempt(Long billId, BillChannel channel, int timeoutSeconds) {
        Optional<BillNotificationRow> preemptedRow = invoiceQueryRepository.tryPreemptForSending(billId, channel, timeoutSeconds);
        return preemptedRow;
    }

    @Transactional
    public void completeWithSuccess(Long billId) {
        int updatedCount = invoiceQueryRepository.updateSendStatusComplete(billId, SendStatus.SENT);
    }
}
