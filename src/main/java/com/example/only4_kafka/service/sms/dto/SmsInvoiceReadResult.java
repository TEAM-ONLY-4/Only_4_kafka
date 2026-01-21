package com.example.only4_kafka.service.sms.dto;

import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import com.example.only4_kafka.repository.dto.BillNotificationRow;

public record SmsInvoiceReadResult(
    Long memberId,
    Long billId,
    SmsBillDto smsBillDto,
    BillNotificationRow billNotificationRow
) {}
