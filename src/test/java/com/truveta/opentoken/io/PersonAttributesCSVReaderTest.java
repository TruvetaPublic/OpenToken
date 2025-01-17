/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

import static org.junit.jupiter.api.Assertions.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersonAttributesCSVReaderTest {

    private File tempFile;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("test_data", ".csv");
    }

    @AfterEach
    void tearDown() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    void testReadValidCSV() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("RecordId,SocialSecurityNumber,Name\n");
            writer.write("1,123-45-6789,John Doe\n");
            writer.write("2,987-65-4321,Jane Smith\n");
        }

        try (PersonAttributesCSVReader reader = new PersonAttributesCSVReader(tempFile.getAbsolutePath())) {
            assertTrue(reader.hasNext());

            Map<String, String> firstRecord = reader.next();
            assertEquals("1", firstRecord.get("RecordId"));
            assertEquals("123-45-6789", firstRecord.get("SocialSecurityNumber"));
            assertEquals("John Doe", firstRecord.get("Name"));

            assertTrue(reader.hasNext());

            Map<String, String> secondRecord = reader.next();
            assertEquals("2", secondRecord.get("RecordId"));
            assertEquals("987-65-4321", secondRecord.get("SocialSecurityNumber"));
            assertEquals("Jane Smith", secondRecord.get("Name"));

            assertFalse(reader.hasNext());
        }
    }

    @Test
    void testReadEmptyCSV() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("RecordId,SocialSecurityNumber,Name\n");
        }

        try (PersonAttributesCSVReader reader = new PersonAttributesCSVReader(tempFile.getAbsolutePath())) {
            assertFalse(reader.hasNext());
        }
    }

    @Test
    void testHasNext() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("RecordId,SocialSecurityNumber,Name\n");
            writer.write("1,123-45-6789,John Doe\n");
        }

        try (PersonAttributesCSVReader reader = new PersonAttributesCSVReader(tempFile.getAbsolutePath())) {
            assertTrue(reader.hasNext());
            reader.next();
            assertFalse(reader.hasNext());
        }
    }

    @Test
    void testNext() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("RecordId,SocialSecurityNumber,Name\n");
            writer.write("1,123-45-6789,John Doe\n");
        }

        try (PersonAttributesCSVReader reader = new PersonAttributesCSVReader(tempFile.getAbsolutePath())) {
            Map<String, String> record = reader.next();
            assertNotNull(record);
            assertEquals("1", record.get("RecordId"));
            assertEquals("123-45-6789", record.get("SocialSecurityNumber"));
            assertEquals("John Doe", record.get("Name"));
        }
    }

    @Test
    void testClose() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write("RecordId,SocialSecurityNumber,Name\n");
            writer.write("1,123-45-6789,John Doe\n");
        }

        PersonAttributesCSVReader reader = new PersonAttributesCSVReader(tempFile.getAbsolutePath());
        reader.close();
        assertThrows(NoSuchElementException.class, () -> reader.next());
    }

    @Test
    void testConstructorThrowsIOException() {
        String invalidFilePath = "non_existent_file.csv";
        assertThrows(IOException.class, () -> new PersonAttributesCSVReader(invalidFilePath));
    }
}