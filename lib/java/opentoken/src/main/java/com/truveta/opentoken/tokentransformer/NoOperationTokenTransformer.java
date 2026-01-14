/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokentransformer;

import com.truveta.opentoken.tokens.Token;

/**
 * A <code>No Operation</code> token transformer. No transformation is
 * applied whatsoever.
 */
public class NoOperationTokenTransformer implements TokenTransformer {
    private static final long serialVersionUID = 1L;

    /**
     * No operation token transformer.
     * <p>
     * Does not transform the token in any ways.
     */
    @Override
    public String transform(String token) {
        if (token == null || token.isBlank()) {
            token = Token.BLANK; // Return blank token for null or blank input
        }
        return token;
    }
}
