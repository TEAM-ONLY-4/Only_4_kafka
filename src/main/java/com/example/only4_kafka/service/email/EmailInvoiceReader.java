package com.example.only4_kafka.service.email;

import com.example.only4_kafka.repository.InvoiceQueryRepository;
import com.example.only4_kafka.repository.dto.EmailInvoiceItemRow;
import com.example.only4_kafka.repository.dto.EmailInvoiceMemberBillRow;
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

        List<EmailInvoiceItemRow> itemRows = queryRepository.findBillItems(billId);

        return new EmailInvoiceReadResult(memberId, billId, memberBill, itemRows);
    }

}
