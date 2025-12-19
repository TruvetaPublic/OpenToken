/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.truveta.opentoken.Metadata;
import com.truveta.opentoken.cli.io.MetadataWriter;
import com.truveta.opentoken.cli.io.PersonAttributesReader;
import com.truveta.opentoken.cli.io.PersonAttributesWriter;
import com.truveta.opentoken.cli.io.TokenReader;
import com.truveta.opentoken.cli.io.TokenWriter;
import com.truveta.opentoken.cli.io.csv.PersonAttributesCSVReader;
import com.truveta.opentoken.cli.io.csv.PersonAttributesCSVWriter;
import com.truveta.opentoken.cli.io.csv.TokenCSVReader;
import com.truveta.opentoken.cli.io.csv.TokenCSVWriter;
import com.truveta.opentoken.cli.io.json.MetadataJsonWriter;
import com.truveta.opentoken.cli.io.parquet.PersonAttributesParquetReader;
import com.truveta.opentoken.cli.io.parquet.PersonAttributesParquetWriter;
import com.truveta.opentoken.cli.io.parquet.TokenParquetReader;
import com.truveta.opentoken.cli.io.parquet.TokenParquetWriter;
import com.truveta.opentoken.cli.processor.PersonAttributesProcessor;
import com.truveta.opentoken.cli.processor.TokenDecryptionProcessor;
import com.truveta.opentoken.tokentransformer.DecryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;
import com.truveta.opentoken.keyexchange.KeyExchange;
import com.truveta.opentoken.keyexchange.KeyExchangeException;
import com.truveta.opentoken.keyexchange.KeyPairManager;
import com.truveta.opentoken.keyexchange.PublicKeyLoader;
import com.truveta.opentoken.cli.io.OutputPackager;

import java.security.KeyPair;
import java.security.PublicKey;

