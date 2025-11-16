/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.processor;

/**
 * Shared constants for token processing.
 */
public final class TokenConstants {
    
    private TokenConstants() {
        // Utility class, prevent instantiation
    }

    /** Column name for the token value. */
    public static final String TOKEN = "Token";
    
    /** Column name for the rule ID. */
    public static final String RULE_ID = "RuleId";
    
    /** Column name for the record ID. */
    public static final String RECORD_ID = "RecordId";
}
