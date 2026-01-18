package com.example.only4_kafka.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafConfig {

    // Thymeleaf 템플릿 엔진 사용해서 Txt도 처리하도록 Config 파일 설정
    // Thymeleaf는 기본적으로 HTML을 처리할 수 있으므로 이 파일과 관련 X
    // SMS 템플릿을 처리할 때만 사용
    @Bean
    public SpringTemplateEngine textTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/"); // 경로 설정
        templateResolver.setSuffix(".txt");        // .txt 파일 읽기
        templateResolver.setTemplateMode(TemplateMode.TEXT); // 텍스트 모드
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false); // 개발 중엔 캐시 끄기

        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }
}