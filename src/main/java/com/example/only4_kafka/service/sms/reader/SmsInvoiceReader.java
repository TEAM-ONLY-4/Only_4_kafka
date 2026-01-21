package com.example.only4_kafka.service.sms.reader;

import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import com.example.only4_kafka.repository.InvoiceQueryRepository;
import com.example.only4_kafka.repository.dto.BillNotificationRow;
import com.example.only4_kafka.service.sms.dto.SmsInvoiceReadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SmsInvoiceReader {
    private final InvoiceQueryRepository queryRepository;

    @Transactional(readOnly = true)
    public SmsInvoiceReadResult read(Long billId) {
        // 청구서의 billNotification 가져오기
        BillNotificationRow billNotification = queryRepository.findBillNotification(billId)
                .orElseThrow(() -> new IllegalArgumentException("BillNotification not found."));

        // SMS용 청구서 정보 가져오기
        SmsBillDto smsBillDto = queryRepository.findSmsBillDto(billId, LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("Bill not found."));

        return new SmsInvoiceReadResult(
                billNotification.memberId(),
                billNotification.billId(),
                smsBillDto,
                billNotification
        );
    }
}
