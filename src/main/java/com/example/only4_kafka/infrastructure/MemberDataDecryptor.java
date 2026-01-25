package com.example.only4_kafka.infrastructure;

import com.example.only4_kafka.infrastructure.encode.Encoder;
import com.example.only4_kafka.infrastructure.encrypt.Encryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 회원 민감정보 복호화 유틸
 * - DB 저장 형식: AES256 암호화 → Base64 인코딩
 * - 복호화 순서: Base64 디코딩 → AES256 복호화
 *
 * 용도별 메서드:
 * - 발송용: decryptEmail(), decryptPhoneNumber() → 원본 반환
 * - 청구서 내용용: decryptAndMaskEmail(), decryptAndMaskPhoneNumber() → 마스킹 반환
 */
@Component
@RequiredArgsConstructor
public class MemberDataDecryptor {

    private final Encoder encoder;
    private final Encryptor encryptor;
    private final MemberDataMasker memberDataMasker;

    // ========== 발송용 (원본) ==========

    // 암호화된 이메일 복호화 (발송용)
    public String decryptEmail(String encryptedEmail) {
        return decrypt(encryptedEmail);
    }

    // 암호화된 전화번호 복호화 (발송용)
    public String decryptPhoneNumber(String encryptedPhoneNumber) {
        return decrypt(encryptedPhoneNumber);
    }

    // ========== 청구서 내용용 (마스킹) ==========

    // 암호화된 이메일 복호화 + 마스킹 (청구서 표시용)
    public String decryptAndMaskEmail(String encryptedEmail) {
        String decrypted = decrypt(encryptedEmail);
        return memberDataMasker.maskEmail(decrypted);
    }

    // 암호화된 전화번호 복호화 + 마스킹 (청구서 표시용)
    public String decryptAndMaskPhoneNumber(String encryptedPhoneNumber) {
        String decrypted = decrypt(encryptedPhoneNumber);
        return memberDataMasker.maskPhoneNumber(decrypted);
    }

    // ========== 내부 메서드 ==========

    private String decrypt(String encryptedBase64) {
        byte[] decoded = encoder.decode(encryptedBase64);
        return encryptor.decrypt(decoded);
    }
}
