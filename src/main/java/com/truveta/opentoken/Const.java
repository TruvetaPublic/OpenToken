/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

public final class Const {

    // Private constructor to prevent instantiation
    private Const() {}

    // Metadata keys
    public static final String JAVA_VERSION = "JavaVersion";
    public static final String PLATFORM = "Platform";
    public static final String OPENTOKEN_VERSION = "OpenTokenVersion";
    public static final String OUTPUT_FORMAT = "OutputFormat";
    public static final String TOTAL_ROWS = "TotalRows";
    public static final String TOTAL_ROWS_WITH_INVALID_ATTRIBUTES = "TotalRowsWithInvalidAttributes";
    public static final String INVALID_ATTRIBUTES_BY_TYPE = "InvalidAttributesByType";
    
    // Metadata values
    public static final String METADATA_OUTPUT_FILE = "target/output";
    public static final String PLATFORM_JAVA = "Java";
    public static final String METADATA_FILE_EXTENSION = ".metadata.json";
    public static final String SYSTEM_JAVA_VERSION = System.getProperty("java.version");
    
    // Output format values
    public static final String OUTPUT_FORMAT_JSON = "JSON";
    public static final String OUTPUT_FORMAT_CSV = "CSV";
    public static final String OUTPUT_FORMAT_PARQUET = "Parquet";
}
