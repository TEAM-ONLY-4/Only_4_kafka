package com.example.only4_kafka.service.email.util;

import com.example.only4_kafka.constant.ThymeleafConstant;
import com.example.only4_kafka.service.email.dto.EmailInvoiceTemplateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final TemplateEngine templateEngine;

    public String render(EmailInvoiceTemplateDto emailInvoiceTemplateDto) {
        Context context = new Context();
        context.setVariable("invoice", emailInvoiceTemplateDto);

        return templateEngine.process(ThymeleafConstant.EMAIL_TEMPLATE_NAME, context);
    }
}
