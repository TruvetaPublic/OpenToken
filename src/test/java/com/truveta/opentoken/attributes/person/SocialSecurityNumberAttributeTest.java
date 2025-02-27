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

class SocialSecurityNumberAttributeTest {
    private SocialSecurityNumberAttribute ssnAttribute;

    @BeforeEach
    void setUp() {
        ssnAttribute = new SocialSecurityNumberAttribute();
    }

    @Test
    void getName_ShouldReturnSocialSecurityNumber() {
        assertEquals("SocialSecurityNumber", ssnAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnSocialSecurityNumberAndNationalIdentificationNumber() {
        String[] expectedAliases = { "SocialSecurityNumber", "NationalIdentificationNumber" };
        assertArrayEquals(expectedAliases, ssnAttribute.getAliases());
    }

    @Test
    void normalize_ShouldFormatWithDashes() {
        assertEquals("123-45-6789", ssnAttribute.normalize("123456789"), "Should format without dashes");
        assertEquals("123-45-6789", ssnAttribute.normalize("123-45-6789"), "Should format with dashes");
    }

    @Test
    void validate_ShouldReturnTrueForValidSSNs() {
        assertTrue(ssnAttribute.validate("123-45-6789"), "Valid SSN should be allowed");
        assertTrue(ssnAttribute.validate("123456789"), "Valid SSN without dashes should be allowed");
        assertTrue(ssnAttribute.validate("001-23-4567"), "Valid SSN with leading zeros should be allowed");
    }

    @Test
    void validate_ShouldReturnFalseForInvalidSSNs() {
        assertFalse(ssnAttribute.validate(null), "Null value should not be allowed");
        assertFalse(ssnAttribute.validate(""), "Empty value should not be allowed");
        assertFalse(ssnAttribute.validate("12345"), "Short SSN should not be allowed");
        assertFalse(ssnAttribute.validate("1234567890"), "Long SSN should not be allowed");
        assertFalse(ssnAttribute.validate("000-00-0000"), "Invalid sequence should not be allowed");
        assertFalse(ssnAttribute.validate("666-00-0000"), "SSN starting with 666 should not be allowed");
        assertFalse(ssnAttribute.validate("123-11-0000"), "All zeros in last group should not be allowed");
        assertFalse(ssnAttribute.validate("123-00-1234"), "All zeros in middle group should not be allowed");
        assertFalse(ssnAttribute.validate("000-45-6789"), "All zeros in first group should not be allowed");
        assertFalse(ssnAttribute.validate("ABCDEFGHI"), "Non-numeric should not be allowed");
    }
}
