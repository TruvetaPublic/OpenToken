/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.unit.tokentransformer;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;

public class EncryptTokenTransformerTest {
    private EncryptTokenTransformer transformer;
    private static final String VALID_KEY = "12345678901234567890123456789012"; // 32-byte key
    private static final String INVALID_KEY = "short-key"; // Invalid short key

    @BeforeEach
    public void setUp() throws Exception {
        transformer = new EncryptTokenTransformer(VALID_KEY);
    }

    @Test
    public void testConstructor_ValidKey_Success() throws Exception {
        EncryptTokenTransformer validTransformer = new EncryptTokenTransformer(VALID_KEY);
        Assertions.assertNotNull(validTransformer);
    }

    @Test
    public void testConstructor_InvalidKeyLength_ThrowsIllegalArgumentException() {
        Exception exception = Assertions.assertThrows(InvalidKeyException.class, () -> {
            new EncryptTokenTransformer(INVALID_KEY); // Key is too short
        });
        Assertions.assertEquals("Key must be 32 characters long", exception.getMessage());
    }

    @Test
    public void testTransform_ValidToken_ReturnsEncryptedToken() throws Exception {
        String token = "mySecretToken";
        String encryptedToken = transformer.transform(token);

        // Ensure the encrypted token is not null or empty
        Assertions.assertNotNull(encryptedToken);
        Assertions.assertFalse(encryptedToken.isEmpty());

        // check if the token is base64-encoded by decoding it
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedToken);
        Assertions.assertNotNull(decodedBytes);
    }

    @Test
    public void testTransform_ReversibleEncryption() throws Exception {
        // Testing if encryption followed by decryption will give back the original
        // token.
        String token = "mySecretToken";

        String encryptedToken = transformer.transform(token); // Encrypt the token

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Decrypt the token using the same settings
        SecretKeySpec secretKey = new SecretKeySpec(VALID_KEY.getBytes(), "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]); // 16-byte IV (all zeroes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedToken));
        String decryptedToken = new String(decryptedBytes, StandardCharsets.UTF_8);

        Assertions.assertEquals(token, decryptedToken); // Ensure the decrypted token matches the original token
    }
}
