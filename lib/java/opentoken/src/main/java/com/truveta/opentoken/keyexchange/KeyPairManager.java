/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages ECDH key pair lifecycle for OpenToken.
 * <p>
 * Handles generation, loading, and saving of elliptic curve key pairs
 * used for Diffie-Hellman key exchange. Uses the P-256 (secp256r1) curve
 * for broad compatibility and security.
 */
public class KeyPairManager {
    private static final Logger logger = LoggerFactory.getLogger(KeyPairManager.class);
    
    /**
     * Default directory for storing key pairs.
     */
    public static final String DEFAULT_KEY_DIR = System.getProperty("user.home") + "/.opentoken";
    
    /**
     * Default filename for private key.
     */
    public static final String DEFAULT_PRIVATE_KEY_FILENAME = "keypair.pem";
    
    /**
     * Default filename for public key.
     */
    public static final String DEFAULT_PUBLIC_KEY_FILENAME = "public_key.pem";
    
    /**
     * Elliptic curve algorithm name.
     */
    public static final String EC_ALGORITHM = "EC";
    
    /**
     * Standard curve for ECDH (P-256 / secp256r1).
     */
    public static final String EC_CURVE = "secp256r1";
    
    private final String keyDirectory;
    
    /**
     * Creates a KeyPairManager with the default key directory.
     */
    public KeyPairManager() {
        this(DEFAULT_KEY_DIR);
    }
    
    /**
     * Creates a KeyPairManager with a custom key directory.
     *
     * @param keyDirectory the directory to store keys
     */
    public KeyPairManager(String keyDirectory) {
        this.keyDirectory = keyDirectory;
    }
    
    /**
     * Loads or generates a key pair.
     * <p>
     * If a key pair exists at the default location, it is loaded.
     * Otherwise, a new key pair is generated and saved.
     *
     * @return the key pair (loaded or newly generated)
     * @throws KeyExchangeException if key loading or generation fails
     */
    public KeyPair getOrCreateKeyPair() throws KeyExchangeException {
        Path privateKeyPath = Paths.get(keyDirectory, DEFAULT_PRIVATE_KEY_FILENAME);
        
        if (Files.exists(privateKeyPath)) {
            logger.info("Loading existing key pair from {}", privateKeyPath);
            return loadKeyPair(privateKeyPath.toString());
        } else {
            logger.info("No existing key pair found. Generating new key pair.");
            return generateAndSaveKeyPair();
        }
    }
    
