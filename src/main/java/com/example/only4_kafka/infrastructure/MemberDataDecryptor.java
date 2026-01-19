package com.example.only4_kafka.infrastructure;

import com.example.only4_kafka.infrastructure.encode.Encoder;
import com.example.only4_kafka.infrastructure.encrypt.Encryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 민감정보 복호화 유틸
 * - DB 저장 형식: AES256 암호화 → Base64 인코딩
 * - 복호화 순서: Base64 디코딩 → AES256 복호화
 */
@Component
@RequiredArgsConstructor
public class MemberDataDecryptor {

    private final Encoder encoder;
    private final Encryptor encryptor;

    // 암호화된 이메일 복호화
    public String decryptEmail(String encryptedEmail) {
        return decrypt(encryptedEmail);
    }

    // 암호화된 전화번호 복호화
    public String decryptPhoneNumber(String encryptedPhoneNumber) {
        return decrypt(encryptedPhoneNumber);
    }

    private String decrypt(String encryptedBase64) {
        byte[] decoded = encoder.decode(encryptedBase64);
        return encryptor.decrypt(decoded);
    }
}
