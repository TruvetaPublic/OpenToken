/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens.tokenizer;

import com.truveta.opentoken.tokens.Token;

/**
 * A no-operation tokenizer that returns the input value unchanged.
 * 
 * <p>
 * This tokenizer implementation passes through the input value without
 * any transformation. It's useful for scenarios where tokenization
 * is not required but a Tokenizer instance is expected.
 * </p>
 */
public final class NoOpTokenizer implements Tokenizer {

    private static final long serialVersionUID = 1L;

    /**
     * The empty token value.
     * <p>
     * This is the value returned when the input value is <code>null</code> or blank.
     */
    public static final String EMPTY = Token.BLANK;

    /**
     * Returns the input value unchanged.
     * 
     * @param value the value to tokenize.
     * 
     * @return the same value that was provided as input. If the value is 
     *         <code>null</code> or blank, {@link #EMPTY EMPTY} is returned.
     * 
     * @throws Exception this implementation never throws exceptions, but the
     *                   signature matches the Tokenizer interface.
     */
    @Override
    public String tokenize(String value) throws Exception {
        if (value == null || value.isBlank()) {
            return EMPTY;
        }
        return value;
    }
}
