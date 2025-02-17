package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FirstNameAttributeTest {

    private FirstNameAttribute firstNameAttribute;

    @BeforeEach
    void setUp() {
        firstNameAttribute = new FirstNameAttribute();
    }

    @Test
    void getName_ShouldReturnFirstName() {
        assertEquals("FirstName", firstNameAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnFirstNameAndGivenName() {
        String[] expectedAliases = { "FirstName", "GivenName" };
        String[] actualAliases = firstNameAttribute.getAliases();
        assertArrayEquals(expectedAliases, actualAliases);
    }

    @Test
    void normalize_ShouldReturnUnchangedValue() {
        String input = "John";
        assertEquals(input, firstNameAttribute.normalize(input));
    }

    @Test
    void validate_ShouldReturnTrueForAnyNonEmptyString() {
        assertTrue(firstNameAttribute.validate("John"));
        assertTrue(firstNameAttribute.validate("Jane Doe"));
        assertTrue(firstNameAttribute.validate("J"));
    }

    @Test
    void validate_ShouldReturnFalseForNullOrEmptyString() {
        assertFalse(firstNameAttribute.validate(null), "Null value should not be allowed");
        assertFalse(firstNameAttribute.validate(""), "Empty value should not be allowed");
        assertTrue(firstNameAttribute.validate("test123"), "Non-empty value should be allowed");
    }
}
