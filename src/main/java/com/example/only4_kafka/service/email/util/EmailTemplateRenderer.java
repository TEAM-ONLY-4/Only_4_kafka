package com.example.only4_kafka.service.email.util;

import com.example.only4_kafka.constant.ThymeleafConstant;
import com.example.only4_kafka.service.email.dto.EmailInvoiceTemplateDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class EmailTemplateRenderer {

    private final TemplateEngine templateEngine;

    public EmailTemplateRenderer(@Qualifier("htmlTemplateEngine") TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(EmailInvoiceTemplateDto emailInvoiceTemplateDto) {
        Context context = new Context();
        context.setVariable("invoice", emailInvoiceTemplateDto);

        return templateEngine.process(ThymeleafConstant.EMAIL_TEMPLATE_NAME, context);
    }
}
