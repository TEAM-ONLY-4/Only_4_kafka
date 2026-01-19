package com.example.only4_kafka.service;

import com.example.only4_kafka.domain.bill.Bill;
import com.example.only4_kafka.domain.bill.BillRepository;
import com.example.only4_kafka.domain.bill.BillSendStatus;
import com.example.only4_kafka.domain.bill_notification.BillChannel;
import com.example.only4_kafka.domain.bill_notification.BillNotification;
import com.example.only4_kafka.domain.bill_notification.BillNotificationRepository;
import com.example.only4_kafka.domain.bill_notification.BillNotificationStatus;
import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.infrastructure.sms.SmsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
public class SmsSendService {
    private final BillRepository billRepository;
    private final BillNotificationRepository billNotificationRepository;
    private final SmsClient smsClient;
    private final SpringTemplateEngine templateEngine;

    public SmsSendService(BillRepository billRepository, BillNotificationRepository billNotificationRepository,
                          SmsClient smsClient, @Qualifier("textTemplateEngine") SpringTemplateEngine templateEngine) {
        this.billRepository = billRepository;
        this.billNotificationRepository = billNotificationRepository;
        this.smsClient = smsClient;
        this.templateEngine = templateEngine;
    }


    public void processSms(SmsSendRequestEvent event) {
        // 1. event에서 memberId, billId 추출
        Long billId = event.billId();

        // 2. 이미 해당 청구서가 발송되었는지 확인
        // 문제점 : '아직 발송중'인 경우 체킹 가능? => 발송은 했으나 도착 여부는 모를 때
        boolean isAlreadySent = billNotificationRepository.existsByBillIdAndSendStatus(billId, BillNotificationStatus.SENT);

        if(isAlreadySent) {
            log.info("[중복 방지] 이미 발송 완료된 청구서입니다. (BillId: {})", billId);
            // 추후 nonRetryableError로 설정하고 재시도 안하는 로직 추가하기
            // 예외 처리로 변경하기
            return;
        }

        // 3. 필요한 정보 추출해 청구서 Dto 생성
        SmsBillDto smsBillDto = billRepository.findSmsBillDtoById(billId, java.time.LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("해당 청구서가 없습니다."));

        // 4. Dto 기반으로 String Teplate 생성
        String smsBillContent = getSmsBillContent(smsBillDto);

        // 4. SMS 발송 (전화번호, 청구서id, 내용)
        smsClient.send(smsBillDto.phoneNumber(), smsBillDto.billId(), smsBillContent);

        // 5. 청구서 발송 상태 변경
        updateBillSendStatus(billId);

    }

    // 질문) bill의 sendStatus vs bill_Notification의 sendStatus는 각각 어떤 용도인가?
    @Transactional
    private void updateBillSendStatus(Long billId) {
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

    // 청구서 Dto -> SMS 텍스트로 변환
    private String getSmsBillContent(SmsBillDto smsBillDto) {
        Context context = new Context();
        context.setVariable("invoice", smsBillDto); // 템플릿에서 invoice로 꺼냄
        return templateEngine.process("SmsBill", context); // SmsBill.txt 파일 읽어서 처리
    }
}
