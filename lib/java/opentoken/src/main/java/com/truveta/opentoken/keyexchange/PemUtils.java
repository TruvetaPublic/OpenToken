/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.keyexchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for encoding and decoding cryptographic keys in PEM format.
 * <p>
 * Handles conversion between Java Key objects and PEM-encoded strings,
 * following the standard PEM format with BEGIN/END markers.
 */
public final class PemUtils {
    
    private static final String PEM_BEGIN = "-----BEGIN %s-----";
    private static final String PEM_END = "-----END %s-----";
    private static final int LINE_LENGTH = 64;
    
    private PemUtils() {
        // Utility class, prevent instantiation
    }
    
    /**
     * Encodes a key to PEM format.
     *
     * @param key the key to encode
     * @param label the PEM label (e.g., "PRIVATE KEY", "PUBLIC KEY")
     * @return the PEM-encoded string
     */
    public static String encodeToPem(Key key, String label) {
        byte[] encoded = key.getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);
        
        StringBuilder pem = new StringBuilder();
        pem.append(String.format(PEM_BEGIN, label)).append("\n");
        
        // Split base64 into lines of 64 characters
        for (int i = 0; i < base64.length(); i += LINE_LENGTH) {
            int end = Math.min(i + LINE_LENGTH, base64.length());
            pem.append(base64, i, end).append("\n");
        }
        
        pem.append(String.format(PEM_END, label)).append("\n");
        
        return pem.toString();
    }
    
    /**
     * Decodes a PEM-encoded key string.
     *
     * @param pemContent the PEM content
     * @return the decoded key bytes
     */
    public static byte[] decodePem(String pemContent) {
        // Remove PEM headers and footers
        String base64 = pemContent
            .replaceAll("-----BEGIN [^-]+-----", "")
            .replaceAll("-----END [^-]+-----", "")
            .replaceAll("\\s", "");
        
        return Base64.getDecoder().decode(base64);
    }
    
    /**
     * Loads a private key from a PEM file.
     *
     * @param filePath the path to the PEM file
     * @return the private key
     * @throws IOException if reading the file fails
     * @throws NoSuchAlgorithmException if the EC algorithm is not available
     * @throws InvalidKeySpecException if the key spec is invalid
     */
    public static PrivateKey loadPrivateKey(String filePath) 
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pemContent = new String(Files.readAllBytes(Paths.get(filePath)));
        byte[] keyBytes = decodePem(pemContent);
        
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KeyPairManager.EC_ALGORITHM);
        
        return keyFactory.generatePrivate(keySpec);
    }
    
    /**
     * Loads a public key from a PEM file.
     *
     * @param filePath the path to the PEM file
     * @return the public key
     * @throws IOException if reading the file fails
     * @throws NoSuchAlgorithmException if the EC algorithm is not available
     * @throws InvalidKeySpecException if the key spec is invalid
     */
    public static PublicKey loadPublicKey(String filePath) 
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pemContent = new String(Files.readAllBytes(Paths.get(filePath)));
        byte[] keyBytes = decodePem(pemContent);
        
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KeyPairManager.EC_ALGORITHM);
        
        return keyFactory.generatePublic(keySpec);
    }
    
    /**
     * Loads a public key from a PEM string.
     *
     * @param pemContent the PEM content as a string
     * @return the public key
     * @throws NoSuchAlgorithmException if the EC algorithm is not available
     * @throws InvalidKeySpecException if the key spec is invalid
     */
    public static PublicKey loadPublicKeyFromString(String pemContent) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = decodePem(pemContent);
        
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KeyPairManager.EC_ALGORITHM);
        
        return keyFactory.generatePublic(keySpec);
    }
    

}
