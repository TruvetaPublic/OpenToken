/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truveta.opentoken.Const;

public class MetadataWriter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private MetadataWriter() {}

    public static void writeMetadata(Map<String, String> metadataMap) throws IOException {
        
        Files.write(
                Paths.get("target/output" + Const.metadataFileExtension),
                objectMapper.writeValueAsBytes(metadataMap)
        );
    }
}
