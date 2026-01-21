package com.example.only4_kafka.event;

import lombok.Builder;

@Builder
public record SmsSendRequestEvent(
        Long memberId,
        Long billId
) {}
