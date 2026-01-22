package com.example.only4_kafka.constant;

public class EmailConstant {

    private EmailConstant() {
    }

    public static final int RECENT_MONTHS_COUNT = 4;
    public static final String DATE_FORMAT = "yyyy.M.d";

    // EmailClient 발송 시뮬레이션
    public static final int SEND_DELAY_MS = 1000;
    public static final int FAILURE_RATE_PERCENT = 50;
}
