package com.example.only4_kafka.service;

import com.example.only4_kafka.domain.bill.Bill;
import com.example.only4_kafka.domain.bill.BillRepository;
import com.example.only4_kafka.domain.bill.BillSendStatus;
import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.BillNotification;
import com.example.only4_kafka.domain.bill_notification.BillNotificationRepository;
import com.example.only4_kafka.domain.bill_notification.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillNotificationWriter {
    private final BillNotificationRepository billNotificationRepository;

    @Transactional
    public void updateBillNotificationSendStatus(Long billId, BillChannel channel, SendStatus sendStatus, LocalDateTime processStartTime) {
        // BillNotificationRow
        BillNotification notification = billNotificationRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("BillNotificationRow not found"));

        // 청구서 발송 채널 변경
        notification.changeBillChannel(channel);

        // 청구서 발송 이력 상태 변경
        notification.changeSendStatus(sendStatus);

        if(processStartTime != null) {
            notification.changeProcessStartTime(processStartTime);
        }

        log.info("BillNotification 업데이트 완료 (BillId: {}, BillNotification.sendStatus: {})", billId, notification.getSendStatus());
    }
}
