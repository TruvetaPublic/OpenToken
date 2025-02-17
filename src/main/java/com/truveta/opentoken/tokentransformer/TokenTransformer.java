/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokentransformer;

/**
 * A generic interface for the token transformer.
 */
public interface TokenTransformer {
    /**
     * Transforms the token using a token transformation rule/strategy.
     * 
     * @param token the token to be transformed.
     * 
     * @return the transformed token.
     * 
     * @throws Exception error encountered while transforming the token.
     */
    String transform(String token) throws Exception;
}
