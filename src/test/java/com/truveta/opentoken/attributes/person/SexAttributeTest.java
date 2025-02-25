/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SexAttributeTest {
    private SexAttribute sexAttribute;

    @BeforeEach
    void setUp() {
        sexAttribute = new SexAttribute();
    }

    @Test
    void getName_ShouldReturnSex() {
        assertEquals("Sex", sexAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnSexAndGender() {
        String[] expectedAliases = { "Sex", "Gender" };
        assertArrayEquals(expectedAliases, sexAttribute.getAliases());
    }

    @Test
    void normalize_ShouldReturnMaleForMInput() {
        assertEquals("Male", sexAttribute.normalize("M"));
        assertEquals("Male", sexAttribute.normalize("Male"));
        assertEquals("Male", sexAttribute.normalize("m"));
        assertEquals("Male", sexAttribute.normalize("male"));
    }

    @Test
    void normalize_ShouldReturnFemaleForFInput() {
        assertEquals("Female", sexAttribute.normalize("F"));
        assertEquals("Female", sexAttribute.normalize("Female"));
        assertEquals("Female", sexAttribute.normalize("f"));
        assertEquals("Female", sexAttribute.normalize("female"));
    }

    @Test
    void normalize_ShouldReturnNullForInvalidInput() {
        assertNull(sexAttribute.normalize("X"), "Invalid value should return null");
        assertNull(sexAttribute.normalize("Other"), "Invalid value should return null");
    }

    @Test
    void validate_ShouldReturnTrueForValidValues() {
        assertTrue(sexAttribute.validate("M"));
        assertTrue(sexAttribute.validate("F"));
        assertTrue(sexAttribute.validate("Male"));
        assertTrue(sexAttribute.validate("Female"));
    }

    @Test
    void validate_ShouldReturnFalseForInvalidValues() {
        assertFalse(sexAttribute.validate("X"), "Invalid value should not be allowed");
        assertFalse(sexAttribute.validate("Other"), "Invalid value should not be allowed");
        assertFalse(sexAttribute.validate(""), "Empty value should not be allowed");
        assertFalse(sexAttribute.validate(null), "Null value should not be allowed");
    }
}
