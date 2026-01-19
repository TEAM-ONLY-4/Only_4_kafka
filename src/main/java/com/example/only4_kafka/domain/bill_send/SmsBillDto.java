package com.example.only4_kafka.domain.bill_send;

import com.example.only4_kafka.domain.receipt.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record SmsBillDto(
        // 고객 정보
        String name,
        String phoneNumber,
        LocalTime doNotDisturbStartTime,
        LocalTime doNotDisturbEndTime,

        // 납부 정보
        String paymentOwnerName,
        String paymentName,
        String paymentNumber,
        LocalDate dueDate, // 납기일

        // 명세서 정보
        Long billId,
        LocalDate billingYearMonth, // 청구연월
        BigDecimal totalAmount, // 총 사용 금액
        BigDecimal totalDiscountAmount, // 총 할인 금액
        BigDecimal unpaidAmount, // 미납금
        BigDecimal totalBilledAmount, // 총 청구 금액 (실제 납부)
        BigDecimal totalMonthlyAmount, // 월정액
        BigDecimal totalOverageAmount, // 초과 사용료
        BigDecimal totalMicroAmount, // 소액 결제
        BigDecimal vat, // 부가가치세
        LocalDate createdDate // 작성 일자
) {
}
