package com.example.only4_kafka.service;

import com.example.only4_kafka.event.SmsSendRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsSendService {
    public void send(SmsSendRequestEvent message) {
    }
}
