/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.truveta.opentoken.processor.TokenConstants;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenCSVReaderTest {

    private File tempFile;
    private TokenCSVReader reader;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = Files.createTempFile("test_token_", ".csv").toFile();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (reader != null) {
            reader.close();
        }
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void testReadSingleToken() throws Exception {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("RuleId,Token,RecordId\n");
            writer.write("T1,abc123token,rec-001\n");
        }

        reader = new TokenCSVReader(tempFile.getAbsolutePath());
        
        assertTrue(reader.hasNext());
        Map<String, String> data = reader.next();
        
        assertEquals("T1", data.get(TokenConstants.RULE_ID));
        assertEquals("abc123token", data.get(TokenConstants.TOKEN));
        assertEquals("rec-001", data.get(TokenConstants.RECORD_ID));
        
        assertFalse(reader.hasNext());
    }

    @Test
    void testReadMultipleTokens() throws Exception {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("RuleId,Token,RecordId\n");
            writer.write("T1,token1,rec-001\n");
            writer.write("T2,token2,rec-002\n");
        }

        reader = new TokenCSVReader(tempFile.getAbsolutePath());
        
        assertTrue(reader.hasNext());
        Map<String, String> data1 = reader.next();
        assertEquals("T1", data1.get(TokenConstants.RULE_ID));
        assertEquals("token1", data1.get(TokenConstants.TOKEN));
        assertEquals("rec-001", data1.get(TokenConstants.RECORD_ID));
        
        assertTrue(reader.hasNext());
        Map<String, String> data2 = reader.next();
        assertEquals("T2", data2.get(TokenConstants.RULE_ID));
        assertEquals("token2", data2.get(TokenConstants.TOKEN));
        assertEquals("rec-002", data2.get(TokenConstants.RECORD_ID));
        
        assertFalse(reader.hasNext());
    }

    @Test
    void testReadEmptyToken() throws Exception {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("RuleId,Token,RecordId\n");
            writer.write("T1,,rec-001\n");
        }

        reader = new TokenCSVReader(tempFile.getAbsolutePath());
        
        assertTrue(reader.hasNext());
        Map<String, String> data = reader.next();
        
        assertEquals("T1", data.get(TokenConstants.RULE_ID));
        assertEquals("", data.get(TokenConstants.TOKEN));
        assertEquals("rec-001", data.get(TokenConstants.RECORD_ID));
        
        assertFalse(reader.hasNext());
    }

    @Test
    void testMissingRequiredColumn() throws Exception {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("RuleId,Token\n");
            writer.write("T1,token1\n");
        }

        assertThrows(IOException.class, () -> {
            new TokenCSVReader(tempFile.getAbsolutePath());
        });
    }

    @Test
    void testEmptyFile() throws Exception {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("");
        }

        assertThrows(IOException.class, () -> {
            new TokenCSVReader(tempFile.getAbsolutePath());
        });
    }

    @Test
    void testIteratorInterface() throws Exception {
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("RuleId,Token,RecordId\n");
            writer.write("T1,token1,rec-001\n");
            writer.write("T2,token2,rec-002\n");
        }

        reader = new TokenCSVReader(tempFile.getAbsolutePath());
        
        int count = 0;
        while (reader.hasNext()) {
            Map<String, String> data = reader.next();
            count++;
            assertTrue(data.containsKey(TokenConstants.RULE_ID));
            assertTrue(data.containsKey(TokenConstants.TOKEN));
            assertTrue(data.containsKey(TokenConstants.RECORD_ID));
        }
        
        assertEquals(2, count);
    }
}
