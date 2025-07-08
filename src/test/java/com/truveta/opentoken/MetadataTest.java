/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MetadataTest {

    @Test
    void testInitializeWithoutSecrets() {
        Metadata metadata = new Metadata();
        Map<String, Object> result = metadata.initialize(null, null);

        assertTrue(result.containsKey(Metadata.JAVA_VERSION));
        assertTrue(result.containsKey(Metadata.PLATFORM));
        assertTrue(result.containsKey(Metadata.OPENTOKEN_VERSION));

        assertFalse(result.containsKey(Metadata.HASHING_SECRET_HASH));
        assertFalse(result.containsKey(Metadata.ENCRYPTION_SECRET_HASH));

        assertEquals(Metadata.PLATFORM_JAVA, result.get(Metadata.PLATFORM));
        assertEquals(Metadata.DEFAULT_VERSION, result.get(Metadata.OPENTOKEN_VERSION));
    }

    @Test
    void testInitializeWithSecrets() {
        Metadata metadata = new Metadata();
        String hashingSecret = "test-hashing-secret";
        String encryptionKey = "test-encryption-key";

        Map<String, Object> result = metadata.initialize(hashingSecret, encryptionKey);

        assertTrue(result.containsKey(Metadata.HASHING_SECRET_HASH));
        assertTrue(result.containsKey(Metadata.ENCRYPTION_SECRET_HASH));

        assertNotNull(result.get(Metadata.HASHING_SECRET_HASH));
        assertNotNull(result.get(Metadata.ENCRYPTION_SECRET_HASH));

        // Verify hashes are different for different inputs
        assertNotEquals(result.get(Metadata.HASHING_SECRET_HASH),
                result.get(Metadata.ENCRYPTION_SECRET_HASH));
    }

    @Test
    void testInitializeWithOnlyHashingSecret() {
        Metadata metadata = new Metadata();
        String hashingSecret = "test-hashing-secret";

        Map<String, Object> result = metadata.initialize(hashingSecret, null);

        assertTrue(result.containsKey(Metadata.HASHING_SECRET_HASH));
        assertFalse(result.containsKey(Metadata.ENCRYPTION_SECRET_HASH));

        assertNotNull(result.get(Metadata.HASHING_SECRET_HASH));
    }

    @Test
    void testInitializeWithOnlyEncryptionKey() {
        Metadata metadata = new Metadata();
        String encryptionKey = "test-encryption-key";

        Map<String, Object> result = metadata.initialize(null, encryptionKey);

        assertFalse(result.containsKey(Metadata.HASHING_SECRET_HASH));
        assertTrue(result.containsKey(Metadata.ENCRYPTION_SECRET_HASH));

        assertNotNull(result.get(Metadata.ENCRYPTION_SECRET_HASH));
    }

    @Test
    void testInitializeWithEmptySecrets() {
        Metadata metadata = new Metadata();

        Map<String, Object> result = metadata.initialize("", "");

        assertFalse(result.containsKey(Metadata.HASHING_SECRET_HASH));
        assertFalse(result.containsKey(Metadata.ENCRYPTION_SECRET_HASH));
    }

    @Test
    void testCalculateSecureHashWithValidInput() {
        String input = "test-input";
        String hash = Metadata.calculateSecureHash(input);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertEquals(64, hash.length()); // SHA-256 produces 64 character hex string

        // Verify the hash is consistent
        String hash2 = Metadata.calculateSecureHash(input);
        assertEquals(hash, hash2);
    }

    @Test
    void testCalculateSecureHashWithKnownValue() {
        // Test with a known SHA-256 value to ensure compatibility
        String input = "hello";
        String expectedHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

        String actualHash = Metadata.calculateSecureHash(input);
        assertEquals(expectedHash, actualHash);
    }

    @Test
    void testCalculateSecureHashWithDifferentInputs() {
        String input1 = "input1";
        String input2 = "input2";

        String hash1 = Metadata.calculateSecureHash(input1);
        String hash2 = Metadata.calculateSecureHash(input2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testCalculateSecureHashWithNullInput() {
        String hash = Metadata.calculateSecureHash(null);
        assertNull(hash);
    }

    @Test
    void testCalculateSecureHashWithEmptyInput() {
        String hash = Metadata.calculateSecureHash("");
        assertNull(hash);
    }

    @Test
    void testCalculateSecureHashWithUnicodeInput() {
        String input = "こんにちは"; // Japanese "hello"
        String hash = Metadata.calculateSecureHash(input);

        assertNotNull(hash);
        assertEquals(64, hash.length());

        // Verify UTF-8 encoding produces consistent results
        String hash2 = Metadata.calculateSecureHash(input);
        assertEquals(hash, hash2);
    }

    @Test
    void testMetadataConstants() {
        // Verify that the new constants are properly defined
        assertNotNull(Metadata.ENCRYPTION_SECRET_HASH);
        assertNotNull(Metadata.HASHING_SECRET_HASH);

        assertEquals("EncryptionSecretHash", Metadata.ENCRYPTION_SECRET_HASH);
        assertEquals("HashingSecretHash", Metadata.HASHING_SECRET_HASH);
    }

    @Test
    void testHashCalculationExceptionCreation() {
        String message = "Test message";
        Exception cause = new RuntimeException("Test cause");

        Metadata.HashCalculationException exception = new Metadata.HashCalculationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
