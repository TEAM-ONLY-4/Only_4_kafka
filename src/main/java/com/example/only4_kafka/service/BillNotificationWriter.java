package com.example.only4_kafka.service;

import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.SendStatus;
import com.example.only4_kafka.global.exception.BusinessException;
import com.example.only4_kafka.global.exception.ErrorCode;
import com.example.only4_kafka.repository.InvoiceQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillNotificationWriter {
    private final InvoiceQueryRepository invoiceQueryRepository;

    @Transactional
    public void updateBillNotificationSendStatus(Long billId, BillChannel channel, SendStatus sendStatus, LocalDateTime processStartTime) {
        int updatedCount = invoiceQueryRepository.updateBillNotification(billId, channel, sendStatus, processStartTime);
        if (updatedCount == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        log.info("BillNotification 업데이트 완료 (BillId: {}, Channel: {}, SendStatus: {})", billId, channel, sendStatus);
    }
}