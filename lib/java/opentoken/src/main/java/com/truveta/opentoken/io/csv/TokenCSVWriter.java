/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.csv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.io.TokenWriter;
import com.truveta.opentoken.processor.TokenConstants;

/**
 * Writes decrypted tokens to a CSV file.
 * Output columns: RuleId, Token, RecordId
 */
public class TokenCSVWriter implements TokenWriter {
    private static final Logger logger = LoggerFactory.getLogger(TokenCSVWriter.class);

    private final BufferedWriter writer;

    /**
     * Initialize the class with the output file in CSV format.
     * 
     * @param filePath The output file path.
     * @throws IOException If an I/O error occurs.
     */
    public TokenCSVWriter(String filePath) throws IOException {
        // Create directory if it doesn't exist
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        this.writer = new BufferedWriter(new FileWriter(filePath));

        // Write header with LF line ending to match Python output
        writer.write(TokenConstants.RULE_ID + "," + TokenConstants.TOKEN + "," + TokenConstants.RECORD_ID);
        writer.write('\n');
    }

    /**
     * Write a token row to the CSV file.
     * 
     * @param data A map containing RuleId, Token, and RecordId.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void writeToken(Map<String, String> data) throws IOException {
        String ruleId = data.getOrDefault(TokenConstants.RULE_ID, "");
        String token = data.getOrDefault(TokenConstants.TOKEN, "");
        String recordId = data.getOrDefault(TokenConstants.RECORD_ID, "");

        writer.write(String.format("%s,%s,%s", ruleId, token, recordId));
        writer.write('\n'); // Use LF line ending to match Python output
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
