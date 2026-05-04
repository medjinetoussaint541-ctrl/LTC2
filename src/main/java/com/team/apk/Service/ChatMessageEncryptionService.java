package com.team.apk.Service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public ChatMessageEncryptionService(@Value("${app.chat.encryption-secret}") String encryptionSecret) {
        String normalizedSecret = encryptionSecret == null ? "" : encryptionSecret.trim();
        this.secretKey = normalizedSecret.isBlank() ? null : buildSecretKey(normalizedSecret);
    }

    public EncryptedPayload encrypt(String plainText) {
        try {
            requireSecretKey();
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return new EncryptedPayload(
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(encryptedBytes));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Impossible de chiffrer le message du chat.", exception);
        }
    }

    public String decrypt(String ivBase64, String cipherTextBase64) {
        try {
            requireSecretKey();
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            byte[] encryptedBytes = Base64.getDecoder().decode(cipherTextBase64);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(encryptedBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Impossible de déchiffrer le message du chat.", exception);
        }
    }

    private void requireSecretKey() {
        if (secretKey == null) {
            throw new IllegalStateException(
                    "La clé de chiffrement du chat est absente. Renseigne APP_CHAT_ENCRYPTION_SECRET sur l'environnement de déploiement.");
        }
    }

    private SecretKey buildSecretKey(String encryptionSecret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Impossible d'initialiser le chiffrement du chat.", exception);
        }
    }

    public record EncryptedPayload(String ivBase64, String cipherTextBase64) {
    }
}
