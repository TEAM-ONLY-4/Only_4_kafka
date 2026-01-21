package com.example.only4_kafka.controller;

import com.example.only4_kafka.event.EmailSendRequestEvent;
import com.example.only4_kafka.global.common.SuccessResponse;
import com.example.only4_kafka.service.email.EmailKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/test/kafka")
@RequiredArgsConstructor
public class TestController {

    private final EmailKafkaProducer emailKafkaProducer;

    @GetMapping("/email/bulk")
    public ResponseEntity<SuccessResponse<String>> sendBulkEmail() {
        IntStream.range(0, 1000).forEach(i -> {
            EmailSendRequestEvent event = EmailSendRequestEvent.builder()
                    .memberId((long) (i + 1))
                    .billId((long) (i + 1000 + 1))
                    .build();
            emailKafkaProducer.send(event);
        });

        return SuccessResponse.of("1000 events published").asHttp(HttpStatus.OK);
    }
}
