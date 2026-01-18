package com.example.only4_kafka.service.email;

import com.example.only4_kafka.event.EmailSendRequestEvent;
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

    public void send(EmailSendRequestEvent event) {
        log.info("이메일 발송 요청. memberId={}, billId={}", event.memberId(), event.billId());

        // 1. 데이터 조회
        EmailInvoiceReadResult emailInvoiceReadResult = emailInvoiceReader.read(event.memberId(), event.billId());

        // 2. 템플릿용 DTO로 변환
        EmailInvoiceTemplateDto emailInvoiceTemplateDto = emailInvoiceMapper.toDto(emailInvoiceReadResult);

        // 3. HTML 렌더링
        String htmlContent = emailTemplateRenderer.render(emailInvoiceTemplateDto);

        // 4. 이메일 발송
        String memberEmail = emailInvoiceReadResult.memberBill().memberEmail();
        emailClient.send(memberEmail, htmlContent);
    }
}
