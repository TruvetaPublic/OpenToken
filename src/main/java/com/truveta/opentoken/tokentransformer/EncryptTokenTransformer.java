/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokentransformer;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
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
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";

    private final Cipher cipher;
    private final Encoder encoder;

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

        if (encryptionKey.length() != 32) {
            logger.error("Invalid Argument. Key must be 32 characters long");
            throw new InvalidKeyException("Key must be 32 characters long");
        }

        SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), AES);

        // Generate random IV (16 bytes for AES block size) - using zero IV for
        // simplicity
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);

        // Initialize AES cipher in CBC mode with PKCS5 padding for encryption
        this.cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);

        // Initialize the cipher for encryption
        this.cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

        this.encoder = Base64.getEncoder();
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
     * @throws javax.crypto.AEADBadTagException       invalid tag.
     */
    @Override
    public String transform(String token)
            throws IllegalStateException, IllegalBlockSizeException, BadPaddingException, AEADBadTagException {
        byte[] encryptedBytes = this.cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
        return this.encoder.encodeToString(encryptedBytes);
    }
}
