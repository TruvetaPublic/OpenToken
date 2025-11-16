/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.JCommander;
import com.truveta.opentoken.tokentransformer.DecryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;
import com.truveta.opentoken.io.MetadataWriter;
import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.io.TokenReader;
import com.truveta.opentoken.io.TokenWriter;
import com.truveta.opentoken.io.csv.PersonAttributesCSVReader;
import com.truveta.opentoken.io.csv.PersonAttributesCSVWriter;
import com.truveta.opentoken.io.csv.TokenCSVReader;
import com.truveta.opentoken.io.csv.TokenCSVWriter;
import com.truveta.opentoken.io.json.MetadataJsonWriter;
import com.truveta.opentoken.io.parquet.PersonAttributesParquetReader;
import com.truveta.opentoken.io.parquet.PersonAttributesParquetWriter;
import com.truveta.opentoken.io.parquet.TokenParquetReader;
import com.truveta.opentoken.io.parquet.TokenParquetWriter;
import com.truveta.opentoken.processor.PersonAttributesProcessor;
import com.truveta.opentoken.processor.TokenDecryptionProcessor;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        CommandLineArguments commandLineArguments = loadCommandLineArguments(args);
        String hashingSecret = commandLineArguments.getHashingSecret();
        String encryptionKey = commandLineArguments.getEncryptionKey();
        String inputPath = commandLineArguments.getInputPath();
        String inputType = commandLineArguments.getInputType();
        String outputPath = commandLineArguments.getOutputPath();
        String outputType = commandLineArguments.getOutputType();
        boolean decryptMode = commandLineArguments.isDecrypt();
        boolean hashOnlyMode = commandLineArguments.isHashOnly();
        
        if (outputType == null || outputType.isEmpty()) {
            outputType = inputType; // defaulting to input type if not provided
        }

        logger.info("Decrypt Mode: {}", decryptMode);
        logger.info("Hash-Only Mode: {}", hashOnlyMode);
        if (logger.isInfoEnabled()) {
            logger.info("Hashing Secret: {}", maskString(hashingSecret));
            logger.info("Encryption Key: {}", maskString(encryptionKey));
        }
        logger.info("Input Path: {}", inputPath);
        logger.info("Input Type: {}", inputType);
        logger.info("Output Path: {}", outputPath);
        logger.info("Output Type: {}", outputType);

        // Validate input and output types for both modes
        if (!(CommandLineArguments.TYPE_CSV.equals(inputType) || CommandLineArguments.TYPE_PARQUET.equals(inputType))) {
            logger.error("Only csv and parquet input types are supported!");
            return;
        }
        if (!(CommandLineArguments.TYPE_CSV.equals(outputType) || CommandLineArguments.TYPE_PARQUET.equals(outputType))) {
            logger.error("Only csv and parquet output types are supported!");
            return;
        }

        // Process based on mode
        if (decryptMode) {
            // Decrypt mode - process encrypted tokens
            if (encryptionKey == null || encryptionKey.isBlank()) {
                logger.error("Encryption key must be specified for decryption");
                return;
            }
            
            decryptTokens(inputPath, outputPath, inputType, outputType, encryptionKey);
            logger.info("Token decryption completed successfully.");
        } else {
            // Token generation mode - validate and process person attributes
            // Hashing secret is always required
            if (hashingSecret == null || hashingSecret.isBlank()) {
                logger.error("Hashing secret must be specified");
                return;
            }
            
            // Encryption key is only required when not in hash-only mode
            if (!hashOnlyMode && (encryptionKey == null || encryptionKey.isBlank())) {
                logger.error("Encryption key must be specified (or use --hash-only to skip encryption)");
                return;
            }

            processTokens(inputPath, outputPath, inputType, outputType, hashingSecret, encryptionKey, hashOnlyMode);
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
            
            // Set encryption secret and mark encryption status
            if (!hashOnlyMode) {
                metadata.addHashedSecret(Metadata.ENCRYPTION_SECRET_HASH, encryptionKey);
                metadataMap.put("encryption_used", true);
            } else {
                metadataMap.put("encryption_used", false);
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

    private static CommandLineArguments loadCommandLineArguments(String[] args) {
        logger.debug("Processing command line arguments. {}", String.join("|", args));
        CommandLineArguments commandLineArguments = new CommandLineArguments();
        JCommander.newBuilder().addObject(commandLineArguments).build().parse(args);
        logger.info("Command line arguments processed.");
        return commandLineArguments;
    }

    private static String maskString(String input) {
        if (input == null || input.length() <= 3) {
            return input;
        }
        return input.substring(0, 3) + "*".repeat(input.length() - 3);
    }

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
}