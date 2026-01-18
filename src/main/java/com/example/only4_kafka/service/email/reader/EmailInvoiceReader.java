package com.example.only4_kafka.service.email.reader;

import com.example.only4_kafka.constant.EmailConstant;
import com.example.only4_kafka.repository.InvoiceQueryRepository;
import com.example.only4_kafka.repository.dto.EmailInvoiceItemRow;
import com.example.only4_kafka.repository.dto.EmailInvoiceMemberBillRow;
import com.example.only4_kafka.repository.dto.RecentBillRow;
import com.example.only4_kafka.service.email.dto.EmailInvoiceReadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailInvoiceReader {

    private final InvoiceQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public EmailInvoiceReadResult read(Long memberId, Long billId) {
        EmailInvoiceMemberBillRow memberBill = queryRepository.findMemberBill(memberId, billId)
                .orElseThrow(() -> new IllegalArgumentException("Member/Bill not found."));

        List<EmailInvoiceItemRow> itemRowList = queryRepository.findBillItems(billId);

        List<RecentBillRow> recentBillList = queryRepository.findRecentBills(
                memberId,
                memberBill.billingYearMonth(),
                EmailConstant.RECENT_MONTHS_COUNT
        );

        return new EmailInvoiceReadResult(memberId, billId, memberBill, itemRowList, recentBillList);
    }

}
