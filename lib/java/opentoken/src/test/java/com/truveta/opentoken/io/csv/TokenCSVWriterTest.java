/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.truveta.opentoken.processor.TokenConstants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenCSVWriterTest {

    private File tempFile;
    private TokenCSVWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = Files.createTempFile("test_token_", ".csv").toFile();
        writer = new TokenCSVWriter(tempFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (writer != null) {
            writer.close();
        }
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void testWriteSingleToken() throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(TokenConstants.RULE_ID, "T1");
        data.put(TokenConstants.TOKEN, "abc123token");
        data.put(TokenConstants.RECORD_ID, "rec-001");

        writer.writeToken(data);
        writer.close();

        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String header = reader.readLine();
            assertEquals("RuleId,Token,RecordId", header);

            String record = reader.readLine();
            assertEquals("T1,abc123token,rec-001", record);
        }
    }

    @Test
    void testWriteMultipleTokens() throws Exception {
        Map<String, String> data1 = new LinkedHashMap<>();
        data1.put(TokenConstants.RULE_ID, "T1");
        data1.put(TokenConstants.TOKEN, "token1");
        data1.put(TokenConstants.RECORD_ID, "rec-001");

        Map<String, String> data2 = new LinkedHashMap<>();
        data2.put(TokenConstants.RULE_ID, "T2");
        data2.put(TokenConstants.TOKEN, "token2");
        data2.put(TokenConstants.RECORD_ID, "rec-002");

        writer.writeToken(data1);
        writer.writeToken(data2);
        writer.close();

        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String header = reader.readLine();
            assertEquals("RuleId,Token,RecordId", header);

            String record1 = reader.readLine();
            assertEquals("T1,token1,rec-001", record1);

            String record2 = reader.readLine();
            assertEquals("T2,token2,rec-002", record2);
        }
    }

    @Test
    void testHeaderWrittenOnlyOnce() throws Exception {
        Map<String, String> data1 = new LinkedHashMap<>();
        data1.put(TokenConstants.RULE_ID, "T1");
        data1.put(TokenConstants.TOKEN, "token1");
        data1.put(TokenConstants.RECORD_ID, "rec-001");

        Map<String, String> data2 = new LinkedHashMap<>();
        data2.put(TokenConstants.RULE_ID, "T2");
        data2.put(TokenConstants.TOKEN, "token2");
        data2.put(TokenConstants.RECORD_ID, "rec-002");

        writer.writeToken(data1);
        writer.writeToken(data2);
        writer.close();

        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String header = reader.readLine();
            assertEquals("RuleId,Token,RecordId", header);

            String record1 = reader.readLine();
            assertEquals("T1,token1,rec-001", record1);

            String record2 = reader.readLine();
            assertEquals("T2,token2,rec-002", record2);

            // Ensure there are no more lines
            String nextLine = reader.readLine();
            assertTrue(nextLine == null || nextLine.isEmpty());
        }
    }

    @Test
    void testWriteTokenWithBlankValue() throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(TokenConstants.RULE_ID, "T1");
        data.put(TokenConstants.TOKEN, "");
        data.put(TokenConstants.RECORD_ID, "rec-001");

        writer.writeToken(data);
        writer.close();

        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
            String header = reader.readLine();
            assertEquals("RuleId,Token,RecordId", header);

            String record = reader.readLine();
            assertEquals("T1,,rec-001", record);
        }
    }
}
