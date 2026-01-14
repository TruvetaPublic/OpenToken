/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.truveta.opentoken.Metadata;
import com.truveta.opentoken.cli.commands.DecryptCommand;
import com.truveta.opentoken.cli.commands.GenerateKeypairCommand;
import com.truveta.opentoken.cli.commands.TokenizeCommand;
import com.truveta.opentoken.cli.io.MetadataWriter;
import com.truveta.opentoken.cli.io.OutputPackager;
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
import com.truveta.opentoken.keyexchange.KeyExchange;
import com.truveta.opentoken.keyexchange.KeyPairManager;
import com.truveta.opentoken.keyexchange.PublicKeyLoader;
import com.truveta.opentoken.tokentransformer.DecryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;

/**
 * Entry point for the OpenToken command-line application.
 * <p>
 * This class orchestrates ECDH-based token generation and decryption workflows:
 * <ul>
 *   <li><b>Encrypt mode</b>: Uses ECDH key exchange with receiver's public key to derive
 *   encryption/hashing keys, generates tokens via HMAC-SHA256 hashing and AES-256 encryption.</li>
 *   <li><b>Decrypt mode</b>: Uses ECDH key exchange with sender's public key to derive
 *   matching keys and decrypt tokens.</li>
 *   <li><b>Key generation</b>: Generates new ECDH key pairs (default P-384 curve).</li>
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
        // Create subcommand objects
        GenerateKeypairCommand generateKeypairCommand = new GenerateKeypairCommand();
        TokenizeCommand tokenizeCommand = new TokenizeCommand();
        DecryptCommand decryptCommand = new DecryptCommand();

        // Build JCommander with subcommands
        JCommander jc = JCommander.newBuilder()
                .programName("opentoken")
                .addCommand("generate-keypair", generateKeypairCommand)
                .addCommand("tokenize", tokenizeCommand)
                .addCommand("decrypt", decryptCommand)
                .build();

        // Parse arguments
        try {
            jc.parse(args);
        } catch (Exception e) {
            logger.error("Error parsing arguments: {}", e.getMessage());
            jc.usage();
            return;
        }

        // Get the parsed command
        String parsedCommand = jc.getParsedCommand();

        if (parsedCommand == null) {
            logger.error("No command specified. Use one of: generate-keypair, tokenize, decrypt");
            jc.usage();
            return;
        }

        // Route to appropriate handler
        switch (parsedCommand) {
            case "generate-keypair":
                handleGenerateKeypair(generateKeypairCommand);
                break;
            case "tokenize":
                handleTokenize(tokenizeCommand);
                break;
            case "decrypt":
                handleDecrypt(decryptCommand);
                break;
            default:
                logger.error("Unknown command: {}", parsedCommand);
                jc.usage();
        }
    }

    /**
     * Handles the generate-keypair subcommand.
     *
     * @param command the parsed command object
     */
    private static void handleGenerateKeypair(GenerateKeypairCommand command) {
        String keyDir = command.getOutputDir() != null
                ? command.getOutputDir()
                : KeyPairManager.DEFAULT_KEY_DIR;
        generateKeypair(command.getEcdhCurve(), keyDir);
    }

    /**
     * Handles the tokenize subcommand.
     *
     * @param command the parsed command object
     * @throws IOException if an I/O error occurs
     */
    private static void handleTokenize(TokenizeCommand command) throws IOException {
        String inputPath = command.getInputPath();
        String inputType = command.getInputType();
        String outputPath = command.getOutputPath();
        String outputType = command.getOutputType();
        String receiverPublicKeyPath = command.getReceiverPublicKey();
        String senderKeypairPath = command.getSenderKeypairPath();
        boolean hashOnly = command.isHashOnly();
        String ecdhCurveInput = command.getEcdhCurve();

        String ecdhCurveNormalized = KeyPairManager.normalizeCurveName(ecdhCurveInput);
        String ecdhCurveDisplay = displayCurveName(ecdhCurveNormalized, ecdhCurveInput);
        if (outputType == null || outputType.isEmpty()) {
            outputType = inputType;
        }

        String mode = hashOnly ? "Hash-only (ECDH-derived hash key, no encryption)" : "Encrypt with ECDH";
        logger.info("Mode: {}", mode);
        logger.info("ECDH Curve: {} (normalized: {})", ecdhCurveDisplay, ecdhCurveNormalized);
        logger.info("Receiver Public Key: {}", receiverPublicKeyPath);
        logger.info("Input Path: {}", inputPath);
        logger.info("Input Type: {}", inputType);
        logger.info("Output Path: {}", outputPath);
        logger.info("Output Type: {}", outputType);

        // Validate input and output types
        if (!("csv".equals(inputType) || "parquet".equals(inputType))) {
            logger.error("Only csv and parquet input types are supported!");
            return;
        }
        if (!("csv".equals(outputType) || "parquet".equals(outputType))) {
            logger.error("Only csv and parquet output types are supported!");
            return;
        }

        processTokensWithEcdh(inputPath, outputPath, inputType, outputType,
                receiverPublicKeyPath, senderKeypairPath, ecdhCurveNormalized, ecdhCurveDisplay, hashOnly);
    }

    /**
     * Handles the decrypt subcommand.
     *
     * @param command the parsed command object
     * @throws IOException if an I/O error occurs
     */
    private static void handleDecrypt(DecryptCommand command) throws IOException {
        String inputPath = command.getInputPath();
        String inputType = command.getInputType();
        String outputPath = command.getOutputPath();
        String outputType = command.getOutputType();
        String senderPublicKeyPath = command.getSenderPublicKey();
        String receiverKeypairPath = command.getReceiverKeypairPath();
        String ecdhCurveInput = command.getEcdhCurve();

        String ecdhCurveNormalized = KeyPairManager.normalizeCurveName(ecdhCurveInput);
        if (outputType == null || outputType.isEmpty()) {
            outputType = inputType;
        }

        logger.info("Mode: Decrypt with ECDH");
        logger.info("Sender Public Key: {}", senderPublicKeyPath);
        logger.info("Input Path: {}", inputPath);
        logger.info("Input Type: {}", inputType);
        logger.info("Output Path: {}", outputPath);
        logger.info("Output Type: {}", outputType);

        // Validate input and output types
        if (!("csv".equals(inputType) || "parquet".equals(inputType))) {
            logger.error("Only csv and parquet input types are supported!");
            return;
        }
        if (!("csv".equals(outputType) || "parquet".equals(outputType))) {
            logger.error("Only csv and parquet output types are supported!");
            return;
        }

        decryptTokensWithEcdh(inputPath, outputPath, inputType, outputType,
                senderPublicKeyPath, receiverKeypairPath, ecdhCurveNormalized);
        logger.info("Token decryption completed successfully.");
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
            case "csv":
                return new PersonAttributesCSVReader(inputPath);
            case "parquet":
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
    private static PersonAttributesWriter createPersonAttributesWriter(String outputPath, String outputType)
            throws IOException {
        switch (outputType.toLowerCase()) {
            case "csv":
                return new PersonAttributesCSVWriter(outputPath);
            case "parquet":
                return new PersonAttributesParquetWriter(outputPath);
            default:
                throw new IllegalArgumentException("Unsupported output type: " + outputType);
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
            case "csv":
                return new TokenCSVReader(inputPath);
            case "parquet":
                return new TokenParquetReader(inputPath);
            default:
                throw new IllegalArgumentException("Unsupported input type: " + inputType);
        }
    }

    /**
     * Creates a {@link TokenWriter} for the given output type.
     *
     * @param outputPath path to write the decrypted tokens file
     * @param outputType output type ("csv" or "parquet")
     * @return a writer capable of streaming tokens
     * @throws IOException if the writer cannot be created
     * @throws IllegalArgumentException if the output type is unsupported
     */
    private static TokenWriter createTokenWriter(String outputPath, String outputType) throws IOException {
        switch (outputType.toLowerCase()) {
            case "csv":
                return new TokenCSVWriter(outputPath);
            case "parquet":
                return new TokenParquetWriter(outputPath);
            default:
                throw new IllegalArgumentException("Unsupported output type: " + outputType);
        }
    }

    /**
     * Returns a display name for the ECDH curve name.
     *
     * @param normalized the normalized curve name (lowercase)
     * @param original the original user-provided curve name
     * @return a display-friendly curve name
     */
    private static String displayCurveName(String normalized, String original) {
        if (normalized == null || normalized.isEmpty()) {
            return original;
        }
        // Map normalized names to display names
        switch (normalized) {
            case "secp256r1":
                return "P-256";
            case "secp384r1":
                return "P-384";
            case "secp521r1":
                return "P-521";
            default:
                return original;
        }
    }

    /**
     * Generates a new ECDH key pair and saves it to the specified location.
     *
     * @param ecdhCurveInput the user-provided curve name (e.g., "P-384")
     * @param keyDir the directory to save the key pair
     */
    private static void generateKeypair(String ecdhCurveInput, String keyDir) {
        try {
            String ecdhCurveNormalized = KeyPairManager.normalizeCurveName(ecdhCurveInput);
            String ecdhCurveDisplay = displayCurveName(ecdhCurveNormalized, ecdhCurveInput);
            logger.info("Generating new ECDH key pair (curve: {})...", ecdhCurveDisplay);
            KeyPairManager keyPairManager = new KeyPairManager(keyDir, ecdhCurveNormalized);
            keyPairManager.generateAndSaveKeyPair();

            logger.info("\u2713 Key pair generated successfully");
            logger.info("\u2713 Private key saved to: {}/keypair.pem (0600 permissions)",
                    keyPairManager.getKeyDirectory());
            logger.info("\u2713 Public key saved to: {}/public_key.pem", keyPairManager.getKeyDirectory());
        } catch (Exception e) {
            logger.error("Error generating key pair: {}", e.getMessage(), e);
        }
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
     * Processes tokens using ECDH-based key exchange.
     *
     * @param inputPath path to person attributes file
     * @param outputPath path to output ZIP file
     * @param inputType input type ("csv" or "parquet")
     * @param outputType output type ("csv" or "parquet")
     * @param receiverPublicKeyPath path to receiver's public key
     * @param senderKeypairPath optional path to sender's keypair
     * @param ecdhCurveNormalized normalized ECDH curve name
     * @param ecdhCurveDisplay display name for the ECDH curve
     * @param hashOnly true to skip encryption and emit hashed tokens only
     */
    private static void processTokensWithEcdh(String inputPath, String outputPath, String inputType,
            String outputType, String receiverPublicKeyPath,
            String senderKeypairPath, String ecdhCurveNormalized, String ecdhCurveDisplay, boolean hashOnly) {
        try {
            logger.info("Processing tokens with ECDH key exchange...");

            // Load receiver's public key
            PublicKeyLoader publicKeyLoader = new PublicKeyLoader();
            PublicKey receiverPublicKey = publicKeyLoader.loadPublicKey(receiverPublicKeyPath);
            logger.info("\u2713 Loaded receiver's public key");

            // Load or generate sender's key pair
            KeyPairManager senderKeyManager = senderKeypairPath != null
                    ? new KeyPairManager(new java.io.File(senderKeypairPath).getParent(), ecdhCurveNormalized)
                    : new KeyPairManager(KeyPairManager.DEFAULT_KEY_DIR, ecdhCurveNormalized);
            KeyPair senderKeyPair = senderKeyManager.getOrCreateKeyPair();
            logger.info("\u2713 Sender key pair ready (saved to: {})", senderKeyManager.getKeyDirectory());

            // Perform ECDH key exchange
            KeyExchange keyExchange = new KeyExchange();
            KeyExchange.DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
                    senderKeyPair.getPrivate(), receiverPublicKey);
            logger.info("\u2713 Performed ECDH key exchange");
            logger.info("\u2713 Derived hashing key (32 bytes)");
            if (!hashOnly) {
                logger.info("\u2713 Derived encryption key (32 bytes)");
            }

            // Create transformers with derived keys
            List<TokenTransformer> tokenTransformerList = new ArrayList<>();
            tokenTransformerList.add(new HashTokenTransformer(keys.getHashingKey()));
            if (!hashOnly) {
                tokenTransformerList.add(new EncryptTokenTransformer(keys.getEncryptionKey()));
            }

            // Create temporary output for tokens
            String tempOutputPath = outputPath.endsWith(".zip")
                    ? outputPath.replace(".zip", "_temp." + outputType)
                    : outputPath + "_temp";
            String metadataPath = null;

            try (PersonAttributesReader reader = createPersonAttributesReader(inputPath, inputType);
                    PersonAttributesWriter writer = createPersonAttributesWriter(tempOutputPath, outputType)) {
                // Create metadata with key exchange info
                Metadata metadata = new Metadata();
                Map<String, Object> metadataMap = metadata.initialize();

                // Add key exchange metadata
                byte[] senderPublicKeyBytes = senderKeyPair.getPublic().getEncoded();
                byte[] receiverPublicKeyBytes = receiverPublicKey.getEncoded();
                metadata.addKeyExchangeMetadata(senderPublicKeyBytes, receiverPublicKeyBytes, ecdhCurveDisplay);

                // Process data
                PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

                // Write metadata
                int dotIndex = tempOutputPath.lastIndexOf('.');
                String metadataBasePath = dotIndex > 0 ? tempOutputPath.substring(0, dotIndex) : tempOutputPath;
                metadataPath = metadataBasePath + Metadata.METADATA_FILE_EXTENSION;
                MetadataWriter metadataWriter = new MetadataJsonWriter(tempOutputPath);
                metadataWriter.write(metadataMap);
            }

            if (metadataPath == null) {
                logger.error("Metadata path not initialized; skipping packaging");
                return;
            }

            if (OutputPackager.isZipFile(outputPath)) {
                OutputPackager.packageOutput(tempOutputPath, metadataPath,
                        senderKeyPair.getPublic(), outputPath);
                logger.info("\u2713 Output package created: {}", outputPath);
                logger.info("  \u251c\u2500 tokens.{} ({})", outputType, hashOnly ? "hashed" : "encrypted");
                logger.info("  \u251c\u2500 tokens.metadata.json");
                logger.info("  \u2514\u2500 sender_public_key.pem");

                new java.io.File(tempOutputPath).delete();
                new java.io.File(metadataPath).delete();
            } else {
                logger.info("\u2713 Tokens generated successfully");
                logger.info("Note: Use .zip extension for automatic packaging with sender's public key");

                // Move temp output to final output path for non-zip outputs
                java.io.File tempFile = new java.io.File(tempOutputPath);
                java.io.File finalFile = new java.io.File(outputPath);
                if (tempFile.exists()) {
                    if (tempFile.renameTo(finalFile)) {
                        logger.info("\u2713 Moved temp output {} to final output {}", tempOutputPath, outputPath);
                    } else {
                        logger.warn("Could not move temp output {} to final path {}", tempOutputPath, outputPath);
                    }
                } else {
                    logger.warn("Temp output not found at {}", tempOutputPath);
                }

                // Move metadata to be alongside final output
                int finalDot = outputPath.lastIndexOf('.');
                String finalMetadataBase = finalDot > 0 ? outputPath.substring(0, finalDot) : outputPath;
                String finalMetadataPath = finalMetadataBase + Metadata.METADATA_FILE_EXTENSION;
                java.io.File metadataFile = new java.io.File(metadataPath);
                java.io.File finalMetadataFile = new java.io.File(finalMetadataPath);
                if (metadataFile.exists()) {
                    if (metadataFile.renameTo(finalMetadataFile)) {
                        logger.info("\u2713 Moved metadata {} to {}", metadataPath, finalMetadataPath);
                    } else {
                        logger.warn("Could not move metadata {} to {}", metadataPath, finalMetadataPath);
                    }
                } else {
                    logger.warn("Metadata not found at {}", metadataPath);
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
            String receiverKeypairPath, String ecdhCurveNormalized) {
        try {
            logger.info("Decrypting tokens with ECDH key exchange...");

            PublicKey senderPublicKey;
            String tokensInputPath = inputPath;

            // Extract sender's public key from ZIP if needed
            if (OutputPackager.isZipFile(inputPath)) {
                senderPublicKey = OutputPackager.extractSenderPublicKey(inputPath);
                logger.info("\u2713 Extracted sender's public key from ZIP");

                // Extract tokens to temp file
                tokensInputPath = inputPath.replace(".zip", "_temp." + inputType);
                OutputPackager.extractTokensFile(inputPath, tokensInputPath);
            } else if (senderPublicKeyPath != null && !senderPublicKeyPath.isBlank()) {
                PublicKeyLoader publicKeyLoader = new PublicKeyLoader();
                senderPublicKey = publicKeyLoader.loadPublicKey(senderPublicKeyPath);
                logger.info("\u2713 Loaded sender's public key from: {}", senderPublicKeyPath);
            } else {
                logger.error("Sender's public key must be provided or available in ZIP");
                return;
            }

            // Load receiver's private key
            KeyPairManager receiverKeyManager = receiverKeypairPath != null
                    ? new KeyPairManager(new java.io.File(receiverKeypairPath).getParent(), ecdhCurveNormalized)
                    : new KeyPairManager(KeyPairManager.DEFAULT_KEY_DIR, ecdhCurveNormalized);
            KeyPair receiverKeyPair = receiverKeyManager.loadKeyPair(
                    receiverKeypairPath != null ? receiverKeypairPath
                            : receiverKeyManager.getKeyDirectory() + "/keypair.pem");
            logger.info("\u2713 Loaded receiver's private key from: {}", receiverKeyManager.getKeyDirectory());

            // Perform ECDH key exchange (same as sender)
            KeyExchange keyExchange = new KeyExchange();
            KeyExchange.DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
                    receiverKeyPair.getPrivate(), senderPublicKey);
            logger.info("\u2713 Performed ECDH key exchange");
            logger.info("\u2713 Derived encryption key (matches sender's key)");

            // Decrypt tokens
            DecryptTokenTransformer decryptor = new DecryptTokenTransformer(keys.getEncryptionKey());
            try (TokenReader reader = createTokenReader(tokensInputPath, inputType);
                    TokenWriter writer = createTokenWriter(outputPath, outputType)) {
                TokenDecryptionProcessor.process(reader, writer, decryptor);
            }

            // Clean up temp file if we extracted from ZIP
            if (OutputPackager.isZipFile(inputPath)) {
                new java.io.File(tokensInputPath).delete();
            }

            logger.info("\u2713 Tokens decrypted successfully");
            logger.info("\u2713 Output written to: {}", outputPath);

        } catch (Exception e) {
            logger.error("Error decrypting tokens with ECDH: ", e);
        }
    }
}
