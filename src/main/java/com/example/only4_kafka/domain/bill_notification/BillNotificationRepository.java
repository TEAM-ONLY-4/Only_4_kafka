package com.example.only4_kafka.domain.bill_notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillNotificationRepository extends JpaRepository<BillNotification, Long> {
    // 청구서의 발송 상태 (READY, SENT, FAILED) 확인
    boolean existsByBillIdAndSendStatus(Long billId, SendStatus sendStatus);
}
