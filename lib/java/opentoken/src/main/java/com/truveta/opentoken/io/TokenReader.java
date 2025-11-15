/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io;

import java.util.Iterator;
import java.util.Map;

/**
 * A generic interface for a streaming token reader.
 * <p>
 * Reads encrypted or decrypted tokens from an input source.
 */
public interface TokenReader extends Iterator<Map<String, String>>, AutoCloseable {

    /**
     * Retrieve the next token row from an input source.
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
     * @return a token map containing RuleId, Token, and RecordId.
     */
    @Override
    Map<String, String> next();
}
