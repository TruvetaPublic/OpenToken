/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The PersonAttributesMetadataWriter class is responsible for writing metadata for output file
 * such as Java version, OpenToken version, output format, total rows, and invalid attributes..
 */
public class PersonAttributesMetadataWriter {

    Map<String, Object> metadata = new LinkedHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public PersonAttributesMetadataWriter(String outputFormat, int totalRows, Long invalidAttributeCount, Map<String, Long> invalidAttributesByType) {
        metadata.put("java_version", System.getProperty("java.version"));
        metadata.put("opentoken_version", "1.0.0");
        metadata.put("output_format", outputFormat);
        metadata.put("total_rows", totalRows);
        metadata.put("total_rows_with_invalid_attributes", invalidAttributeCount);
        metadata.put("invalid_attributes_by_type", new HashMap<>(invalidAttributesByType));
    }

    public void writeToFile(String baseFilePath) throws IOException {
        Files.write(
                Paths.get(baseFilePath + ".metadata.json"),
                objectMapper.writeValueAsBytes(metadata)
        );
    }

    public String toJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(metadata);
    }
}
