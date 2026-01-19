package com.example.only4_kafka.infrastructure.encrypt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;

@Component
public class Encryptor {

    // PostgreSQL pgp_sym_encrypt에서 사용한 키
    private static final String SECRET_KEY = "my_secret_key";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * PostgreSQL pgp_sym_encrypt로 암호화된 데이터를 복호화
     * (Base64 디코딩된 바이트 배열을 입력으로 받음)
     */
    public String decrypt(byte[] encryptedData) {
        try {
            JcaPGPObjectFactory pgpObjectFactory = new JcaPGPObjectFactory(encryptedData);
            Object pgpObject = pgpObjectFactory.nextObject();

            // 암호화된 데이터 리스트 추출
            PGPEncryptedDataList encryptedDataList;
            if (pgpObject instanceof PGPEncryptedDataList) {
                encryptedDataList = (PGPEncryptedDataList) pgpObject;
            } else {
                encryptedDataList = (PGPEncryptedDataList) pgpObjectFactory.nextObject();
            }

            // 비밀번호 기반 암호화 데이터 추출
            PGPPBEEncryptedData pbeEncryptedData = (PGPPBEEncryptedData) encryptedDataList.get(0);

            // 복호화 팩토리 생성
            JcePBEDataDecryptorFactoryBuilder decryptorFactoryBuilder =
                    new JcePBEDataDecryptorFactoryBuilder(
                            new JcaPGPDigestCalculatorProviderBuilder()
                                    .setProvider("BC")
                                    .build()
                    ).setProvider("BC");

            // 복호화 스트림 열기
            InputStream decryptedStream = pbeEncryptedData.getDataStream(
                    decryptorFactoryBuilder.build(SECRET_KEY.toCharArray())
            );

            // 복호화된 데이터에서 원본 추출
            JcaPGPObjectFactory plainFactory = new JcaPGPObjectFactory(decryptedStream);
            Object message = plainFactory.nextObject();

            // 압축된 경우 처리
            if (message instanceof PGPCompressedData) {
                PGPCompressedData compressedData = (PGPCompressedData) message;
                plainFactory = new JcaPGPObjectFactory(compressedData.getDataStream());
                message = plainFactory.nextObject();
            }

            // 리터럴 데이터에서 평문 추출
            if (message instanceof PGPLiteralData) {
                PGPLiteralData literalData = (PGPLiteralData) message;
                InputStream literalStream = literalData.getInputStream();

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = literalStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }

                return outputStream.toString(StandardCharsets.UTF_8);
            }

            throw new RuntimeException("예상치 못한 PGP 메시지 형식");

        } catch (Exception e) {
            throw new RuntimeException("PGP 복호화 실패", e);
        }
    }
}
