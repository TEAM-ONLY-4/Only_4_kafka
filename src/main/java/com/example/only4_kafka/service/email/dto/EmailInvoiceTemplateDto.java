package com.example.only4_kafka.service.email.dto;

import java.math.BigDecimal;
import java.util.List;

public record EmailInvoiceTemplateDto(
        // 기본 정보
        int billingYear,
        int billingMonth,
        String customerName,
        String phoneNumber,
        String customerGrade,
        String customerAddress,

        // 청구서 정보
        String invoiceNumber,
        String usagePeriod,
        String issueDate,
        String dueDate,

        // 금액 정보
        BigDecimal totalPayment,
        BigDecimal usageAmount,
        BigDecimal discountAmount,
        BigDecimal unpaidAmount,

        // 당월요금 상세
        BigDecimal monthlyFee,
        BigDecimal roundingDiscount,
        BigDecimal vat,
        BigDecimal additionalFee,

        // 최근 4개월 요금내역
        List<RecentMonth> recentMonthList,

        // 상품 정보
        List<Product> productList,

        // 할인내역
        List<Discount> discountList,
        BigDecimal totalDiscountAmount,

        // 결제정보
        String paymentHolder,
        String paymentName,
        String paymentNumber
) {
    public record RecentMonth(
            String month,
            BigDecimal monthlyFee,
            BigDecimal additionalFee,
            BigDecimal roundingDiscount,
            BigDecimal vat,
            BigDecimal discountAmount,
            BigDecimal totalPayment
    ) {}

    public record Product(
            String name,
            BigDecimal amount,
            String category,
            String feeType,
            String spec
    ) {}

    public record Discount(
            String name,
            BigDecimal amount
    ) {}
}
