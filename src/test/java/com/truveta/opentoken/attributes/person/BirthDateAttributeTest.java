/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BirthDateAttributeTest {

    private BirthDateAttribute birthDateAttribute;

    @BeforeEach
    void setUp() {
        birthDateAttribute = new BirthDateAttribute();
    }

    @Test
    void getName_ShouldReturnBirthDate() {
        assertEquals("BirthDate", birthDateAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnBirthDateAlias() {
        assertArrayEquals(new String[] { "BirthDate" }, birthDateAttribute.getAliases());
    }

    @Test
    void normalize_ValidDateFormats_ShouldNormalizeToYYYYMMDD() {
        assertEquals("2023-10-26", birthDateAttribute.normalize("2023-10-26"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("2023/10/26"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("10/26/2023"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("10-26-2023"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("26.10.2023"));
    }

    @Test
    void normalize_InvalidDateFormat_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            birthDateAttribute.normalize("20231026");
        });
        assertEquals("Invalid date format: 20231026", exception.getMessage());
    }

    @Test
    void validate_ValidDate_ShouldReturnTrue() {
        assertTrue(birthDateAttribute.validate("2023-10-26"));
    }
}