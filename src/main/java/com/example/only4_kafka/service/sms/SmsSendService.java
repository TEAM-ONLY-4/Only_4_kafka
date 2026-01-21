package com.example.only4_kafka.service.sms;

import com.example.only4_kafka.domain.bill.BillRepository;
import com.example.only4_kafka.domain.bill_notification.BillNotificationRepository;
import com.example.only4_kafka.domain.bill_notification.SendStatus;
import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import com.example.only4_kafka.event.SmsSendRequestEvent;
import com.example.only4_kafka.infrastructure.sms.SmsClient;
import com.example.only4_kafka.service.sms.writer.SmsSendStatusWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
public class SmsSendService {
    private final BillRepository billRepository;
    private final BillNotificationRepository billNotificationRepository;
    private final SmsClient smsClient;
    private final SpringTemplateEngine templateEngine;
    private final SmsSendStatusWriter smsSendStatusWriter;

    public SmsSendService(BillRepository billRepository, BillNotificationRepository billNotificationRepository,
                          SmsClient smsClient, @Qualifier("textTemplateEngine") SpringTemplateEngine templateEngine,
                          SmsSendStatusWriter smsSendStatusWriter, SmsSendStatusWriter smsSendStatusWriter1) {
        this.billRepository = billRepository;
        this.billNotificationRepository = billNotificationRepository;
        this.smsClient = smsClient;
        this.templateEngine = templateEngine;
        this.smsSendStatusWriter = smsSendStatusWriter;
    }


    public void processSms(SmsSendRequestEvent event) {
        // 1. event에서 memberId, billId 추출
        Long billId = event.billId();

        // 2. 이미 해당 청구서가 발송되었는지 확인
        // 문제점 : '아직 발송중'인 경우 체킹 가능? => 발송은 했으나 도착 여부는 모를 때
        boolean isAlreadySent = billNotificationRepository.existsByBillIdAndSendStatus(billId, SendStatus.SENT);

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

        log.info("[SMS 청구서]\n {}", smsBillContent);

        // 4. SMS 발송 (전화번호, 청구서id, 내용)
        smsClient.send(smsBillDto.phoneNumber(), smsBillDto.billId(), smsBillContent);

        // 5. 청구서 발송 상태 변경
        smsSendStatusWriter.updateBillSendStatus(billId);

    }

    // 청구서 Dto -> SMS 텍스트로 변환
    private String getSmsBillContent(SmsBillDto smsBillDto) {
        Context context = new Context();
        context.setVariable("invoice", smsBillDto); // 템플릿에서 invoice로 꺼냄
        return templateEngine.process("SmsBill", context); // SmsBill.txt 파일 읽어서 처리
    }
}
