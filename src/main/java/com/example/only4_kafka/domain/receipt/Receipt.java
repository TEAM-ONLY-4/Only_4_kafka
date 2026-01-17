package com.example.only4_kafka.domain.receipt;

import com.example.only4_kafka.domain.bill.Bill;
import com.example.only4_kafka.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "receipt")
public class Receipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_to_bill"))
    private Bill bill;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_method", nullable = false, columnDefinition = "payment_method_enum")
    private PaymentMethod paymentMethod;

    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal paidAmount;

    @PrePersist
    private void prePersist() {
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
    }
}
