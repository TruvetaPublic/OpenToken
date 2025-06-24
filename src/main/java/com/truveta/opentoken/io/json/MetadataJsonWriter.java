/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.json;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truveta.opentoken.Const;
import com.truveta.opentoken.io.MetadataWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A JSON implementation of the MetadataWriter interface.
 * This class is responsible for writing metadata in JSON format to a specified
 * output file.
 */
public class MetadataJsonWriter implements MetadataWriter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public MetadataJsonWriter() {}

    /**
     * Writes the provided metadata map to a JSON file.
     * The file is saved at the path specified by Const.METADATA_OUTPUT_FILE with a
     * .metadata.json extension.
     *
     * @param metadataMap a map containing metadata key-value pairs.
     * @throws IOException if an error occurs while writing the metadata to the
     *                     file.
     */
    @Override
    public void writeMetadata(Map<String, String> metadataMap) throws IOException {
        // Create a node tree that allows mixed types
        ObjectNode root = objectMapper.createObjectNode();
        
        metadataMap.forEach((key, value) -> {
            // Special handling for InvalidAttributesByType to prevent double escaping
            if (Const.INVALID_ATTRIBUTES_BY_TYPE.equals(key) && value.startsWith("{")) {
                try {
                    // Parse the JSON string back to an object and add it directly
                    root.set(key, objectMapper.readTree(value));
                } catch (Exception e) {
                    // Fallback if parsing fails
                    root.put(key, value);
                }
            } else {
                root.put(key, value);
            }
        });

        // Write the properly structured JSON
        Files.write(
                Paths.get(Const.METADATA_OUTPUT_FILE + Const.METADATA_FILE_EXTENSION),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root));
    }
}
