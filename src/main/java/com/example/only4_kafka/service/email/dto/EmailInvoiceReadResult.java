package com.example.only4_kafka.service.email.dto;

import com.example.only4_kafka.repository.dto.EmailInvoiceItemRow;
import com.example.only4_kafka.repository.dto.EmailInvoiceMemberBillRow;
import com.example.only4_kafka.repository.dto.RecentBillRow;

import java.util.List;

public record EmailInvoiceReadResult(
        Long memberId,
        Long billId,
        EmailInvoiceMemberBillRow memberBill,
        List<EmailInvoiceItemRow> items,
        List<RecentBillRow> recentBills
) {}
