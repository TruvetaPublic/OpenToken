// Copyright (c) Truveta. All rights reserved.
package com.truveta.opentoken;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.JCommander;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;
import com.truveta.opentoken.io.PersonAttributesCSVReader;
import com.truveta.opentoken.io.PersonAttributesCSVWriter;
import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.processor.PersonAttributesProcessor;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class.getName());
    
    public static void main(String[] args) {
        CommandLineArguments commandLineArguments = loadCommandLineArguments(args);
        String hashingSecret = commandLineArguments.getHashingSecret();
        String encryptionKey = commandLineArguments.getEncryptionKey();
        String inputPath = commandLineArguments.getInputPath();
        String inputType = commandLineArguments.getInputType();
        String outputPath = commandLineArguments.getOutputPath();

        logger.info("Hashing Secret: {}", hashingSecret);
        logger.info("Encryption Key: {}", encryptionKey);
        logger.info("Input Path: {}", inputPath);
        logger.info("Input Type: {}", inputType);
        logger.info("Output Path: {}", outputPath);

        PersonAttributesReader reader = null;
        PersonAttributesWriter writer = null;

        if (inputType.equals("csv")) {
            reader = new PersonAttributesCSVReader(inputPath);
            writer = new PersonAttributesCSVWriter(outputPath);
        } else {
            logger.error("No input type other than csv supported yet!");
            return;
        }

        if ((hashingSecret == null || hashingSecret.isBlank()) && (encryptionKey == null || encryptionKey.isBlank())){
            logger.error("Hashing secret and encryption key must be specified");
            return;
        }

        List<TokenTransformer> tokenTransformerList = new ArrayList<>();
        try {
            tokenTransformerList.add(new HashTokenTransformer(hashingSecret));
            tokenTransformerList.add(new EncryptTokenTransformer(encryptionKey));
            PersonAttributesProcessor.process(reader, writer, tokenTransformerList);
        } catch (Exception e) {
            logger.error("Error in initializing the transformer. Execution halted. ", e);
            return;
        }
    }

    private static CommandLineArguments loadCommandLineArguments(String[] args) {
        logger.info("Processing command line arguments. {}", String.join("|", args));
        CommandLineArguments commandLineArguments = new CommandLineArguments();
        JCommander.newBuilder().addObject(commandLineArguments).build().parse(args);
        logger.info("Command line arguments processed.");
        return commandLineArguments;
    }
}