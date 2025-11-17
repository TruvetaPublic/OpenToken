/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.parquet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.truveta.opentoken.processor.TokenConstants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenParquetReaderTest {

    private File tempFile;
    private String tempFilePath;
    private TokenParquetReader reader;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = Files.createTempFile("test_token_", ".parquet").toFile();
        tempFilePath = tempFile.getAbsolutePath();
        // Delete the temp file so Parquet can create it fresh
        tempFile.delete();
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
        // Write test data
        try (TokenParquetWriter writer = new TokenParquetWriter(tempFilePath)) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put(TokenConstants.RULE_ID, "T1");
            data.put(TokenConstants.TOKEN, "abc123token");
            data.put(TokenConstants.RECORD_ID, "rec-001");
            writer.writeToken(data);
        }

        reader = new TokenParquetReader(tempFilePath);

        assertTrue(reader.hasNext());
        Map<String, String> record = reader.next();

        assertEquals("T1", record.get(TokenConstants.RULE_ID));
        assertEquals("abc123token", record.get(TokenConstants.TOKEN));
        assertEquals("rec-001", record.get(TokenConstants.RECORD_ID));

        assertFalse(reader.hasNext());
    }

    @Test
    void testReadMultipleTokens() throws Exception {
        // Write test data
        try (TokenParquetWriter writer = new TokenParquetWriter(tempFilePath)) {
            Map<String, String> data1 = new LinkedHashMap<>();
            data1.put(TokenConstants.RULE_ID, "T1");
            data1.put(TokenConstants.TOKEN, "token1");
            data1.put(TokenConstants.RECORD_ID, "rec-001");
            writer.writeToken(data1);

            Map<String, String> data2 = new LinkedHashMap<>();
            data2.put(TokenConstants.RULE_ID, "T2");
            data2.put(TokenConstants.TOKEN, "token2");
            data2.put(TokenConstants.RECORD_ID, "rec-002");
            writer.writeToken(data2);
        }

        reader = new TokenParquetReader(tempFilePath);

        assertTrue(reader.hasNext());
        Map<String, String> record1 = reader.next();
        assertEquals("T1", record1.get(TokenConstants.RULE_ID));
        assertEquals("token1", record1.get(TokenConstants.TOKEN));
        assertEquals("rec-001", record1.get(TokenConstants.RECORD_ID));

        assertTrue(reader.hasNext());
        Map<String, String> record2 = reader.next();
        assertEquals("T2", record2.get(TokenConstants.RULE_ID));
        assertEquals("token2", record2.get(TokenConstants.TOKEN));
        assertEquals("rec-002", record2.get(TokenConstants.RECORD_ID));

        assertFalse(reader.hasNext());
    }

    @Test
    void testReadEmptyToken() throws Exception {
        // Write test data
        try (TokenParquetWriter writer = new TokenParquetWriter(tempFilePath)) {
            Map<String, String> data = new LinkedHashMap<>();
            data.put(TokenConstants.RULE_ID, "T1");
            data.put(TokenConstants.TOKEN, "");
            data.put(TokenConstants.RECORD_ID, "rec-001");
            writer.writeToken(data);
        }

        reader = new TokenParquetReader(tempFilePath);

        assertTrue(reader.hasNext());
        Map<String, String> record = reader.next();

        assertEquals("T1", record.get(TokenConstants.RULE_ID));
        assertEquals("", record.get(TokenConstants.TOKEN));
        assertEquals("rec-001", record.get(TokenConstants.RECORD_ID));

        assertFalse(reader.hasNext());
    }

    @Test
    void testIteratorInterface() throws Exception {
        // Write test data
        try (TokenParquetWriter writer = new TokenParquetWriter(tempFilePath)) {
            Map<String, String> data1 = new LinkedHashMap<>();
            data1.put(TokenConstants.RULE_ID, "T1");
            data1.put(TokenConstants.TOKEN, "token1");
            data1.put(TokenConstants.RECORD_ID, "rec-001");
            writer.writeToken(data1);

            Map<String, String> data2 = new LinkedHashMap<>();
            data2.put(TokenConstants.RULE_ID, "T2");
            data2.put(TokenConstants.TOKEN, "token2");
            data2.put(TokenConstants.RECORD_ID, "rec-002");
            writer.writeToken(data2);
        }

        reader = new TokenParquetReader(tempFilePath);

        int count = 0;
        while (reader.hasNext()) {
            Map<String, String> record = reader.next();
            count++;
            assertTrue(record.containsKey(TokenConstants.RULE_ID));
            assertTrue(record.containsKey(TokenConstants.TOKEN));
            assertTrue(record.containsKey(TokenConstants.RECORD_ID));
        }

        assertEquals(2, count);
    }

    @Test
    void testNonExistentFile() {
        assertThrows(IOException.class, () -> {
            new TokenParquetReader("/nonexistent/path/file.parquet");
        });
    }
}
