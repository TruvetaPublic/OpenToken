/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Elliptic Curve Diffie-Hellman (ECDH) key exchange and key derivation.
 * <p>
 * This class performs ECDH key agreement between two parties and derives
 * separate hashing and encryption keys using HKDF (HMAC-based Key Derivation Function).
 * The derived keys are used for HMAC-SHA256 hashing and AES-256-GCM encryption.
 */
public class KeyExchange {
    private static final Logger logger = LoggerFactory.getLogger(KeyExchange.class);
    
    /**
     * ECDH algorithm name.
     */
    public static final String ECDH_ALGORITHM = "ECDH";
    
    /**
     * HMAC algorithm for key derivation.
     */
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    
    /**
     * Salt for hashing key derivation (must be consistent across implementations).
     */
    public static final String HASHING_KEY_SALT = "HASHING";
    
    /**
     * Info string for hashing key derivation (must be consistent across implementations).
     */
    public static final String HASHING_KEY_INFO = "OpenToken-Hash";
    
    /**
     * Salt for encryption key derivation (must be consistent across implementations).
     */
    public static final String ENCRYPTION_KEY_SALT = "ENCRYPTION";
    
    /**
     * Info string for encryption key derivation (must be consistent across implementations).
     */
    public static final String ENCRYPTION_KEY_INFO = "OpenToken-Encrypt";
    
    /**
     * Length of derived keys in bytes (32 bytes = 256 bits).
     */
    public static final int KEY_LENGTH = 32;
    
