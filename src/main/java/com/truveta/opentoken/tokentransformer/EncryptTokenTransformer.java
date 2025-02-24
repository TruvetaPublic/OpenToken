/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokentransformer;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms the token using a AES-256 symmetric encryption.
 * 
 * @see <a href=https://datatracker.ietf.org/doc/html/rfc3826>AES</a>
 */
public class EncryptTokenTransformer implements TokenTransformer {

    private static final Logger logger = LoggerFactory.getLogger(EncryptTokenTransformer.class.getName());

    private static final String AES = "AES";
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_BYTE_LENGTH = 32;
    private static final int BLOCK_SIZE = 32;
    private static final int TAG_LENGTH_BITS = 128;

    private Cipher cipher;
    private byte[] ivBytes;

    /**
     * Initializes the underlying cipher (AES) with the encryption secret.
     * 
     * @param encryptionKey the encryption key. The key must be 32 characters long.
     * 
     * @throws java.security.NoSuchAlgorithmException           invalid encryption
     *                                                          algorithm/mode.
     * @throws javax.crypto.NoSuchPaddingException              invalid encryption
     *                                                          algorithm padding.
     * @throws java.security.InvalidKeyException                invalid encryption
     *                                                          key.
     * @throws java.security.InvalidAlgorithmParameterException invalid encryption
     *                                                          algorithm
     *                                                          parameters.
     */
    public EncryptTokenTransformer(String encryptionKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        if (encryptionKey.length() != KEY_BYTE_LENGTH) {
            logger.error("Invalid Argument. Key must be {} characters long", KEY_BYTE_LENGTH);
            throw new InvalidKeyException(String.format("Key must be %s characters long", KEY_BYTE_LENGTH));
        }

        SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), AES);

        // Generate random IV (for AES block size)
        this.ivBytes = new byte[BLOCK_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(this.ivBytes);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BITS, this.ivBytes);

        // Initialize AES cipher in GCM mode with no padding for encryption
        this.cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

        // Initialize the cipher for encryption
        this.cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
    }

    /**
     * Encryption token transformer.
     * <p>
     * Encrypts the token using AES-256 symmetric encryption algorithm.
     *
     * @throws java.lang.IllegalStateException        the underlying cipher
     *                                                is in a wrong state.
     * @throws javax.crypto.IllegalBlockSizeException if this cipher is a block
     *                                                cipher,
     *                                                no padding has been requested
     *                                                (only in encryption mode), and
     *                                                the total
     *                                                input length of the data
     *                                                processed by this cipher is
     *                                                not a multiple of
     *                                                block size; or if this
     *                                                encryption algorithm is unable
     *                                                to
     *                                                process the input data
     *                                                provided.
     * @throws javax.crypto.BadPaddingException       invalid padding size.
     */
    @Override
    public String transform(String token)
            throws IllegalStateException, IllegalBlockSizeException, BadPaddingException {
        byte[] encryptedBytes = this.cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));

        byte[] messageBytes = new byte[BLOCK_SIZE + encryptedBytes.length];

        System.arraycopy(this.ivBytes, 0, messageBytes, 0, BLOCK_SIZE);
        System.arraycopy(encryptedBytes, 0, messageBytes, BLOCK_SIZE, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(messageBytes);
    }
}
