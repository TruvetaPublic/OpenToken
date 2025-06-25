/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;
import com.truveta.opentoken.io.MetadataWriter;
import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.io.csv.PersonAttributesCSVReader;
import com.truveta.opentoken.io.csv.PersonAttributesCSVWriter;
import com.truveta.opentoken.io.json.MetadataJsonWriter;
import com.truveta.opentoken.io.parquet.PersonAttributesParquetReader;
import com.truveta.opentoken.io.parquet.PersonAttributesParquetWriter;
import com.truveta.opentoken.processor.PersonAttributesProcessor;

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
        if (outputType == null || outputType.isEmpty()) {
            outputType = inputType; // defaulting to input type if not provided
        }

        if (logger.isInfoEnabled()) {
            logger.info("Hashing Secret: {}", maskString(hashingSecret));
            logger.info("Encryption Key: {}", maskString(encryptionKey));
        }
        logger.info("Input Path: {}", inputPath);
        logger.info("Input Type: {}", inputType);
        logger.info("Output Path: {}", outputPath);
        logger.info("Output Type: {}", outputType);

        if (!(CommandLineArguments.TYPE_CSV.equals(inputType) || CommandLineArguments.TYPE_PARQUET.equals(inputType))) {
            logger.error("Only csv and parquet input types are supported!");
            return;
        } else if (hashingSecret == null || hashingSecret.isBlank() || encryptionKey == null
                || encryptionKey.isBlank()) {
            logger.error("Hashing secret and encryption key must be specified");
            return;
        }

        List<TokenTransformer> tokenTransformerList = new ArrayList<>();
        try {
            tokenTransformerList.add(new HashTokenTransformer(hashingSecret));
            tokenTransformerList.add(new EncryptTokenTransformer(encryptionKey));
        } catch (Exception e) {
            logger.error("Error in initializing the transformer. Execution halted. ", e);
            return;
        }

        try (PersonAttributesReader reader = createPersonAttributesReader(inputPath, inputType);
                PersonAttributesWriter writer = createPersonAttributesWriter(outputPath, outputType)) {

            // Create initial metadata with system information
            Map<String, String> metadataMap = writeMetaData();
            
            // Process data and get updated metadata
            Map<String, String> processedMetadata = PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);
            
            // Write the metadata to file
            MetadataWriter metadataWriter = new MetadataJsonWriter();
            metadataWriter.writeMetadata(processedMetadata);

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

    private static Map<String, String> writeMetaData() throws IOException {
        Map<String, String> metadata = new LinkedHashMap<>();
        try {
            // System information
            Map<String, String> sysInfo = Map.of(
                Const.PLATFORM, Const.PLATFORM_JAVA,
                Const.JAVA_VERSION, Const.SYSTEM_JAVA_VERSION,
                Const.OPENTOKEN_VERSION, ApplicationVersion.getApplicationVersion()
            );
            metadata.putAll(sysInfo);
            
            // Initialize statistics with zeros
            // These will be overwritten by the processor with actual values
            Map<String, String> initialStats = Map.of(
                Const.TOTAL_ROWS, "0",
                Const.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES, "0"
            );
            metadata.putAll(initialStats);
            
            // Initialize complex JSON value
            metadata.put(Const.INVALID_ATTRIBUTES_BY_TYPE, new ObjectMapper().writeValueAsString(Map.of()));
        } catch (Exception e) {
            logger.error("Error creating metadata", e);
        }
        return metadata;
    }
}