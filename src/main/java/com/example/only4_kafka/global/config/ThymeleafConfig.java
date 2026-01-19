package com.example.only4_kafka.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafConfig {

    // 이메일 HTML 템플릿용 엔진
    @Primary
    @Bean
    public SpringTemplateEngine htmlTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }

    // SMS 텍스트 템플릿용 엔진
    @Bean
    public SpringTemplateEngine textTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".txt");
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}