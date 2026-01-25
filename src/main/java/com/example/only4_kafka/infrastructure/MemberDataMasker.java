package com.example.only4_kafka.infrastructure;

import org.springframework.stereotype.Component;

/**
 * 회원 민감정보 마스킹 유틸
 * - 청구서에 표시할 때 사용
 * - 전화번호: 010-1234-5678 -> 010-****-5678
 * - 이메일: test@email.com -> te**@email.com
 */
@Component
public class MemberDataMasker {

    private static final String PHONE_MASK = "****";
    private static final String EMAIL_MASK = "****";

    /**
     * 전화번호 마스킹
     * - 010-1234-5678 -> 010-****-5678
     * - 01012345678 -> 010****5678
     */
    public String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }

        // 하이픈 포함 여부 확인
        if (phoneNumber.contains("-")) {
            return maskPhoneNumberWithHyphen(phoneNumber);
        } else {
            return maskPhoneNumberWithoutHyphen(phoneNumber);
        }
    }

    /**
     * 이메일 마스킹
     * - test@email.com -> te**@email.com
     * - ab@email.com -> ab@email.com (2자 이하면 마스킹 안함)
     * - abcdef@email.com -> ab****@email.com
     */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        // 2자 이하면 마스킹 안함
        if (localPart.length() <= 2) {
            return email;
        }

        // 앞 2자 + 마스킹 + 도메인
        String visiblePart = localPart.substring(0, 2);
        return visiblePart + EMAIL_MASK + domainPart;
    }

    // 하이픈 포함: 010-1234-5678 -> 010-****-5678
    private String maskPhoneNumberWithHyphen(String phoneNumber) {
        String[] parts = phoneNumber.split("-");
        if (parts.length != 3) {
            return phoneNumber;
        }
        return parts[0] + "-" + PHONE_MASK + "-" + parts[2];
    }

    // 하이픈 미포함: 01012345678 -> 010****5678
    private String maskPhoneNumberWithoutHyphen(String phoneNumber) {
        if (phoneNumber.length() < 7) {
            return phoneNumber;
        }
        String prefix = phoneNumber.substring(0, 3);
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + PHONE_MASK + suffix;
    }
}