/**
 * Entry point for the OpenToken command-line application.
 * <p>
 * This class orchestrates two primary workflows:
 * <ul>
 *   <li><b>Encrypt mode</b>: Reads person attributes (CSV/Parquet), validates and
 *   normalizes them, generates tokens via HMAC-SHA256 hashing and AES-256 encryption,
 *   writes tokens to CSV/Parquet, and emits a metadata JSON file.</li>
 *   <li><b>Decrypt mode</b>: Reads encrypted tokens (CSV/Parquet) and writes
 *   decrypted tokens to CSV/Parquet.</li>
 * </ul>
 * Input and output formats support CSV and Parquet.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point. Parses command-line arguments, validates them, and
     * routes execution to encryption or decryption workflows.
     *
     * @param args command-line arguments
     * @throws IOException if an I/O error occurs while creating readers or writers
     */
    public static void main(String[] args) throws IOException {
        CommandLineArguments commandLineArguments = loadCommandLineArguments(args);
        
        // Handle keypair generation mode
        if (commandLineArguments.isGenerateKeypair()) {
            generateKeypair();
            return;
        }
        
        String hashingSecret = commandLineArguments.getHashingSecret();
        String encryptionKey = commandLineArguments.getEncryptionKey();
        String inputPath = commandLineArguments.getInputPath();
        String inputType = commandLineArguments.getInputType();
        String outputPath = commandLineArguments.getOutputPath();
        String outputType = commandLineArguments.getOutputType();
        boolean decryptMode = commandLineArguments.isDecrypt();
        boolean hashOnlyMode = commandLineArguments.isHashOnly();
        boolean decryptWithEcdh = commandLineArguments.isDecryptWithEcdh();
        String receiverPublicKeyPath = commandLineArguments.getReceiverPublicKey();
        String senderPublicKeyPath = commandLineArguments.getSenderPublicKey();
        String senderKeypairPath = commandLineArguments.getSenderKeypairPath();
        String receiverKeypairPath = commandLineArguments.getReceiverKeypairPath();

        if (outputType == null || outputType.isEmpty()) {
            outputType = inputType; // defaulting to input type if not provided
        }

        logger.info("Decrypt Mode: {}", decryptMode);
        logger.info("Decrypt with ECDH: {}", decryptWithEcdh);
        logger.info("Hash-Only Mode: {}", hashOnlyMode);
        logger.info("Receiver Public Key: {}", receiverPublicKeyPath);
        if (logger.isInfoEnabled()) {
            logger.info("Hashing Secret: {}", maskString(hashingSecret));
            logger.info("Encryption Key: {}", maskString(encryptionKey));
        }
        logger.info("Input Path: {}", inputPath);
        logger.info("Input Type: {}", inputType);
        logger.info("Output Path: {}", outputPath);
        logger.info("Output Type: {}", outputType);

        // Validate input and output types for both modes
        if (!(CommandLineArguments.TYPE_CSV.equals(inputType)
                || CommandLineArguments.TYPE_PARQUET.equals(inputType))) {
            logger.error("Only csv and parquet input types are supported!");
            return;
        }
        if (!(CommandLineArguments.TYPE_CSV.equals(outputType)
                || CommandLineArguments.TYPE_PARQUET.equals(outputType))) {
            logger.error("Only csv and parquet output types are supported!");
            return;
        }

        // Process based on mode
        if (decryptMode || decryptWithEcdh) {
            // Decrypt mode
            if (decryptWithEcdh) {
                // ECDH-based decryption
                decryptTokensWithEcdh(inputPath, outputPath, inputType, outputType, 
                                    senderPublicKeyPath, receiverKeypairPath);
            } else {
                // Secret-based decryption (legacy)
                if (encryptionKey == null || encryptionKey.isBlank()) {
                    logger.error("Encryption key must be specified for decryption");
                    return;
                }
                decryptTokens(inputPath, outputPath, inputType, outputType, encryptionKey);
            }
            logger.info("Token decryption completed successfully.");
        } else {
            // Token generation mode
            if (receiverPublicKeyPath != null && !receiverPublicKeyPath.isBlank()) {
                // ECDH-based encryption
                processTokensWithEcdh(inputPath, outputPath, inputType, outputType, 
                                    receiverPublicKeyPath, senderKeypairPath);
            } else {
                // Secret-based encryption (legacy)
                if (hashingSecret == null || hashingSecret.isBlank()) {
                    logger.error("Hashing secret must be specified");
                    return;
                }
                if (!hashOnlyMode && (encryptionKey == null || encryptionKey.isBlank())) {
                    logger.error("Encryption key must be specified (or use --hash-only to skip encryption)");
                    return;
                }
                processTokens(inputPath, outputPath, inputType, outputType, hashingSecret, encryptionKey, hashOnlyMode);
            }
        }
    }

    private static void processTokens(String inputPath, String outputPath, String inputType, String outputType,
            String hashingSecret, String encryptionKey, boolean hashOnlyMode) {
        List<TokenTransformer> tokenTransformerList = new ArrayList<>();
        try {
            // Always add hash transformer
            tokenTransformerList.add(new HashTokenTransformer(hashingSecret));

            // Only add encryption transformer if not in hash-only mode
            if (!hashOnlyMode) {
                tokenTransformerList.add(new EncryptTokenTransformer(encryptionKey));
            }
        } catch (Exception e) {
            logger.error("Error in initializing the transformer. Execution halted. ", e);
            return;
        }

        try (PersonAttributesReader reader = createPersonAttributesReader(inputPath, inputType);
                PersonAttributesWriter writer = createPersonAttributesWriter(outputPath, outputType)) {

            // Create initial metadata with system information
            Metadata metadata = new Metadata();
            Map<String, Object> metadataMap = metadata.initialize();

            // Set hashing secret
            metadata.addHashedSecret(Metadata.HASHING_SECRET_HASH, hashingSecret);

            // Set encryption secret if applicable
            if (!hashOnlyMode) {
                metadata.addHashedSecret(Metadata.ENCRYPTION_SECRET_HASH, encryptionKey);
            }

            // Process data and get updated metadata
            PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

            // Write the metadata to file
            MetadataWriter metadataWriter = new MetadataJsonWriter(outputPath);
            metadataWriter.write(metadataMap);

        } catch (Exception e) {
            logger.error("Error in processing the input file. Execution halted. ", e);
        }
    }

    /**
     * Creates a {@link PersonAttributesReader} for the given input type.
     *
     * @param inputPath path to the person attributes file
     * @param inputType input type ("csv" or "parquet")
     * @return a reader capable of streaming person attributes
     * @throws IOException if the reader cannot be created
     * @throws IllegalArgumentException if the input type is unsupported
     */
    private static PersonAttributesReader createPersonAttributesReader(String inputPath, String inputType)
            throws IOException {
        switch (inputType.toLowerCase()) {
            case CommandLineArguments.TYPE_CSV:
                return new PersonAttributesCSVReader(inputPath);
            case CommandLineArguments.TYPE_PARQUET:
                return new PersonAttributesParquetReader(inputPath);
            default:
                throw new IllegalArgumentException("Unsupported input type: " + inputType);
        }
    }

    /**
     * Creates a {@link PersonAttributesWriter} for the given output type.
     *
     * @param outputPath path to write the transformed tokens file
     * @param outputType output type ("csv" or "parquet")
     * @return a writer capable of streaming tokens
     * @throws IOException if the writer cannot be created
     * @throws IllegalArgumentException if the output type is unsupported
     */
    private static PersonAttributesWriter createPersonAttributesWriter(String outputPath,
            String outputType) throws IOException {
        switch (outputType.toLowerCase()) {
            case CommandLineArguments.TYPE_CSV:
                return new PersonAttributesCSVWriter(outputPath);
            case CommandLineArguments.TYPE_PARQUET:
                return new PersonAttributesParquetWriter(outputPath);
            default:
                throw new IllegalArgumentException("Unsupported output type: " + outputType);
        }
    }

    /**
     * Parses and loads command-line arguments using JCommander.
     *
     * @param args raw command-line arguments
     * @return populated {@link CommandLineArguments}
     */
    private static CommandLineArguments loadCommandLineArguments(String[] args) {
        logger.debug("Processing command line arguments. {}", String.join("|", args));
        CommandLineArguments commandLineArguments = new CommandLineArguments();
        JCommander.newBuilder().addObject(commandLineArguments).build().parse(args);
        logger.info("Command line arguments processed.");
        return commandLineArguments;
    }

    /**
     * Masks a sensitive string by preserving the first three characters and
     * replacing the remainder with asterisks. Returns the input unchanged if the
     * input is {@code null} or has length less than or equal to three.
     *
     * @param input the original sensitive string
     * @return a masked representation suitable for logs
     */
    private static String maskString(String input) {
        if (input == null || input.length() <= 3) {
            return input;
        }
        return input.substring(0, 3) + "*".repeat(input.length() - 3);
    }

    /**
     * Executes the decryption workflow for tokens.
     * <p>
     * Reads encrypted tokens from the specified input, decrypts them using the
     * provided encryption key, and writes the result to the specified output.
     *
     * @param inputPath     path to the input file containing encrypted tokens
     * @param outputPath    path to the output file for decrypted tokens
     * @param inputType     input type ("csv" or "parquet")
     * @param outputType    output type ("csv" or "parquet")
     * @param encryptionKey secret key used for AES-based decryption
     */
    private static void decryptTokens(String inputPath, String outputPath, String inputType, String outputType,
            String encryptionKey) {
        try {
            DecryptTokenTransformer decryptor = new DecryptTokenTransformer(encryptionKey);

            try (TokenReader reader = createTokenReader(inputPath, inputType);
                    TokenWriter writer = createTokenWriter(outputPath, outputType)) {
                TokenDecryptionProcessor.process(reader, writer, decryptor);
            }
        } catch (Exception e) {
            logger.error("Error during token decryption: ", e);
        }
    }

    /**
     * Creates a {@link TokenReader} for the given input type.
     *
     * @param inputPath path to the token file
     * @param inputType input type ("csv" or "parquet")
     * @return a reader capable of streaming tokens
     * @throws IOException if the reader cannot be created
     * @throws IllegalArgumentException if the input type is unsupported
     */
    private static TokenReader createTokenReader(String inputPath, String inputType) throws IOException {
        switch (inputType.toLowerCase()) {
            case CommandLineArguments.TYPE_CSV:
                return new TokenCSVReader(inputPath);
            case CommandLineArguments.TYPE_PARQUET:
                return new TokenParquetReader(inputPath);
            default:
                throw new IllegalArgumentException("Unsupported input type: " + inputType);
        }
    }

    /**
     * Creates a {@link TokenWriter} for the given output type.
     *
     * @param outputPath path to write tokens
     * @param outputType output type ("csv" or "parquet")
     * @return a writer capable of streaming tokens
     * @throws IOException if the writer cannot be created
     * @throws IllegalArgumentException if the output type is unsupported
     */
    private static TokenWriter createTokenWriter(String outputPath, String outputType) throws IOException {
        switch (outputType.toLowerCase()) {
            case CommandLineArguments.TYPE_CSV:
                return new TokenCSVWriter(outputPath);
            case CommandLineArguments.TYPE_PARQUET:
                return new TokenParquetWriter(outputPath);
            default:
                throw new IllegalArgumentException("Unsupported output type: " + outputType);
        }
    }
    
    /**
     * Generates a new ECDH key pair and saves it to the default location.
     */
    private static void generateKeypair() {
        try {
            logger.info("Generating new ECDH P-256 key pair...");
            KeyPairManager keyPairManager = new KeyPairManager();
            KeyPair keyPair = keyPairManager.generateAndSaveKeyPair();
            
            logger.info("✓ Key pair generated successfully");
            logger.info("✓ Private key saved to: {}/keypair.pem (0600 permissions)", keyPairManager.getKeyDirectory());
            logger.info("✓ Public key saved to: {}/public_key.pem", keyPairManager.getKeyDirectory());
            
        } catch (KeyExchangeException e) {
            logger.error("Error generating key pair: ", e);
        }
    }
    
    /**
     * Processes tokens using ECDH-based key exchange.
     *
     * @param inputPath path to person attributes file
     * @param outputPath path to output ZIP file
     * @param inputType input type ("csv" or "parquet")
     * @param outputType output type ("csv" or "parquet")
     * @param receiverPublicKeyPath path to receiver's public key
     * @param senderKeypairPath optional path to sender's keypair
     */
    private static void processTokensWithEcdh(String inputPath, String outputPath, String inputType, 
                                             String outputType, String receiverPublicKeyPath, 
                                             String senderKeypairPath) {
        try {
            logger.info("Processing tokens with ECDH key exchange...");
            
            // Load receiver's public key
            PublicKeyLoader publicKeyLoader = new PublicKeyLoader();
            PublicKey receiverPublicKey = publicKeyLoader.loadPublicKey(receiverPublicKeyPath);
            logger.info("✓ Loaded receiver's public key");
            
            // Load or generate sender's key pair
            KeyPairManager senderKeyManager = senderKeypairPath != null 
                ? new KeyPairManager(new java.io.File(senderKeypairPath).getParent())
                : new KeyPairManager();
            KeyPair senderKeyPair = senderKeyManager.getOrCreateKeyPair();
            logger.info("✓ Sender key pair ready (saved to: {})", senderKeyManager.getKeyDirectory());
            
            // Perform ECDH key exchange
            KeyExchange keyExchange = new KeyExchange();
            KeyExchange.DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
                senderKeyPair.getPrivate(), receiverPublicKey);
            logger.info("✓ Performed ECDH key exchange");
            logger.info("✓ Derived hashing key (32 bytes)");
            logger.info("✓ Derived encryption key (32 bytes)");
            
            // Create transformers with derived keys
            List<TokenTransformer> tokenTransformerList = new ArrayList<>();
            tokenTransformerList.add(new HashTokenTransformer(keys.getHashingKeyAsString()));
            tokenTransformerList.add(new EncryptTokenTransformer(keys.getEncryptionKeyAsString()));
            
            // Create temporary output for tokens
            String tempOutputPath = outputPath.endsWith(".zip") 
                ? outputPath.replace(".zip", "_temp." + outputType) 
                : outputPath + "_temp";
            
            try (PersonAttributesReader reader = createPersonAttributesReader(inputPath, inputType);
                 PersonAttributesWriter writer = createPersonAttributesWriter(tempOutputPath, outputType)) {
                
                // Create metadata with key exchange info
                Metadata metadata = new Metadata();
                Map<String, Object> metadataMap = metadata.initialize();
                
                // Add key exchange metadata
                byte[] senderPublicKeyBytes = senderKeyPair.getPublic().getEncoded();
                byte[] receiverPublicKeyBytes = receiverPublicKey.getEncoded();
                metadata.addKeyExchangeMetadata(senderPublicKeyBytes, receiverPublicKeyBytes);
                
                // Process data
                PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);
                
                // Write metadata
                String metadataPath = tempOutputPath + Metadata.METADATA_FILE_EXTENSION;
                MetadataWriter metadataWriter = new MetadataJsonWriter(tempOutputPath);
                metadataWriter.write(metadataMap);
                
                // Package output as ZIP if needed
                if (OutputPackager.isZipFile(outputPath)) {
                    OutputPackager.packageOutput(tempOutputPath, metadataPath, 
                                                senderKeyPair.getPublic(), outputPath);
                    logger.info("✓ Output package created: {}", outputPath);
                    logger.info("  ├─ tokens.{} (encrypted)", outputType);
                    logger.info("  ├─ tokens.metadata.json");
                    logger.info("  └─ sender_public_key.pem");
                    
                    // Clean up temp files
                    new java.io.File(tempOutputPath).delete();
                    new java.io.File(metadataPath).delete();
                } else {
                    logger.info("✓ Tokens generated successfully");
                    logger.info("Note: Use .zip extension for automatic packaging with sender's public key");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing tokens with ECDH: ", e);
        }
    }
    
    /**
     * Decrypts tokens using ECDH-based key exchange.
     *
     * @param inputPath path to input file (or ZIP)
     * @param outputPath path to decrypted output file
     * @param inputType input type ("csv" or "parquet")
     * @param outputType output type ("csv" or "parquet")
     * @param senderPublicKeyPath optional path to sender's public key (extracted from ZIP if not provided)
     * @param receiverKeypairPath optional path to receiver's keypair
     */
    private static void decryptTokensWithEcdh(String inputPath, String outputPath, String inputType,
                                             String outputType, String senderPublicKeyPath, 
                                             String receiverKeypairPath) {
        try {
            logger.info("Decrypting tokens with ECDH key exchange...");
            
            PublicKey senderPublicKey;
            String tokensInputPath = inputPath;
            
            // Extract sender's public key from ZIP if needed
            if (OutputPackager.isZipFile(inputPath)) {
                senderPublicKey = OutputPackager.extractSenderPublicKey(inputPath);
                logger.info("✓ Extracted sender's public key from ZIP");
                
                // Extract tokens to temp file
                tokensInputPath = inputPath.replace(".zip", "_temp." + inputType);
                OutputPackager.extractTokensFile(inputPath, tokensInputPath);
            } else if (senderPublicKeyPath != null && !senderPublicKeyPath.isBlank()) {
                PublicKeyLoader publicKeyLoader = new PublicKeyLoader();
                senderPublicKey = publicKeyLoader.loadPublicKey(senderPublicKeyPath);
                logger.info("✓ Loaded sender's public key from: {}", senderPublicKeyPath);
            } else {
                logger.error("Sender's public key must be provided or available in ZIP");
                return;
            }
            
            // Load receiver's private key
            KeyPairManager receiverKeyManager = receiverKeypairPath != null
                ? new KeyPairManager(new java.io.File(receiverKeypairPath).getParent())
                : new KeyPairManager();
            KeyPair receiverKeyPair = receiverKeyManager.loadKeyPair(
                receiverKeypairPath != null ? receiverKeypairPath 
                : receiverKeyManager.getKeyDirectory() + "/keypair.pem");
            logger.info("✓ Loaded receiver's private key from: {}", receiverKeyManager.getKeyDirectory());
            
            // Perform ECDH key exchange (same as sender)
            KeyExchange keyExchange = new KeyExchange();
            KeyExchange.DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
                receiverKeyPair.getPrivate(), senderPublicKey);
            logger.info("✓ Performed ECDH key exchange");
            logger.info("✓ Derived encryption key (matches sender's key)");
            
            // Decrypt tokens
            DecryptTokenTransformer decryptor = new DecryptTokenTransformer(keys.getEncryptionKeyAsString());
            
            try (TokenReader reader = createTokenReader(tokensInputPath, inputType);
                 TokenWriter writer = createTokenWriter(outputPath, outputType)) {
                TokenDecryptionProcessor.process(reader, writer, decryptor);
            }
            
            // Clean up temp file if we extracted from ZIP
            if (OutputPackager.isZipFile(inputPath)) {
                new java.io.File(tokensInputPath).delete();
            }
            
            logger.info("✓ Tokens decrypted successfully");
            logger.info("✓ Output written to: {}", outputPath);
            
        } catch (Exception e) {
            logger.error("Error decrypting tokens with ECDH: ", e);
        }
    }
}
