package com.example.only4_kafka.repository.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentBillRow(
        LocalDate billingYearMonth,
        BigDecimal totalAmount,
        BigDecimal vat,
        BigDecimal totalDiscountAmount,
        BigDecimal totalBilledAmount,
        BigDecimal monthlyFee,
        BigDecimal additionalFee
) {}
