package org.nastya.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    private final SecretKey secretKey;

    public EncryptionService(@Value("${crypto.secret}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES");
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new IllegalStateException("Failed to encrypt data", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)));
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new IllegalStateException("Failed to decrypt data", e);
        }
    }

    public String maskCardNumber(String plainText) {
        if (plainText.length() < 4) {
            return "****";
        }
        return "**** **** **** " + plainText.substring(plainText.length() - 4);
    }
}

