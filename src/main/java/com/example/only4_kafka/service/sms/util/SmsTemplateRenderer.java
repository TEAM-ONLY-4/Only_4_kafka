package com.example.only4_kafka.service.sms.util;

import com.example.only4_kafka.domain.bill_send.SmsBillDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Component
public class SmsTemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    public SmsTemplateRenderer(@Qualifier("textTemplateEngine") SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(SmsBillDto smsBillDto) {
        Context context = new Context();
        context.setVariable("invoice", smsBillDto);
        // SmsBill.txt 템플릿 사용
        return templateEngine.process("SmsBill", context);
    }
}