    /**
     * Performs ECDH key agreement between sender and receiver.
     *
     * @param senderPrivateKey the sender's private key
     * @param receiverPublicKey the receiver's public key
     * @return the shared secret bytes
     * @throws KeyExchangeException if key agreement fails
     */
    public byte[] performKeyExchange(PrivateKey senderPrivateKey, PublicKey receiverPublicKey) 
            throws KeyExchangeException {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(ECDH_ALGORITHM);
            keyAgreement.init(senderPrivateKey);
            keyAgreement.doPhase(receiverPublicKey, true);
            
            byte[] sharedSecret = keyAgreement.generateSecret();
            logger.debug("Performed ECDH key exchange, shared secret length: {} bytes", sharedSecret.length);
            
            return sharedSecret;
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalStateException e) {
            throw new KeyExchangeException("Failed to perform ECDH key exchange", e);
        }
    }
    
    /**
     * Derives a hashing key from the shared secret using HKDF.
     *
     * @param sharedSecret the shared secret from ECDH
     * @return the derived hashing key (32 bytes)
     * @throws KeyExchangeException if key derivation fails
     */
    public byte[] deriveHashingKey(byte[] sharedSecret) throws KeyExchangeException {
        return hkdf(sharedSecret, HASHING_KEY_SALT, HASHING_KEY_INFO, KEY_LENGTH);
    }
    
    /**
     * Derives an encryption key from the shared secret using HKDF.
     *
     * @param sharedSecret the shared secret from ECDH
     * @return the derived encryption key (32 bytes)
     * @throws KeyExchangeException if key derivation fails
     */
    public byte[] deriveEncryptionKey(byte[] sharedSecret) throws KeyExchangeException {
        return hkdf(sharedSecret, ENCRYPTION_KEY_SALT, ENCRYPTION_KEY_INFO, KEY_LENGTH);
    }
    
    /**
     * Derives both hashing and encryption keys from the shared secret.
     *
     * @param sharedSecret the shared secret from ECDH
     * @return a DerivedKeys object containing both keys
     * @throws KeyExchangeException if key derivation fails
     */
    public DerivedKeys deriveKeys(byte[] sharedSecret) throws KeyExchangeException {
        byte[] hashingKey = deriveHashingKey(sharedSecret);
        byte[] encryptionKey = deriveEncryptionKey(sharedSecret);
        
        logger.debug("Derived hashing key ({} bytes) and encryption key ({} bytes)", 
                    hashingKey.length, encryptionKey.length);
        
        return new DerivedKeys(hashingKey, encryptionKey);
    }
    
    /**
     * Performs a complete key exchange and derivation.
     *
     * @param senderPrivateKey the sender's private key
     * @param receiverPublicKey the receiver's public key
     * @return a DerivedKeys object containing both derived keys
     * @throws KeyExchangeException if key exchange or derivation fails
     */
    public DerivedKeys exchangeAndDeriveKeys(PrivateKey senderPrivateKey, PublicKey receiverPublicKey) 
            throws KeyExchangeException {
        byte[] sharedSecret = performKeyExchange(senderPrivateKey, receiverPublicKey);
        return deriveKeys(sharedSecret);
    }
    
    /**
     * HKDF (HMAC-based Key Derivation Function) implementation.
     * <p>
     * This is a simplified HKDF implementation suitable for our use case.
     * It performs the Extract-and-Expand paradigm as defined in RFC 5869.
     *
     * @param ikm the input keying material (shared secret)
     * @param salt the salt value
     * @param info the application-specific info string
     * @param length the desired output length in bytes
     * @return the derived key material
     * @throws KeyExchangeException if key derivation fails
     */
    private byte[] hkdf(byte[] ikm, String salt, String info, int length) throws KeyExchangeException {
        try {
            // Step 1: Extract - generate a pseudorandom key (PRK)
            byte[] prk = hkdfExtract(salt.getBytes(StandardCharsets.UTF_8), ikm);
            
            // Step 2: Expand - expand the PRK to the desired length
            return hkdfExpand(prk, info.getBytes(StandardCharsets.UTF_8), length);
        } catch (Exception e) {
            throw new KeyExchangeException("HKDF key derivation failed", e);
        }
    }
    
    /**
     * HKDF Extract step: generates a pseudorandom key from input keying material.
     *
     * @param salt the salt value
     * @param ikm the input keying material
     * @return the pseudorandom key
     * @throws NoSuchAlgorithmException if HMAC algorithm is not available
     * @throws InvalidKeyException if the key is invalid
     */
    private byte[] hkdfExtract(byte[] salt, byte[] ikm) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(salt, HMAC_ALGORITHM));
        return mac.doFinal(ikm);
    }
    
    /**
     * HKDF Expand step: expands a pseudorandom key to the desired length.
     *
     * @param prk the pseudorandom key from extract step
     * @param info the application-specific info string
     * @param length the desired output length in bytes
     * @return the output keying material
     * @throws NoSuchAlgorithmException if HMAC algorithm is not available
     * @throws InvalidKeyException if the key is invalid
     */
    private byte[] hkdfExpand(byte[] prk, byte[] info, int length) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(prk, HMAC_ALGORITHM));
        
        int hashLength = mac.getMacLength();
        int iterations = (int) Math.ceil((double) length / hashLength);
        
        byte[] okm = new byte[length];
        byte[] t = new byte[0];
        
        for (int i = 1; i <= iterations; i++) {
            mac.reset();
            mac.update(t);
            mac.update(info);
            mac.update((byte) i);
            t = mac.doFinal();
            
            int copyLength = Math.min(hashLength, length - (i - 1) * hashLength);
            System.arraycopy(t, 0, okm, (i - 1) * hashLength, copyLength);
        }
        
        return okm;
    }
    
    /**
     * Container for derived keys.
     */
    public static class DerivedKeys {
        private final byte[] hashingKey;
        private final byte[] encryptionKey;
        
        /**
         * Creates a DerivedKeys instance.
         *
         * @param hashingKey the derived hashing key
         * @param encryptionKey the derived encryption key
         */
        public DerivedKeys(byte[] hashingKey, byte[] encryptionKey) {
            this.hashingKey = hashingKey.clone();
            this.encryptionKey = encryptionKey.clone();
        }
        
        /**
         * Gets the hashing key.
         *
         * @return a copy of the hashing key
         */
        public byte[] getHashingKey() {
            return hashingKey.clone();
        }
        
        /**
         * Gets the encryption key.
         *
         * @return a copy of the encryption key
         */
        public byte[] getEncryptionKey() {
            return encryptionKey.clone();
        }
        
        /**
         * Gets the hashing key as a string (for use with HashTokenTransformer).
         *
         * @return the hashing key as a UTF-8 string
         */
        public String getHashingKeyAsString() {
            return new String(hashingKey, StandardCharsets.ISO_8859_1);
        }
        
        /**
         * Gets the encryption key as a string (for use with EncryptTokenTransformer).
         *
         * @return the encryption key as a UTF-8 string
         */
        public String getEncryptionKeyAsString() {
            return new String(encryptionKey, StandardCharsets.ISO_8859_1);
        }
    }
}
