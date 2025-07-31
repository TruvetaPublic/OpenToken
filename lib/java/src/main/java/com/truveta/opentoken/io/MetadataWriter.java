/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for writing metadata to a specified output format.
 * Implementations of this interface should handle the specifics of writing
 * metadata in the desired format, such as JSON, CSV, or Parquet.
 * Each call to write will generate a new metadata file based on the input given.
 */
public interface MetadataWriter {

    /**
     * Writes metadata to the output destination.
     * 
     * @param metadataMap a map containing metadata key-value pairs.
     * @throws IOException if an error occurs while writing the metadata.
     *                     This method should handle the specifics of the output
     *                     format,
     *                     such as JSON, CSV, or Parquet.
     */
    void write(Map<String, Object> metadataMap) throws IOException;
}
