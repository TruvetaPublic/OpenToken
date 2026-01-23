/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.truveta.opentoken.keyexchange.KeyExchange.DerivedKeys;

/**
 * Unit tests for {@link KeyExchange}.
 */
class KeyExchangeTest {
    
    private KeyExchange keyExchange;
    private KeyPairManager keyPairManager;
    
    @BeforeEach
    void setUp() {
        keyExchange = new KeyExchange();
        keyPairManager = new KeyPairManager();
    }
    
    @Test
    void testPerformKeyExchange() throws KeyExchangeException {
        // Generate two key pairs (simulating sender and receiver)
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        // Perform key exchange
        byte[] sharedSecret = keyExchange.performKeyExchange(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        assertNotNull(sharedSecret);
        assertTrue(sharedSecret.length > 0);
        
        // Verify symmetric property: both parties should derive the same shared secret
        byte[] sharedSecret2 = keyExchange.performKeyExchange(
            receiverKeyPair.getPrivate(),
            senderKeyPair.getPublic()
        );
        
        assertArrayEquals(sharedSecret, sharedSecret2);
    }
    
    @Test
    void testDeriveHashingKey() throws KeyExchangeException {
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        byte[] sharedSecret = keyExchange.performKeyExchange(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        byte[] hashingKey = keyExchange.deriveHashingKey(sharedSecret);
        
        assertNotNull(hashingKey);
        assertEquals(KeyExchange.KEY_LENGTH, hashingKey.length);
    }
    
    @Test
    void testDeriveEncryptionKey() throws KeyExchangeException {
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        byte[] sharedSecret = keyExchange.performKeyExchange(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        byte[] encryptionKey = keyExchange.deriveEncryptionKey(sharedSecret);
        
        assertNotNull(encryptionKey);
        assertEquals(KeyExchange.KEY_LENGTH, encryptionKey.length);
    }
    
    @Test
    void testDeriveKeys() throws KeyExchangeException {
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        byte[] sharedSecret = keyExchange.performKeyExchange(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        DerivedKeys keys = keyExchange.deriveKeys(sharedSecret);
        
        assertNotNull(keys);
        assertNotNull(keys.getHashingKey());
        assertNotNull(keys.getEncryptionKey());
        assertEquals(KeyExchange.KEY_LENGTH, keys.getHashingKey().length);
        assertEquals(KeyExchange.KEY_LENGTH, keys.getEncryptionKey().length);
        
        // Verify that hashing and encryption keys are different
        boolean keysAreDifferent = false;
        byte[] hashingKey = keys.getHashingKey();
        byte[] encryptionKey = keys.getEncryptionKey();
        for (int i = 0; i < hashingKey.length; i++) {
            if (hashingKey[i] != encryptionKey[i]) {
                keysAreDifferent = true;
                break;
            }
        }
        assertTrue(keysAreDifferent, "Hashing and encryption keys should be different");
    }
    
    @Test
    void testExchangeAndDeriveKeys() throws KeyExchangeException {
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        assertNotNull(keys);
        assertNotNull(keys.getHashingKey());
        assertNotNull(keys.getEncryptionKey());
    }
    
    @Test
    void testSymmetricKeyDerivation() throws KeyExchangeException {
        // Generate two key pairs
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        // Sender derives keys
        DerivedKeys senderKeys = keyExchange.exchangeAndDeriveKeys(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        // Receiver derives keys
        DerivedKeys receiverKeys = keyExchange.exchangeAndDeriveKeys(
            receiverKeyPair.getPrivate(),
            senderKeyPair.getPublic()
        );
        
        // Both should derive the same keys
        assertArrayEquals(senderKeys.getHashingKey(), receiverKeys.getHashingKey());
        assertArrayEquals(senderKeys.getEncryptionKey(), receiverKeys.getEncryptionKey());
    }
    
    @Test
    void testDerivedKeysAsString() throws KeyExchangeException {
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        String hashingKeyString = keys.getHashingKeyAsString();
        String encryptionKeyString = keys.getEncryptionKeyAsString();
        
        assertNotNull(hashingKeyString);
        assertNotNull(encryptionKeyString);
        assertEquals(KeyExchange.KEY_LENGTH, hashingKeyString.length());
        assertEquals(KeyExchange.KEY_LENGTH, encryptionKeyString.length());
    }
    
    @Test
    void testDerivedKeysGettersReturnCopies() throws KeyExchangeException {
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        // Get keys twice
        byte[] hashingKey1 = keys.getHashingKey();
        byte[] hashingKey2 = keys.getHashingKey();
        
        // They should be equal but not the same object
        assertArrayEquals(hashingKey1, hashingKey2);
        assertTrue(hashingKey1 != hashingKey2, "Getters should return copies, not references");
        
        // Modify one copy
        hashingKey1[0] = (byte) 0xFF;
        
        // The other copy should be unchanged
        byte[] hashingKey3 = keys.getHashingKey();
        assertTrue(hashingKey3[0] != (byte) 0xFF, "Modification to copy should not affect internal state");
    }
    
    @Test
    void testConsistentDerivationWithSameSharedSecret() throws KeyExchangeException {
        KeyPair senderKeyPair = keyPairManager.generateKeyPair();
        KeyPair receiverKeyPair = keyPairManager.generateKeyPair();
        
        byte[] sharedSecret = keyExchange.performKeyExchange(
            senderKeyPair.getPrivate(),
            receiverKeyPair.getPublic()
        );
        
        // Derive keys multiple times from the same shared secret
        DerivedKeys keys1 = keyExchange.deriveKeys(sharedSecret);
        DerivedKeys keys2 = keyExchange.deriveKeys(sharedSecret);
        
        // Should be identical
        assertArrayEquals(keys1.getHashingKey(), keys2.getHashingKey());
        assertArrayEquals(keys1.getEncryptionKey(), keys2.getEncryptionKey());
    }
}
