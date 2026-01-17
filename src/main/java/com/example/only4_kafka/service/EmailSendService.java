package com.example.only4_kafka.service;

import com.example.only4_kafka.event.EmailSendRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailSendService {

    public void send(EmailSendRequestEvent event) {
        log.info("Received email send request. memberId={}, billId={}", event.memberId(), event.billId());
    }
}
