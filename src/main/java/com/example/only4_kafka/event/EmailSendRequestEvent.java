package com.example.only4_kafka.event;

public record EmailSendRequestEvent(
        Long memberId,
        Long billId
) {}
