package com.example.only4_kafka.repository.dto;

import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.SendStatus;

import java.time.LocalDateTime;

public record BillNotificationRow(
        long memberId,
        long billId,
        BillChannel billChannel,
        SendStatus sendStatus,
        LocalDateTime processStartTime
) {
}
