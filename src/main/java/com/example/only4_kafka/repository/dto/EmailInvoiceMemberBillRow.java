package com.example.only4_kafka.repository.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmailInvoiceMemberBillRow(
        long memberId,
        String memberName,
        String memberPhoneNumber,
        String memberGrade,
        String memberAddress,
        String memberEmail,
        String memberPaymentMethod,
        long billId,
        LocalDate billingYearMonth,
        BigDecimal totalAmount,
        BigDecimal vat,
        BigDecimal unpaidAmount,
        BigDecimal totalDiscountAmount,
        BigDecimal totalBilledAmount,
        LocalDate dueDate,
        LocalDate createdDate,
        String paymentOwnerNameSnapshot,
        String paymentNameSnapshot,
        String paymentNumberSnapshot
) {}
