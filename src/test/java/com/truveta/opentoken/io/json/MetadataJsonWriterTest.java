/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truveta.opentoken.Const;
import com.truveta.opentoken.io.MetadataWriter;

/**
 * Tests for the MetadataJsonWriter class.
 * This test verifies that the JSON implementation of the MetadataWriter
 * correctly writes metadata to a JSON file with the expected format.
 */
class MetadataJsonWriterTest {
    
    private MetadataWriter writer;
    private String metadataFilePath;
    
    @BeforeEach
    void setUp() {
        writer = new MetadataJsonWriter();
        // Set the metadata file path
        metadataFilePath = Const.METADATA_OUTPUT_FILE + Const.METADATA_FILE_EXTENSION;
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Delete the test output file if it exists
        Files.deleteIfExists(Paths.get(metadataFilePath));
    }
    
    @Test
    void testWriteMetadata_SimpleKeyValues() throws IOException {
        // Create a sample metadata map
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("key1", "value1");
        metadataMap.put("key2", "value2");
        metadataMap.put("key3", "value3");
        
        writer.writeMetadata(metadataMap);
        
        File outputFile = new File(metadataFilePath);
        assertTrue(outputFile.exists(), "Metadata file should have been created");
        
        // Read the JSON file and verify its contents
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(outputFile);
        
        // Verify all keys and values were correctly written
        assertEquals("value1", root.get("key1").asText());
        assertEquals("value2", root.get("key2").asText());
        assertEquals("value3", root.get("key3").asText());
    }
    
    @Test
    void testWriteMetadata_NestedJsonValues() throws IOException {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("simpleKey", "simpleValue");
        metadataMap.put(Const.INVALID_ATTRIBUTES_BY_TYPE, "{\"attr1\":10,\"attr2\":20}");
        
        writer.writeMetadata(metadataMap);
        
        File outputFile = new File(metadataFilePath);
        assertTrue(outputFile.exists(), "Metadata file should have been created");
        
        // Read the JSON file and verify its contents
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(outputFile);
        
        // Verify simple key-value was correctly written
        assertEquals("simpleValue", root.get("simpleKey").asText());
        
        // Verify nested JSON was correctly parsed and not double-escaped
        JsonNode nestedJson = root.get(Const.INVALID_ATTRIBUTES_BY_TYPE);
        assertTrue(nestedJson.isObject(), "Nested JSON should be parsed as an object");
        assertEquals(10, nestedJson.get("attr1").asInt());
        assertEquals(20, nestedJson.get("attr2").asInt());
    }
    
    @Test
    void testWriteMetadata_MalformedJsonValue() throws IOException {
        // Create a metadata map with malformed JSON string
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(Const.INVALID_ATTRIBUTES_BY_TYPE, "{malformed json}");
        
        // Write the metadata - should not throw exception
        assertDoesNotThrow(() -> writer.writeMetadata(metadataMap));
        
        // Verify the file was created
        File outputFile = new File(metadataFilePath);
        assertTrue(outputFile.exists(), "Metadata file should have been created");
        
        // Read the JSON file and verify the malformed JSON was handled correctly
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(outputFile);
        
        // Verify the malformed JSON was stored as a string
        assertTrue(root.has(Const.INVALID_ATTRIBUTES_BY_TYPE), "The malformed JSON key should exist in the output");
        assertEquals("{malformed json}", root.get(Const.INVALID_ATTRIBUTES_BY_TYPE).asText(), "Malformed JSON should be stored as a string");
    }
}
