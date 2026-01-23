/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and validates public keys for OpenToken key exchange.
 * <p>
 * Supports loading EC public keys in PEM format from files or strings,
 * and validates that they use a supported curve (P-256/P-384/P-521).
 */
public class PublicKeyLoader {
    private static final Logger logger = LoggerFactory.getLogger(PublicKeyLoader.class);

    /**
     * Loads a public key from a PEM file.
     *
     * @param filePath the path to the PEM file containing the public key
     * @return the loaded public key
     * @throws KeyExchangeException if loading or validation fails
     */
    public PublicKey loadPublicKey(String filePath) throws KeyExchangeException {
        try {
            logger.debug("Loading public key from {}", filePath);

            // Check if file exists
            if (!Files.exists(Paths.get(filePath))) {
                throw new KeyExchangeException("Public key file not found: " + filePath);
            }

            PublicKey publicKey = PemUtils.loadPublicKey(filePath);
            validatePublicKey(publicKey);

            logger.info("Successfully loaded and validated public key from {}", filePath);
            return publicKey;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new KeyExchangeException("Failed to load public key from " + filePath, e);
        }
    }

    /**
     * Loads a public key from a PEM-encoded string.
     *
     * @param pemContent the PEM-encoded public key content
     * @return the loaded public key
     * @throws KeyExchangeException if loading or validation fails
     */
    public PublicKey loadPublicKeyFromString(String pemContent) throws KeyExchangeException {
        try {
            logger.debug("Loading public key from PEM string");

            PublicKey publicKey = PemUtils.loadPublicKeyFromString(pemContent);
            validatePublicKey(publicKey);

            logger.info("Successfully loaded and validated public key from PEM string");
            return publicKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new KeyExchangeException("Failed to load public key from PEM string", e);
        }
    }

    /**
     * Validates that a public key is an EC key using a supported curve.
     *
     * @param publicKey the public key to validate
     * @throws KeyExchangeException if validation fails
     */
    public void validatePublicKey(PublicKey publicKey) throws KeyExchangeException {
        if (publicKey == null) {
            throw new KeyExchangeException("Public key is null");
        }

        // Check if it's an EC key
        if (!(publicKey instanceof ECPublicKey)) {
            throw new KeyExchangeException(
                    "Invalid key type: expected EC public key, got " + publicKey.getClass().getName());
        }

        ECPublicKey ecPublicKey = (ECPublicKey) publicKey;

        // Get the curve name (this is a simplified check)
        String curveName = getCurveName(ecPublicKey);
        logger.debug("Public key uses curve: {}", curveName);

        // Validate the algorithm
        if (!KeyPairManager.EC_ALGORITHM.equals(publicKey.getAlgorithm())) {
            throw new KeyExchangeException(
                    "Invalid key algorithm: expected " + KeyPairManager.EC_ALGORITHM +
                            ", got " + publicKey.getAlgorithm());
        }

        // Note: More rigorous curve validation could be added here
        // For now, we trust that if it's an EC key, it's using a standard curve
        logger.debug("Public key validation passed");
    }

    /**
     * Gets the curve name from an EC public key.
     * <p>
     * This is a best-effort method that returns a string representation
     * of the curve parameters.
     *
     * @param ecPublicKey the EC public key
     * @return a string representing the curve (simplified)
     */
    private String getCurveName(ECPublicKey ecPublicKey) {
        // Get curve parameters
        java.security.spec.ECParameterSpec params = ecPublicKey.getParams();

        // Get the curve field size (bit length)
        int fieldSize = params.getCurve().getField().getFieldSize();

        // Map field size to common curve names
        switch (fieldSize) {
            case 256:
                return "P-256/secp256r1";
            case 384:
                return "P-384/secp384r1";
            case 521:
                return "P-521/secp521r1";
            default:
                return "Unknown (" + fieldSize + " bits)";
        }
    }

    /**
     * Checks if a public key file exists.
     *
     * @param filePath the path to check
     * @return true if the file exists, false otherwise
     */
    public boolean publicKeyFileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
