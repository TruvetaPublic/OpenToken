/**
 * Copyright (c) Truveta. All rights reserved.
 * Represents a writer for person attributes.
 * This interface provides a method to write person attributes to a file.
 */
package com.truveta.opentoken.io;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A generic interface for the person attributes writer.
 */
public interface PersonAttributesWriter {
    /**
     * Writes person attributes from a given input source.
     * <p>
     * Example person attribute map:
     * <code>
     * {
     *   RecordId: 2ea45fee-90c3-494a-a503-36022c9e1281,
     *   RuleId: T1,
     *   Token: 812f4cec4ff577e90f6a0dce95361be59b3208892ffe46ce970649e35c1e923d
     * }
     * </code>
     * @param data a list of person attributes map.
     * @throws java.io.IOException errors encountered while writing to the output data source.
     */
    void writeAttributes(List<Map<String, String>> data) throws IOException;
}