    /**
     * Generates a new ECDH key pair using the P-256 curve.
     *
     * @return the newly generated key pair
     * @throws KeyExchangeException if key generation fails
     */
    public KeyPair generateKeyPair() throws KeyExchangeException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM);
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(EC_CURVE);
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            logger.debug("Generated new ECDH key pair using curve {}", EC_CURVE);
            
            return keyPair;
        } catch (NoSuchAlgorithmException | java.security.InvalidAlgorithmParameterException e) {
            throw new KeyExchangeException("Failed to generate ECDH key pair", e);
        }
    }
    
    /**
     * Generates a new key pair and saves it to the default location.
     *
     * @return the newly generated key pair
     * @throws KeyExchangeException if generation or saving fails
     */
    public KeyPair generateAndSaveKeyPair() throws KeyExchangeException {
        KeyPair keyPair = generateKeyPair();
        saveKeyPair(keyPair);
        return keyPair;
    }
    
    /**
     * Saves a key pair to the configured directory.
     * <p>
     * The private key is saved with restrictive permissions (0600 on Unix-like systems).
     *
     * @param keyPair the key pair to save
     * @throws KeyExchangeException if saving fails
     */
    public void saveKeyPair(KeyPair keyPair) throws KeyExchangeException {
        try {
            // Create directory if it doesn't exist
            Path keyDirPath = Paths.get(keyDirectory);
            if (!Files.exists(keyDirPath)) {
                Files.createDirectories(keyDirPath);
                logger.debug("Created key directory: {}", keyDirectory);
            }
            
            // Save private key
            Path privateKeyPath = Paths.get(keyDirectory, DEFAULT_PRIVATE_KEY_FILENAME);
            savePrivateKey(keyPair.getPrivate(), privateKeyPath.toString());
            
            // Save public key
            Path publicKeyPath = Paths.get(keyDirectory, DEFAULT_PUBLIC_KEY_FILENAME);
            savePublicKey(keyPair.getPublic(), publicKeyPath.toString());
            
            logger.info("Saved key pair to {}", keyDirectory);
        } catch (IOException e) {
            throw new KeyExchangeException("Failed to save key pair", e);
        }
    }
    
    /**
     * Saves a private key to a file in PEM format with restrictive permissions.
     *
     * @param privateKey the private key to save
     * @param filePath the path to save the key
     * @throws KeyExchangeException if saving fails
     */
    public void savePrivateKey(PrivateKey privateKey, String filePath) throws KeyExchangeException {
        try {
            String pemContent = PemUtils.encodeToPem(privateKey, "PRIVATE KEY");
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(pemContent.getBytes());
            }
            
            // Set restrictive permissions (Unix-like systems only)
            setRestrictivePermissions(filePath);
            
            logger.debug("Saved private key to {}", filePath);
        } catch (IOException e) {
            throw new KeyExchangeException("Failed to save private key", e);
        }
    }
    
    /**
     * Saves a public key to a file in PEM format.
     *
     * @param publicKey the public key to save
     * @param filePath the path to save the key
     * @throws KeyExchangeException if saving fails
     */
    public void savePublicKey(PublicKey publicKey, String filePath) throws KeyExchangeException {
        try {
            String pemContent = PemUtils.encodeToPem(publicKey, "PUBLIC KEY");
            
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(pemContent.getBytes());
            }
            
            logger.debug("Saved public key to {}", filePath);
        } catch (IOException e) {
            throw new KeyExchangeException("Failed to save public key", e);
        }
    }
    
    /**
     * Loads a key pair from the specified private key file path.
     * <p>
     * This method loads the private key from the specified path and attempts to load
     * the corresponding public key from the same directory (using the standard public key filename).
     *
     * @param privateKeyPath the path to the private key file
     * @return the loaded key pair
     * @throws KeyExchangeException if loading fails
     */
    public KeyPair loadKeyPair(String privateKeyPath) throws KeyExchangeException {
        try {
            PrivateKey privateKey = PemUtils.loadPrivateKey(privateKeyPath);
            
            // Determine public key path (same directory, standard filename)
            Path privatePath = Paths.get(privateKeyPath);
            Path publicKeyPath = privatePath.getParent().resolve(DEFAULT_PUBLIC_KEY_FILENAME);
            
            PublicKey publicKey;
            if (Files.exists(publicKeyPath)) {
                publicKey = PemUtils.loadPublicKey(publicKeyPath.toString());
            } else {
                throw new KeyExchangeException(
                    "Public key file not found at expected location: " + publicKeyPath
                );
            }
            
            return new KeyPair(publicKey, privateKey);
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new KeyExchangeException("Failed to load key pair from " + privateKeyPath, e);
        }
    }
    
    /**
     * Sets restrictive file permissions (0600) on Unix-like systems.
     * <p>
     * On Windows, this method does nothing as the permission model is different.
     *
     * @param filePath the path to the file
     */
    private void setRestrictivePermissions(String filePath) {
        try {
            Path path = Paths.get(filePath);
            
            // Check if we're on a POSIX-compliant system
            if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> permissions = new HashSet<>();
                permissions.add(PosixFilePermission.OWNER_READ);
                permissions.add(PosixFilePermission.OWNER_WRITE);
                
                Files.setPosixFilePermissions(path, permissions);
                logger.debug("Set restrictive permissions (0600) on {}", filePath);
            } else {
                logger.debug("POSIX permissions not supported on this system, skipping permission setting");
            }
        } catch (IOException e) {
            logger.warn("Failed to set restrictive permissions on {}: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * Returns the key directory path.
     *
     * @return the key directory
     */
    public String getKeyDirectory() {
        return keyDirectory;
    }
}
