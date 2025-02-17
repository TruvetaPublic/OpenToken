package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostalCodeAttributeTest {
    private PostalCodeAttribute postalCodeAttribute;

    @BeforeEach
    void setUp() {
        postalCodeAttribute = new PostalCodeAttribute();
    }

    @Test
    void getName_ShouldReturnPostalCode() {
        assertEquals("PostalCode", postalCodeAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnPostalCodeAndZipCode() {
        String[] expectedAliases = { "PostalCode", "ZipCode" };
        assertArrayEquals(expectedAliases, postalCodeAttribute.getAliases());
    }

    @Test
    void normalize_ShouldReturnFirst5Digits() {
        assertEquals("12345", postalCodeAttribute.normalize("12345-6789"));
        assertEquals("12345", postalCodeAttribute.normalize("12345"));
    }

    @Test
    void validate_ShouldReturnTrueForValidPostalCodes() {
        assertTrue(postalCodeAttribute.validate("12345"));
        assertTrue(postalCodeAttribute.validate("12345-6789"));
        assertTrue(postalCodeAttribute.validate("01234-6789"));
    }

    @Test
    void validate_ShouldReturnFalseForInvalidPostalCodes() {
        assertFalse(postalCodeAttribute.validate(null), "Null value should not be allowed");
        assertFalse(postalCodeAttribute.validate(""), "Empty value should not be allowed");
        assertFalse(postalCodeAttribute.validate("1234"), "Short postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("123456"), "Long postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("1234-5678"), "Invalid format should not be allowed");
        assertFalse(postalCodeAttribute.validate("abcde"), "Non-numeric should not be allowed");
    }
}
