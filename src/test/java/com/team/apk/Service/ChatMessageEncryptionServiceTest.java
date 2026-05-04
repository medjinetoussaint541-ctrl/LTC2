package com.team.apk.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ChatMessageEncryptionServiceTest {

    @Test
    void shouldEncryptAndDecryptMessage() {
        ChatMessageEncryptionService service = new ChatMessageEncryptionService("ltc-chat-secret-test-2026");

        ChatMessageEncryptionService.EncryptedPayload payload = service.encrypt("Bonjour mon coeur");
        String decrypted = service.decrypt(payload.ivBase64(), payload.cipherTextBase64());

        assertEquals("Bonjour mon coeur", decrypted);
    }

    @Test
    void shouldUseRandomIvForEachEncryption() {
        ChatMessageEncryptionService service = new ChatMessageEncryptionService("ltc-chat-secret-test-2026");

        ChatMessageEncryptionService.EncryptedPayload first = service.encrypt("Message identique");
        ChatMessageEncryptionService.EncryptedPayload second = service.encrypt("Message identique");

        assertNotEquals(first.ivBase64(), second.ivBase64());
        assertNotEquals(first.cipherTextBase64(), second.cipherTextBase64());
    }
}
