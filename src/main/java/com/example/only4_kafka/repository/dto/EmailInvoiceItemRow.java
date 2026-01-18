package com.example.only4_kafka.repository.dto;

import java.math.BigDecimal;

public record EmailInvoiceItemRow(
        String category,
        String subcategory,
        String name,
        BigDecimal amount,
        String detailSnapshot
) {}
