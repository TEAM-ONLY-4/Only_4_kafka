package com.example.only4_kafka.domain.bill_notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillNotificationRepository extends JpaRepository<BillNotification, Long> {
}
