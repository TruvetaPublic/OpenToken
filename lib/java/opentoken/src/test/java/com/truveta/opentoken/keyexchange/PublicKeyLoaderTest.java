/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PublicKeyLoader}.
 */
class PublicKeyLoaderTest {
    
    @TempDir
    Path tempDir;
    
    private PublicKeyLoader publicKeyLoader;
    private KeyPairManager keyPairManager;
    
    @BeforeEach
    void setUp() {
        publicKeyLoader = new PublicKeyLoader();
        keyPairManager = new KeyPairManager(tempDir.toString());
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Temp directory cleanup is handled automatically by @TempDir
    }
    
    @Test
    void testLoadPublicKey() throws KeyExchangeException {
        // Generate and save a key pair
        KeyPair keyPair = keyPairManager.generateAndSaveKeyPair();
        Path publicKeyPath = Paths.get(tempDir.toString(), KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME);
        
        // Load the public key
        PublicKey loadedKey = publicKeyLoader.loadPublicKey(publicKeyPath.toString());
        
        assertNotNull(loadedKey);
        assertNotNull(loadedKey.getEncoded());
    }
    
    @Test
    void testLoadPublicKeyFromString() throws KeyExchangeException {
        // Generate a key pair
        KeyPair keyPair = keyPairManager.generateKeyPair();
        
        // Convert to PEM string
        String pemContent = PemUtils.encodeToPem(keyPair.getPublic(), "PUBLIC KEY");
        
        // Load from string
        PublicKey loadedKey = publicKeyLoader.loadPublicKeyFromString(pemContent);
        
        assertNotNull(loadedKey);
        assertNotNull(loadedKey.getEncoded());
    }
    
    @Test
    void testLoadPublicKey_FileNotFound() {
        String nonExistentPath = Paths.get(tempDir.toString(), "nonexistent.pem").toString();
        
        assertThrows(KeyExchangeException.class, () -> {
            publicKeyLoader.loadPublicKey(nonExistentPath);
        });
    }
    
    @Test
    void testValidatePublicKey() throws KeyExchangeException {
        KeyPair keyPair = keyPairManager.generateKeyPair();
        
        // Should not throw exception for valid EC public key
        publicKeyLoader.validatePublicKey(keyPair.getPublic());
    }
    
    @Test
    void testValidatePublicKey_Null() {
        assertThrows(KeyExchangeException.class, () -> {
            publicKeyLoader.validatePublicKey(null);
        });
    }
    
    @Test
    void testPublicKeyFileExists() throws KeyExchangeException {
        // Create a key pair file
        keyPairManager.generateAndSaveKeyPair();
        Path publicKeyPath = Paths.get(tempDir.toString(), KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME);
        
        assertTrue(publicKeyLoader.publicKeyFileExists(publicKeyPath.toString()));
        
        // Test non-existent file
        String nonExistentPath = Paths.get(tempDir.toString(), "nonexistent.pem").toString();
        assertFalse(publicKeyLoader.publicKeyFileExists(nonExistentPath));
    }
    
    @Test
    void testLoadAndValidatePublicKey() throws KeyExchangeException {
        // Generate and save a key pair
        keyPairManager.generateAndSaveKeyPair();
        Path publicKeyPath = Paths.get(tempDir.toString(), KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME);
        
        // Load the public key (validation happens automatically)
        PublicKey loadedKey = publicKeyLoader.loadPublicKey(publicKeyPath.toString());
        
        // Additional explicit validation
        publicKeyLoader.validatePublicKey(loadedKey);
        
        assertNotNull(loadedKey);
    }
}
