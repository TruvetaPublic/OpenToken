/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

public final class Const {

    // Private constructor to prevent instantiation
    private Const() {}

    // metadata keys
    public static final String javaVersion = "JavaVersion";
    public static final String openTokenVersion = "OpenTokenVersion";
    public static final String outputFormat = "OutputFormat";
    public static final String totalRows = "TotalRows";
    public static final String totalRowsWithInvalidAttributes = "TotalRowsWithInvalidAttributes";
    public static final String invalidAttributesByType = "InvalidAttributesByType";
    
    // metatadata values
    public static final String metadataFileExtension = ".metadata.json";
    public static final String systemJavaVersion = System.getProperty("java.version");
    public static final String outputFormatCSV = "CSV";
    public static final String outputFormatParquet = "Parquet";
}
