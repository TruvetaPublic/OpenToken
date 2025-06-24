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
    
    // Default version
    private static final String DEFAULT_VERSION = "1.7.0";
    
    /**
     * Get the application version.
     * @return The application version from manifest, properties, or default
     */
    public static String getApplicationVersion() {
        // Try system property first
        String version = System.getProperty("app.version");
        if (version != null && !version.isEmpty()) {
            return version;
        }
        
        // Try Package API (this is the most reliable method when running from JAR)
        Package pkg = Const.class.getPackage();
        if (pkg != null) {
            version = pkg.getImplementationVersion();
            if (version != null && !version.isEmpty()) {
                return version;
            }
        }
        
        // Try reading from MANIFEST.MF directly
        try {
            java.util.jar.Manifest manifest = new java.util.jar.Manifest(
                Const.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
            version = manifest.getMainAttributes().getValue("Implementation-Version");
            if (version != null && !version.isEmpty()) {
                return version;
            }
        } catch (Exception e) {
            // Ignore and try next method
        }
        
        // Default fallback
        return DEFAULT_VERSION;
    }
}
