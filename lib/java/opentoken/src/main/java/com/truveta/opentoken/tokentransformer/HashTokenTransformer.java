/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokentransformer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms the token using a cryptographic hash function with
 * a secret key.
 * 
 * @see <a href=https://datatracker.ietf.org/doc/html/rfc4868>HMACSHA256</a>
 */
public class HashTokenTransformer implements TokenTransformer {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(HashTokenTransformer.class);

    private transient Mac mac;
    private transient Encoder encoder;
    private final String hashingSecret;
    private final byte[] hashingSecretBytes;

    /**
     * Initializes the underlying MAC with the secret key.
     *
     * @param hashingSecret the cryptographic secret key.
     *
     * @throws java.security.NoSuchAlgorithmException invalid HMAC algorithm.
     * @throws java.security.InvalidKeyException      if the given key is
     *                                                inappropriate for
     *                                                initializing this HMAC.
     */
    public HashTokenTransformer(String hashingSecret) throws NoSuchAlgorithmException, InvalidKeyException {
        this(hashingSecret, hashingSecret == null ? null : hashingSecret.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Initializes the underlying MAC with the secret key provided as raw bytes.
     * This avoids charset expansion when secrets are derived from binary key
     * material (for example, ECDH-derived keys).
     */
    public HashTokenTransformer(byte[] hashingSecretBytes) throws NoSuchAlgorithmException, InvalidKeyException {
        this(hashingSecretBytes == null ? null
                : new String(hashingSecretBytes, StandardCharsets.ISO_8859_1), hashingSecretBytes);
    }

    private HashTokenTransformer(String hashingSecret, byte[] hashingSecretBytes)
            throws NoSuchAlgorithmException, InvalidKeyException {
        this.hashingSecret = hashingSecret;
        this.hashingSecretBytes = hashingSecretBytes == null ? null : hashingSecretBytes.clone();
        if (StringUtils.isEmpty(this.hashingSecret) || this.hashingSecretBytes == null
                || this.hashingSecretBytes.length == 0) {
            this.mac = null;
            this.encoder = null;
            return;
        }
        this.mac = Mac.getInstance("HmacSHA256");
        this.mac.init(new SecretKeySpec(this.hashingSecretBytes, "HmacSHA256"));
        this.encoder = Base64.getEncoder();
    }

    /**
     * Hash token transformer.
     * <p>
     * The token is transformed using HMAC SHA256 algorithm.
     *
     * @return hashed token in <code>base64</code> format.
     * 
     * @throws java.lang.IllegalArgumentException <code>null</code> or blank token
     *                                            provided.
     * @throws java.lang.IllegalStateException    if the HMAC is not initialized
     *                                            properly.
     */
    @Override
    public String transform(String token) throws IllegalArgumentException, IllegalStateException {
        if (token == null || token.isBlank()) {
            logger.error("Invalid Argument. Token can't be Null.");
            throw new IllegalArgumentException("Invalid Argument. Token can't be Null.");
        }

        synchronized (this.mac) {
            byte[] dataAsBytes = token.getBytes();
            byte[] sha = this.mac.doFinal(dataAsBytes);
            return this.encoder.encodeToString(sha);
        }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject(); // Serializes hashingSecret
    }

    // Custom deserialization
    private void readObject(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        ois.defaultReadObject(); // Deserializes hashingSecret
        try {
            if (StringUtils.isEmpty(this.hashingSecret)) {
                this.mac = null;
                this.encoder = null;
                return;
            }
            byte[] secretBytes = this.hashingSecretBytes != null ? this.hashingSecretBytes
                    : this.hashingSecret.getBytes(StandardCharsets.ISO_8859_1);
            this.mac = Mac.getInstance("HmacSHA256");
            this.mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            this.encoder = Base64.getEncoder();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IOException("Failed to reconstruct MAC", e);
        }
    }

}
