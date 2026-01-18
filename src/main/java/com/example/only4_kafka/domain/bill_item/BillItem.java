package com.example.only4_kafka.domain.bill_item;

import com.example.only4_kafka.domain.bill.Bill;
import com.example.only4_kafka.domain.common.BaseEntity;
import com.example.only4_kafka.domain.product.BillItemSubcategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "bill_item")
public class BillItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_item_to_bill"))
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "item_category", nullable = false, columnDefinition = "bill_item_category_enum")
    private BillItemCategory itemCategory;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "item_subcategory", columnDefinition = "bill_item_subcategory_enum")
    private BillItemSubcategory itemSubcategory;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> detailSnapshot;

    @PrePersist
    private void prePersist() {
        if (amount == null) amount = BigDecimal.ZERO;
    }
}
