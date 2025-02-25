/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.general;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecordIdAttributeTest {

    private RecordIdAttribute recordIdAttribute;

    @BeforeEach
    void setUp() {
        recordIdAttribute = new RecordIdAttribute();
    }

    @Test
    void getName_ShouldReturnRecordId() {
        assertEquals("RecordId", recordIdAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnRecordIdAndId() {
        assertArrayEquals(new String[] { "RecordId", "Id" }, recordIdAttribute.getAliases());
    }

    @Test
    void normalize_ShouldReturnUnchangedValue() {
        String input = "test123";
        assertEquals(input, recordIdAttribute.normalize(input));
    }

    @Test
    void validate_ShouldNotAllowNullOrEmpty() {
        assertFalse(recordIdAttribute.validate(null), "Null value should not be allowed");
        assertFalse(recordIdAttribute.validate(""), "Empty value should not be allowed");
        assertTrue(recordIdAttribute.validate("test123"), "Non-empty value should be allowed");
    }
}
