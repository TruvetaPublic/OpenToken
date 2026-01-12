/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.keyexchange.KeyExchangeException;
import com.truveta.opentoken.keyexchange.KeyPairManager;
import com.truveta.opentoken.keyexchange.PublicKeyLoader;

/**
 * Utility class for packaging OpenToken output as ZIP files.
 * <p>
 * Handles creating ZIP packages containing tokens, metadata, and sender's public key
 * for ECDH-based key exchange workflows.
 */
public class OutputPackager {
    
    private static final Logger logger = LoggerFactory.getLogger(OutputPackager.class);
    
    private static final String SENDER_PUBLIC_KEY_FILENAME = "sender_public_key.pem";
    private static final int BUFFER_SIZE = 4096;
    
    /**
     * Packages output files into a ZIP archive.
     *
     * @param tokensFile the tokens file (CSV or Parquet)
     * @param metadataFile the metadata JSON file
     * @param senderPublicKey the sender's public key
     * @param outputZipPath the output ZIP file path
     * @throws IOException if an I/O error occurs
     * @throws KeyExchangeException if key operations fail
     */
    public static void packageOutput(String tokensFile, String metadataFile, 
                                     PublicKey senderPublicKey, String outputZipPath) 
            throws IOException, KeyExchangeException {
        logger.info("Packaging output to ZIP: {}", outputZipPath);
        
        // Create temp file for sender's public key
        Path tempPublicKeyFile = Files.createTempFile("sender_public_key", ".pem");
        try {
            // Save sender's public key to temp file
            KeyPairManager keyPairManager = new KeyPairManager();
            keyPairManager.savePublicKey(senderPublicKey, tempPublicKeyFile.toString());
            
            // Create ZIP
            try (FileOutputStream fos = new FileOutputStream(outputZipPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                // Add tokens file
                addFileToZip(zos, tokensFile, getFileName(tokensFile));
                logger.debug("Added tokens file to ZIP: {}", tokensFile);
                
                // Add metadata file
                addFileToZip(zos, metadataFile, getFileName(metadataFile));
                logger.debug("Added metadata file to ZIP: {}", metadataFile);
                
                // Add sender's public key
                addFileToZip(zos, tempPublicKeyFile.toString(), SENDER_PUBLIC_KEY_FILENAME);
                logger.debug("Added sender public key to ZIP");
            }
            
            logger.info("Successfully created ZIP package: {}", outputZipPath);
        } finally {
            // Clean up temp file
            Files.deleteIfExists(tempPublicKeyFile);
        }
    }
    
    /**
     * Extracts sender's public key from a ZIP package.
     *
     * @param zipFilePath the path to the ZIP file
     * @return the extracted sender's public key
     * @throws IOException if an I/O error occurs
     * @throws KeyExchangeException if key loading fails
     */
    public static PublicKey extractSenderPublicKey(String zipFilePath) 
            throws IOException, KeyExchangeException {
        logger.info("Extracting sender's public key from ZIP: {}", zipFilePath);
        
        // Create temp directory for extraction
        Path tempDir = Files.createTempDirectory("opentoken_extract");
        try {
            // Extract sender's public key
            try (FileInputStream fis = new FileInputStream(zipFilePath);
                 ZipInputStream zis = new ZipInputStream(fis)) {
                
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (SENDER_PUBLIC_KEY_FILENAME.equals(entry.getName())) {
                        // Extract public key file
                        Path publicKeyPath = tempDir.resolve(SENDER_PUBLIC_KEY_FILENAME);
                        try (FileOutputStream fos = new FileOutputStream(publicKeyPath.toFile())) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        
                        // Load and return the public key
                        PublicKeyLoader loader = new PublicKeyLoader();
                        PublicKey publicKey = loader.loadPublicKey(publicKeyPath.toString());
                        logger.info("Successfully extracted sender's public key");
                        return publicKey;
                    }
                    zis.closeEntry();
                }
            }
            
            throw new KeyExchangeException("Sender's public key not found in ZIP package");
        } finally {
            // Clean up temp directory
            deleteDirectory(tempDir.toFile());
        }
    }
    
    /**
     * Extracts tokens file from a ZIP package.
     *
     * @param zipFilePath the path to the ZIP file
     * @param outputPath the path where tokens should be extracted
     * @throws IOException if an I/O error occurs
     */
    public static void extractTokensFile(String zipFilePath, String outputPath) 
            throws IOException {
        logger.info("Extracting tokens from ZIP: {}", zipFilePath);
        
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // Extract the tokens file (CSV or Parquet, but not metadata or public key)
                if ((entryName.endsWith(".csv") || entryName.endsWith(".parquet")) 
                    && !entryName.endsWith(".metadata.json")
                    && !SENDER_PUBLIC_KEY_FILENAME.equals(entryName)) {
                    
                    logger.debug("Extracting tokens file: {}", entryName);
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    logger.info("Tokens extracted to: {}", outputPath);
                    return;
                }
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Checks if a file is a ZIP file based on its extension.
     *
     * @param filePath the file path to check
     * @return true if the file has a .zip extension
     */
    public static boolean isZipFile(String filePath) {
        return filePath != null && filePath.toLowerCase().endsWith(".zip");
    }
    
    /**
     * Adds a file to a ZIP output stream.
     *
     * @param zos the ZIP output stream
     * @param filePath the path to the file to add
     * @param entryName the name for the ZIP entry
     * @throws IOException if an I/O error occurs
     */
    private static void addFileToZip(ZipOutputStream zos, String filePath, String entryName) 
            throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }
        
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        
        zos.closeEntry();
    }
    
    /**
     * Gets the file name from a file path.
     *
     * @param filePath the file path
     * @return the file name
     */
    private static String getFileName(String filePath) {
        return Paths.get(filePath).getFileName().toString();
    }
    
    /**
     * Recursively deletes a directory and its contents.
     *
     * @param directory the directory to delete
     */
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
