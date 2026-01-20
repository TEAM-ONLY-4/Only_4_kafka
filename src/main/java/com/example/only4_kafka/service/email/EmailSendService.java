package com.example.only4_kafka.service.email;

import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.infrastructure.MemberDataDecryptor;
import com.example.only4_kafka.infrastructure.email.EmailClient;
import com.example.only4_kafka.service.email.dto.EmailInvoiceTemplateDto;
import com.example.only4_kafka.service.email.dto.EmailInvoiceReadResult;
import com.example.only4_kafka.service.email.mapper.EmailInvoiceMapper;
import com.example.only4_kafka.service.email.reader.EmailInvoiceReader;
import com.example.only4_kafka.service.email.util.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailSendService {

    private final EmailInvoiceReader emailInvoiceReader;
    private final EmailInvoiceMapper emailInvoiceMapper;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final EmailClient emailClient;
    private final MemberDataDecryptor memberDataDecryptor;

    public void send(EmailSendRequestEvent event) {
        log.info("1) 이메일 전송 요청. memberId={}, billId={}", event.memberId(), event.billId());

        // 1. Data fetch
        EmailInvoiceReadResult emailInvoiceReadResult = emailInvoiceReader.read(event.memberId(), event.billId());
        log.info("2) 데이터 조회 완료. memberId={}, billId={}", event.memberId(), event.billId());

        // 2. Map to template DTO
        log.info("3) 템플릿 DTO 매핑 시작. memberId={}, billId={}", event.memberId(), event.billId());
        EmailInvoiceTemplateDto emailInvoiceTemplateDto = emailInvoiceMapper.toDto(emailInvoiceReadResult);
        log.info("4) 템플릿 DTO 매핑 완료. memberId={}, billId={}", event.memberId(), event.billId());

        // 3. Render HTML
        log.info("5) HTML 렌더링 시작. memberId={}, billId={}", event.memberId(), event.billId());
        String htmlContent = emailTemplateRenderer.render(emailInvoiceTemplateDto);
        log.info("6) HTML 렌더링 완료. memberId={}, billId={}, contentLength={}",
                event.memberId(), event.billId(), htmlContent.length());
        log.info("6-1) HTML 청구서 결과 \n {}", htmlContent);

        // 4. Send email
        String encryptedEmail = emailInvoiceReadResult.memberBill().memberEmail();
        String memberEmail = memberDataDecryptor.decryptEmail(encryptedEmail);
        log.info("7) Email 전송 요청 시작. memberId={}, billId={}, email={}",
                event.memberId(), event.billId(), memberEmail);
        emailClient.send(memberEmail, htmlContent);
        log.info("8) Email 전송 요청 종료. memberId={}, billId={}", event.memberId(), event.billId());

        // 5. update bill status
    }
}
