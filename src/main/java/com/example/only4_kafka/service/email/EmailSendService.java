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
        log.info("Email send requested. memberId={}, billId={}", event.memberId(), event.billId());

        // 1. Data fetch
        EmailInvoiceReadResult emailInvoiceReadResult = emailInvoiceReader.read(event.memberId(), event.billId());
        log.info("Data fetch done. memberId={}, billId={}", event.memberId(), event.billId());

        // 2. Map to template DTO
        log.info("Template DTO mapping start. memberId={}, billId={}", event.memberId(), event.billId());
        EmailInvoiceTemplateDto emailInvoiceTemplateDto = emailInvoiceMapper.toDto(emailInvoiceReadResult);
        log.info("Template DTO mapping done. memberId={}, billId={}", event.memberId(), event.billId());

        // 3. Render HTML
        log.info("HTML render start. memberId={}, billId={}", event.memberId(), event.billId());
        String htmlContent = emailTemplateRenderer.render(emailInvoiceTemplateDto);
        log.info("HTML render done. memberId={}, billId={}, contentLength={}",
                event.memberId(), event.billId(), htmlContent.length());

        // 4. Send email
        String memberEmail = emailInvoiceReadResult.memberBill().memberEmail();
        log.info("Email send start. memberId={}, billId={}, email={}",
                event.memberId(), event.billId(), memberEmail);
        emailClient.send(memberEmail, htmlContent);
        log.info("Email send done. memberId={}, billId={}", event.memberId(), event.billId());

        // 5. update bill status
    }
}
