/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link KeyPairManager}.
 */
class KeyPairManagerTest {
    
    @TempDir
    Path tempDir;
    
    private KeyPairManager keyPairManager;
    private String keyDirectory;
    
    @BeforeEach
    void setUp() {
        keyDirectory = tempDir.toString();
        keyPairManager = new KeyPairManager(keyDirectory);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Temp directory cleanup is handled automatically by @TempDir
    }
    
    @Test
    void testGenerateKeyPair() throws KeyExchangeException {
        KeyPair keyPair = keyPairManager.generateKeyPair();
        
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
        assertEquals(KeyPairManager.EC_ALGORITHM, keyPair.getPrivate().getAlgorithm());
        assertEquals(KeyPairManager.EC_ALGORITHM, keyPair.getPublic().getAlgorithm());
        
        // Verify it's an EC key with correct curve
        assertTrue(keyPair.getPublic() instanceof ECPublicKey);
        ECPublicKey ecPublicKey = (ECPublicKey) keyPair.getPublic();
        assertEquals(256, ecPublicKey.getParams().getCurve().getField().getFieldSize());
    }
    
    @Test
    void testGenerateAndSaveKeyPair() throws KeyExchangeException {
        KeyPair keyPair = keyPairManager.generateAndSaveKeyPair();
        
        assertNotNull(keyPair);
        
        // Verify files were created
        Path privateKeyPath = Paths.get(keyDirectory, KeyPairManager.DEFAULT_PRIVATE_KEY_FILENAME);
        Path publicKeyPath = Paths.get(keyDirectory, KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME);
        
        assertTrue(Files.exists(privateKeyPath));
        assertTrue(Files.exists(publicKeyPath));
        
        // Verify PEM format
        try {
            String privateKeyContent = new String(Files.readAllBytes(privateKeyPath));
            String publicKeyContent = new String(Files.readAllBytes(publicKeyPath));
            
            assertTrue(privateKeyContent.contains("-----BEGIN PRIVATE KEY-----"));
            assertTrue(privateKeyContent.contains("-----END PRIVATE KEY-----"));
            assertTrue(publicKeyContent.contains("-----BEGIN PUBLIC KEY-----"));
            assertTrue(publicKeyContent.contains("-----END PUBLIC KEY-----"));
        } catch (IOException e) {
            throw new AssertionError("Failed to read key files", e);
        }
    }
    
    @Test
    void testSaveAndLoadKeyPair() throws KeyExchangeException {
        // Generate and save
        KeyPair originalKeyPair = keyPairManager.generateAndSaveKeyPair();
        
        // Load
        Path privateKeyPath = Paths.get(keyDirectory, KeyPairManager.DEFAULT_PRIVATE_KEY_FILENAME);
        KeyPair loadedKeyPair = keyPairManager.loadKeyPair(privateKeyPath.toString());
        
        assertNotNull(loadedKeyPair);
        assertArrayEquals(originalKeyPair.getPrivate().getEncoded(), loadedKeyPair.getPrivate().getEncoded());
        assertArrayEquals(originalKeyPair.getPublic().getEncoded(), loadedKeyPair.getPublic().getEncoded());
    }
    
    @Test
    void testGetOrCreateKeyPair_CreatesNew() throws KeyExchangeException {
        KeyPair keyPair = keyPairManager.getOrCreateKeyPair();
        
        assertNotNull(keyPair);
        
        // Verify files were created
        Path privateKeyPath = Paths.get(keyDirectory, KeyPairManager.DEFAULT_PRIVATE_KEY_FILENAME);
        assertTrue(Files.exists(privateKeyPath));
    }
    
    @Test
    void testGetOrCreateKeyPair_LoadsExisting() throws KeyExchangeException {
        // First call creates
        KeyPair firstKeyPair = keyPairManager.getOrCreateKeyPair();
        
        // Second call loads
        KeyPair secondKeyPair = keyPairManager.getOrCreateKeyPair();
        
        // Should be the same key pair
        assertArrayEquals(firstKeyPair.getPrivate().getEncoded(), secondKeyPair.getPrivate().getEncoded());
        assertArrayEquals(firstKeyPair.getPublic().getEncoded(), secondKeyPair.getPublic().getEncoded());
    }
    
    @Test
    void testLoadKeyPair_FileNotFound() {
        String nonExistentPath = Paths.get(keyDirectory, "nonexistent.pem").toString();
        
        assertThrows(KeyExchangeException.class, () -> {
            keyPairManager.loadKeyPair(nonExistentPath);
        });
    }
    
    @Test
    void testSavePublicKey() throws KeyExchangeException {
        KeyPair keyPair = keyPairManager.generateKeyPair();
        String publicKeyPath = Paths.get(keyDirectory, "test_public.pem").toString();
        
        keyPairManager.savePublicKey(keyPair.getPublic(), publicKeyPath);
        
        assertTrue(Files.exists(Paths.get(publicKeyPath)));
        
        // Verify content
        try {
            String content = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
            assertTrue(content.contains("-----BEGIN PUBLIC KEY-----"));
            assertTrue(content.contains("-----END PUBLIC KEY-----"));
        } catch (IOException e) {
            throw new AssertionError("Failed to read public key file", e);
        }
    }
    
    @Test
    void testSavePrivateKey() throws KeyExchangeException {
        KeyPair keyPair = keyPairManager.generateKeyPair();
        String privateKeyPath = Paths.get(keyDirectory, "test_private.pem").toString();
        
        keyPairManager.savePrivateKey(keyPair.getPrivate(), privateKeyPath);
        
        assertTrue(Files.exists(Paths.get(privateKeyPath)));
        
        // Verify content
        try {
            String content = new String(Files.readAllBytes(Paths.get(privateKeyPath)));
            assertTrue(content.contains("-----BEGIN PRIVATE KEY-----"));
            assertTrue(content.contains("-----END PRIVATE KEY-----"));
        } catch (IOException e) {
            throw new AssertionError("Failed to read private key file", e);
        }
    }
    
    @Test
    void testGetKeyDirectory() {
        assertEquals(keyDirectory, keyPairManager.getKeyDirectory());
    }
    
    @Test
    void testDefaultConstructor() {
        KeyPairManager defaultManager = new KeyPairManager();
        assertEquals(KeyPairManager.DEFAULT_KEY_DIR, defaultManager.getKeyDirectory());
    }
}
