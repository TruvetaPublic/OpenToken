/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokentransformer;

/**
 * A <code>No Operation</code> token transformer. No transformation is
 * applied whatsoever.
 */
public class NoOperationTokenTransformer implements TokenTransformer {

    /**
     * No operation token transformer.
     * <p>
     * Does not transform the token in any ways.
     */
    @Override
    public String transform(String token) {
        return token;
    }
}
