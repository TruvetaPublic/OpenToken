/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

import java.io.IOException;
import java.util.Map;

/**
 * A generic interface for the token writer.
 * <p>
 * Writes encrypted or decrypted tokens to an output target.
 */
public interface TokenWriter extends AutoCloseable {

    /**
     * Writes the provided token to an output target.
     * <p>
     * Example token map:
     * <code>
     * {
     *   RecordId: 2ea45fee-90c3-494a-a503-36022c9e1281,
     *   RuleId: T1,
     *   Token: 812f4cec4ff577e90f6a0dce95361be59b3208892ffe46ce970649e35c1e923d
     * }
     * </code>
     * 
     * @param data a map containing RuleId, Token, and RecordId.
     * @throws IOException errors encountered while writing to the output
     *                     data source.
     */
    void writeToken(Map<String, String> data) throws IOException;

}
