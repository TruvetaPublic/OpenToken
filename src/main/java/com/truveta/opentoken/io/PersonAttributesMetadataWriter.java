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

public class PersonAttributesMetadataWriter {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static Map<String, Object> buildMetadata(
            String outputFormat,
            int totalRows,
            Long invalidAttributeCount,
            Map<String, Long> invalidAttributesByType
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("java_version", System.getProperty("java.version"));
        metadata.put("library_revision", "1.0.0");
        metadata.put("output_format", outputFormat);
        metadata.put("total_rows", totalRows);
        metadata.put("total_rows_with_invalid_attributes", invalidAttributeCount);
        metadata.put("invalid_attributes_by_type", new HashMap<>(invalidAttributesByType));
        return metadata;
    }

    public static void writeToFile(String baseFilePath, Map<String, Object> metadata) throws IOException {
        Files.write(
                Paths.get(baseFilePath + ".metadata.json"),
                objectMapper.writeValueAsBytes(metadata)
        );
    }

    public static String toJson(Map<String, Object> metadata) throws JsonProcessingException {
        return objectMapper.writeValueAsString(metadata);
    }
}
