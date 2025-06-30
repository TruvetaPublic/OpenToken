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
import com.truveta.opentoken.io.MetadataWriter;
import com.truveta.opentoken.Metadata;

/**
 * Tests for the MetadataJsonWriter class.
 * This test verifies that the JSON implementation of the MetadataWriter
 * correctly writes metadata to a JSON file with the expected format.
 */
class MetadataJsonWriterTest {

    private MetadataWriter defaultWriter;
    private MetadataWriter customWriter;
    private String defaultOutputPath;
    private String defaultMetadataFilePath;
    private String customOutputPath;
    private String customMetadataFilePath;

    @BeforeEach
    void setUp() {
        // Initialize default writer and path
        defaultOutputPath = "target/default_output";
        defaultWriter = new MetadataJsonWriter(defaultOutputPath);
        defaultMetadataFilePath = defaultOutputPath + Metadata.METADATA_FILE_EXTENSION;

        // Create a custom output path for testing
        customOutputPath = "target/custom_output";
        customWriter = new MetadataJsonWriter(customOutputPath);
        customMetadataFilePath = customOutputPath + Metadata.METADATA_FILE_EXTENSION;
    }

    @AfterEach
    void tearDown() throws IOException {
        // Delete the test output files if they exist
        Files.deleteIfExists(Paths.get(defaultMetadataFilePath));
        Files.deleteIfExists(Paths.get(customMetadataFilePath));
    }

    @Test
    void testWriteMetadata_SimpleKeyValues() throws IOException {
        // Create a sample metadata map
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("key1", "value1");
        metadataMap.put("key2", "value2");
        metadataMap.put("key3", "value3");

        defaultWriter.write(metadataMap);

        File outputFile = new File(defaultMetadataFilePath);
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
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("simpleKey", "simpleValue");
        
        // Create a nested Map object instead of JSON string
        Map<String, Long> invalidAttributesMap = new HashMap<>();
        invalidAttributesMap.put("attr1", 10L);
        invalidAttributesMap.put("attr2", 20L);
        metadataMap.put("InvalidAttributesByType", invalidAttributesMap);

        defaultWriter.write(metadataMap);

        File outputFile = new File(defaultMetadataFilePath);
        assertTrue(outputFile.exists(), "Metadata file should have been created");

        // Read the JSON file and verify its contents
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(outputFile);

        // Verify simple key-value was correctly written
        assertEquals("simpleValue", root.get("simpleKey").asText());

        // Verify nested Map was written as a proper JSON object
        JsonNode nestedJson = root.get("InvalidAttributesByType");
        assertTrue(nestedJson.isObject(), "Map should be stored as JSON object");
        assertEquals(10, nestedJson.get("attr1").asLong(), "attr1 should be 10");
        assertEquals(20, nestedJson.get("attr2").asLong(), "attr2 should be 20");
    }

    @Test
    void testWriteMetadata_MapObjectValue() throws IOException {
        // Create a metadata map with a Map object value
        Map<String, Object> metadataMap = new HashMap<>();
        
        Map<String, Long> nestedMap = new HashMap<>();
        nestedMap.put("validAttribute", 5L);
        nestedMap.put("anotherAttribute", 15L);
        
        metadataMap.put("InvalidAttributesByType", nestedMap);
        metadataMap.put("TotalRows", 100);

        // Write the metadata - should not throw exception
        assertDoesNotThrow(() -> defaultWriter.write(metadataMap));

        // Verify the file was created
        File outputFile = new File(defaultMetadataFilePath);
        assertTrue(outputFile.exists(), "Metadata file should have been created");

        // Read the JSON file and verify the Map was handled correctly
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(outputFile);

        // Verify the Map was stored as a proper JSON object
        assertTrue(root.has("InvalidAttributesByType"), "The Map key should exist in the output");
        JsonNode invalidAttrs = root.get("InvalidAttributesByType");
        assertTrue(invalidAttrs.isObject(), "Map should be stored as JSON object");
        assertEquals(5, invalidAttrs.get("validAttribute").asLong());
        assertEquals(15, invalidAttrs.get("anotherAttribute").asLong());
        assertEquals(100, root.get("TotalRows").asInt());
    }

    @Test
    void testWriteMetadata_CustomOutputPath() throws IOException {
        // Create a sample metadata map
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("key1", "value1");
        metadataMap.put("key2", "value2");

        // Write metadata using the custom writer
        customWriter.write(metadataMap);

        // Verify the file was created at the custom location
        File outputFile = new File(customMetadataFilePath);
        assertTrue(outputFile.exists(), "Metadata file should have been created at the custom location");

        // Read and verify the contents
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(outputFile);

        assertEquals("value1", root.get("key1").asText());
        assertEquals("value2", root.get("key2").asText());
    }
}
