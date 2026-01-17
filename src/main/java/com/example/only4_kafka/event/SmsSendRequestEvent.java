package com.example.only4_kafka.event;

public record SmsSendRequestEvent(
        Long memberId,
        Long billId
) {}
