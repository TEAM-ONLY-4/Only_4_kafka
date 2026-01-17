package com.example.only4_kafka.service.email.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmailInvoiceDto(
        Long memberId,
        Long billId,
        Header header,
        Customer customer,
        InvoiceInfo invoiceInfo,
        AmountSummary amountSummary,
        MonthlyFeeDetails monthlyFeeDetails,
        List<ProductDetail> productDetails,
        List<DiscountDetail> discounts,
        PaymentInfo paymentInfo
) {
    public record Header(
            int billingYear,
            int billingMonth
    ) {}

    public record Customer(
            String name,
            String phoneNumber,
            String grade,
            String address,
            String email
    ) {}

    public record InvoiceInfo(
            String invoiceNumber,
            String usagePeriod,
            LocalDate issueDate,
            LocalDate dueDate
    ) {}

    public record AmountSummary(
            BigDecimal totalPayment,
            BigDecimal usageAmount,
            BigDecimal discountAmount,
            BigDecimal unpaidAmount
    ) {}

    public record MonthlyFeeDetails(
            BigDecimal monthlyFee,
            BigDecimal roundingDiscount,
            BigDecimal vat,
            BigDecimal additionalFee
    ) {}

    public record ProductDetail(
            String mainProduct,
            String productCategory,
            String feeType,
            BigDecimal productAmount,
            String productSpecHtml
    ) {}

    public record DiscountDetail(
            String name,
            BigDecimal amount
    ) {}

    public record PaymentInfo(
            String paymentType,
            String paymentHolder,
            String paymentName,
            String paymentNumber
    ) {}
}
