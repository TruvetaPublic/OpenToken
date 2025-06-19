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
import com.truveta.opentoken.Const;

/**
 * The PersonAttributesMetadataWriter class is responsible for writing metadata for output file
 * such as Java version, OpenToken version, output format, total rows, and invalid attributes..
 */
public class OutputMetadataWriter {

    Map<String, Object> metadata = new LinkedHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OutputMetadataWriter(String outputFormat, int totalRows, Long invalidAttributeCount, Map<String, Long> invalidAttributesByType) {
        metadata.put(Const.javaVersion, Const.systemJavaVersion);
        metadata.put(Const.openTokenVersion, "1.7.0");
        metadata.put(Const.outputFormat, outputFormat);
        metadata.put(Const.totalRows, totalRows);
        metadata.put(Const.totalRowsWithInvalidAttributes, invalidAttributeCount);
        metadata.put(Const.invalidAttributesByType, new HashMap<>(invalidAttributesByType));
    }

    public void writeToFile(String baseFilePath) throws IOException {
        Files.write(
                Paths.get(baseFilePath + Const.metadataFileExtension),
                objectMapper.writeValueAsBytes(metadata)
        );
    }

    public String toJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(metadata);
    }
}
