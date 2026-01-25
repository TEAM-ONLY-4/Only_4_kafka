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

    // ============================================================================
    // [기존 메서드 - 선점 로직 적용 전 사용]
    // 주의: 이 메서드는 billNotification을 중복 조회함
    //       선점 로직 적용 후에는 readSmsBillDto() 사용 권장
    // ============================================================================
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

    // ============================================================================
    // [신규 메서드 - 선점 로직 적용 후 사용]
    // 선점 성공 시 BillNotificationRow는 tryPreempt()에서 이미 반환받으므로
    // SmsBillDto만 조회하면 됨 (불필요한 SELECT 1회 제거)
    // ============================================================================
    @Transactional(readOnly = true)
    public SmsBillDto readSmsBillDto(Long billId) {
        return queryRepository.findSmsBillDto(billId, LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("Bill not found for billId: " + billId));
    }
}
