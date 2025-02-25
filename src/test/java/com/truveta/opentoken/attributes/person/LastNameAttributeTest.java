/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LastNameAttributeTest {

    private LastNameAttribute lastNameAttribute;

    @BeforeEach
    void setUp() {
        lastNameAttribute = new LastNameAttribute();
    }

    @Test
    void getName_ShouldReturnLastName() {
        assertEquals("LastName", lastNameAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnLastNameAndSurname() {
        String[] expectedAliases = { "LastName", "Surname" };
        String[] actualAliases = lastNameAttribute.getAliases();
        assertArrayEquals(expectedAliases, actualAliases);
    }

    @Test
    void normalize_ShouldReturnUnchangedValue() {
        String input = "Doe";
        assertEquals(input, lastNameAttribute.normalize(input));
    }

    @Test
    void validate_ShouldReturnTrueForAnyNonEmptyString() {
        assertTrue(lastNameAttribute.validate("Doe"));
        assertTrue(lastNameAttribute.validate("Smith-Jones"));
        assertTrue(lastNameAttribute.validate("D"));
        assertTrue(lastNameAttribute.validate("test123"));
    }

    @Test
    void validate_ShouldReturnFalseForNullOrEmptyString() {
        assertFalse(lastNameAttribute.validate(null), "Null value should not be allowed");
        assertFalse(lastNameAttribute.validate(""), "Empty value should not be allowed");
        assertTrue(lastNameAttribute.validate("test123"), "Non-empty value should be allowed");
    }
}
