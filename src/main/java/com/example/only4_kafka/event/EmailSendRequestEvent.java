package com.example.only4_kafka.event;

import lombok.Builder;

@Builder
public record EmailSendRequestEvent(
        Long memberId,
        Long billId
) {}
