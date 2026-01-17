package com.example.only4_kafka.domain.bill;

import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    @Query("""
        SELECT SmsBillDto(
            m.name,
            m.phoneNumber,
            m.doNotDisturbStartTime,
            m.doNotDisturbEndTime,
            
            b.paymentOwnerNameSnapshot,
            b.paymentNameSnapshot,
            b.paymentNumberSnapshot,
            b.dueDate,

            b.id,
            b.billingYearMonth,
            b.totalAmount,
            b.totalDiscountAmount,
            b.unpaidAmount,
            b.totalBilledAmount,
            b.vat,
            CURRENT_DATE
        )
        FROM Bill b
        JOIN b.member m
        WHERE b.id = :billId
    """)
    Optional<SmsBillDto> findSmsBillDtoById(@Param("billId") Long billId);
}
