/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.parquet;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersonAttributesParquetWriterTest {

    private File tempFile;
    private String tempFilePath;
    private PersonAttributesParquetWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        tempFile = Files.createTempFile("test_data", ".parquet").toFile();
        tempFilePath = tempFile.getAbsolutePath();
        writer = new PersonAttributesParquetWriter(tempFilePath);
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
    void testWriteSingleRecord() throws Exception {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("RecordId", "123");
        data.put("Name", "John Doe");
        data.put("SocialSecurityNumber", "123-45-6789");

        writer.writeAttributes(data);
        writer.close();

        try (PersonAttributesParquetReader reader = new PersonAttributesParquetReader(tempFilePath)) {
            boolean hasNext = reader.hasNext();
            assertTrue(hasNext);

            Map<String, String> record = reader.next();
            assertNotNull(record);
            assertEquals("123", record.get("RecordId"));
            assertEquals("123-45-6789", record.get("SocialSecurityNumber"));
            assertEquals("John Doe", record.get("Name"));
        }
    }

    @Test
    void testWriteMultipleRecords() throws Exception {
        Map<String, String> data1 = new LinkedHashMap<>();
        data1.put("RecordId", "123");
        data1.put("Name", "John Doe");
        data1.put("SocialSecurityNumber", "123-45-6789");

        Map<String, String> data2 = new LinkedHashMap<>();
        data2.put("RecordId", "456");
        data2.put("Name", "Jane Smith");
        data2.put("SocialSecurityNumber", "987-65-4321");

        writer.writeAttributes(data1);
        writer.writeAttributes(data2);
        writer.close();

        try (PersonAttributesParquetReader reader = new PersonAttributesParquetReader(tempFilePath)) {
            boolean hasNext = reader.hasNext();
            assertTrue(hasNext);

            Map<String, String> record = reader.next();
            assertNotNull(record);
            assertEquals("123", record.get("RecordId"));
            assertEquals("123-45-6789", record.get("SocialSecurityNumber"));
            assertEquals("John Doe", record.get("Name"));

            hasNext = reader.hasNext();
            assertTrue(hasNext);

            record = reader.next();
            assertNotNull(record);
            assertEquals("456", record.get("RecordId"));
            assertEquals("987-65-4321", record.get("SocialSecurityNumber"));
            assertEquals("Jane Smith", record.get("Name"));
        }
    }
}