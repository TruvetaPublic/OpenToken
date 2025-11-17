/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TokenConstantsTest {

    @Test
    void testTokenConstantsExist() {
        assertNotNull(TokenConstants.TOKEN);
        assertNotNull(TokenConstants.RULE_ID);
        assertNotNull(TokenConstants.RECORD_ID);
    }

    @Test
    void testTokenConstantsValues() {
        assertEquals("Token", TokenConstants.TOKEN);
        assertEquals("RuleId", TokenConstants.RULE_ID);
        assertEquals("RecordId", TokenConstants.RECORD_ID);
    }
}
