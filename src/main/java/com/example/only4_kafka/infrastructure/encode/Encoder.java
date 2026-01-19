package com.example.only4_kafka.infrastructure.encode;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class Encoder {

    private final Base64.Encoder encoder = Base64.getEncoder();
    // PostgreSQL encode(..., 'base64')는 MIME 형식 (76자마다 줄바꿈)
    private final Base64.Decoder decoder = Base64.getMimeDecoder();

    // 바이트 배열을 Base64 문자열로 인코딩
    public String encode(byte[] data) {
        return encoder.encodeToString(data);
    }

    // 문자열을 Base64 문자열로 인코딩
    public String encode(String plainText) {
        return encoder.encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
    }

    // Base64 문자열을 바이트 배열로 디코딩
    public byte[] decode(String base64String) {
        return decoder.decode(base64String);
    }

    // Base64 문자열을 일반 문자열로 디코딩
    public String decodeToString(String base64String) {
        return new String(decoder.decode(base64String), StandardCharsets.UTF_8);
    }
}
