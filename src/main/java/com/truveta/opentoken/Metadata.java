/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import java.util.LinkedHashMap;
import java.util.Map;

public class Metadata {

    // Metadata keys
    public static final String PLATFORM = "Platform";
    public static final String JAVA_VERSION = "JavaVersion";
    public static final String OPENTOKEN_VERSION = "OpenTokenVersion";
    public static final String OUTPUT_FORMAT = "OutputFormat";

    // Metadata values
    public static final String PLATFORM_JAVA = "Java";
    public static final String METADATA_FILE_EXTENSION = ".metadata.json";
    public static final String SYSTEM_JAVA_VERSION = System.getProperty("java.version");

    public static final String DEFAULT_VERSION = "1.9.0";

    // Output format values
    public static final String OUTPUT_FORMAT_JSON = "JSON";
    public static final String OUTPUT_FORMAT_CSV = "CSV";
    public static final String OUTPUT_FORMAT_PARQUET = "Parquet";

    Map<String, Object> metadata;

    public Metadata() {
        metadata = new LinkedHashMap<>();
    }

    public Map<String, Object> initializeMetadata() {
        metadata.put(JAVA_VERSION, SYSTEM_JAVA_VERSION);
        metadata.put(PLATFORM, PLATFORM_JAVA);
        metadata.put(OPENTOKEN_VERSION, DEFAULT_VERSION);
        return metadata;
    }
}
