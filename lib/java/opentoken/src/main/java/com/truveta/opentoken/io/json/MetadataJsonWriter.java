/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.json;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truveta.opentoken.io.MetadataWriter;
import com.truveta.opentoken.Metadata;

/**
 * A JSON implementation of the MetadataWriter interface.
 * This class is responsible for writing metadata in JSON format to a specified
 * output file.
 */
public class MetadataJsonWriter implements MetadataWriter {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String outputFilePath;

    /**
     * Constructs a MetadataJsonWriter with a specified output path.
     * The metadata file will be created with the same base name as the output path,
     * but with the .metadata.json extension.
     * 
     * @param outputFilePath the base output file path to use for the metadata file
     */
    public MetadataJsonWriter(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    /**
     * Writes the provided metadata map to a JSON file.
     * The file is saved at the path specified by the outputFilePath provided in the constructor
     * with a .metadata.json extension.
     *
     * @param metadataMap a map containing metadata key-value pairs.
     * @throws IOException if an error occurs while writing the metadata to the
     *                     file.
     */
    @Override
    public void write(Map<String, Object> metadataMap) throws IOException {
        // Write the metadata map directly as JSON
        int lastDotIndex = outputFilePath.lastIndexOf('.');
        String basePath = lastDotIndex > 0 ? outputFilePath.substring(0, lastDotIndex) : outputFilePath;

        // Write with pretty printing and then fix the separator format to match Python
        String jsonString = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(metadataMap);

        // Replace " : " with ": " to match Python's json.dump format (no space before colon)
        jsonString = jsonString.replace(" : ", ": ");

        Files.write(
                Paths.get(basePath + Metadata.METADATA_FILE_EXTENSION),
                jsonString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
