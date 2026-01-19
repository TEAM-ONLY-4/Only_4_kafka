package com.example.only4_kafka.service.sms.writer;

import com.example.only4_kafka.domain.bill.Bill;
import com.example.only4_kafka.domain.bill.BillRepository;
import com.example.only4_kafka.domain.bill.BillSendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsSendStatusWriter {
    private final BillRepository billRepository;


    // 질문) bill의 sendStatus vs bill_Notification의 sendStatus는 각각 어떤 용도인가?
    @Transactional
    public void updateBillSendStatus(Long billId) {
        // 청구서 조회
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("청구서 엔티티가 없습니다."));

        // 청구서 발송 상태 변경
        bill.changeSendStatus(BillSendStatus.SENT);

        // 청구서 발송 이력 조회
//        BillNotification notification = billNotificationRepository.findById(billId)
//                .orElseThrow(() -> new IllegalArgumentException("청구서 발송 이력이 없습니다."));
//
//        // 청구서 발송 이력 상태 변경
//        notification.changeSendStatus(BillNotificationStatus.SENT);

        log.info("청구서 발송 상태 업데이트 완료 (BillId: {}, Bill.sendStatus: {})", billId, bill.getBillSendStatus()); // , notification.getSendStatus()
    }
}
