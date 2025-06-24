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

public class MetadataJsonWriter implements MetadataWriter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public MetadataJsonWriter() {}

    @Override
    public void writeMetadata(Map<String, String> metadataMap) throws IOException {
        
        Files.write(
                Paths.get(Const.METADATA_OUTPUT_FILE + Const.METADATA_FILE_EXTENSION),
                objectMapper.writeValueAsBytes(metadataMap)
        );
    }
}
