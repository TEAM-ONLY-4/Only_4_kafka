package com.example.only4_kafka.service.sms.mapper;

import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import com.example.only4_kafka.infrastructure.MemberDataDecryptor;
import com.example.only4_kafka.service.sms.dto.SmsInvoiceReadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsInvoiceMapper {

    private final MemberDataDecryptor memberDataDecryptor;

    // ============================================================================
    // [기존 메서드 - SmsInvoiceReadResult를 받는 버전]
    // 선점 로직 적용 전 사용 (기존 코드 호환용)
    // ============================================================================
    public SmsBillDto toDto(SmsInvoiceReadResult result) {
        return toDto(result.smsBillDto());
    }

    // ============================================================================
    // [신규 메서드 - SmsBillDto를 직접 받는 버전]
    // 선점 로직 적용 후 사용 (불필요한 래퍼 객체 제거)
    // ============================================================================
    public SmsBillDto toDto(SmsBillDto original) {
        // 전화번호 복호화 (발송용 - 원본)
        String decryptedPhoneNumber = memberDataDecryptor.decryptPhoneNumber(original.phoneNumber());
        // 전화번호 복호화 + 마스킹 (청구서 표시용)
        String maskedPhoneNumber = memberDataDecryptor.decryptAndMaskPhoneNumber(original.phoneNumber());

        return new SmsBillDto(
            original.name(),
            decryptedPhoneNumber,
            maskedPhoneNumber,
            original.doNotDisturbStartTime(),
            original.doNotDisturbEndTime(),
            original.paymentOwnerName(),
            original.paymentName(),
            original.paymentNumber(),
            original.dueDate(),
            original.billId(),
            original.billingYearMonth(),
            original.totalAmount(),
            original.totalDiscountAmount(),
            original.unpaidAmount(),
            original.totalBilledAmount(),
            original.totalMonthlyAmount(),
            original.totalOverageAmount(),
            original.totalMicroAmount(),
            original.vat(),
            original.createdDate()
        );
    }
}
