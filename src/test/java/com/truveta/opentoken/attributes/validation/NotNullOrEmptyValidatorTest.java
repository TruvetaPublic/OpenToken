/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NotNullOrEmptyValidatorTest {

    @Test
    void validTests() {
        var validator = new NotNullOrEmptyValidator();

        var result = validator.eval("test");
        assertTrue(result);

        result = validator.eval("123");
        assertTrue(result);

        result = validator.eval("  test  ");
        assertTrue(result);

        result = validator.eval("special!@#$");
        assertTrue(result);

        result = validator.eval("multi\nline");
        assertTrue(result);
    }

    @Test
    void invalidTests() {
        var validator = new NotNullOrEmptyValidator();

        var result = validator.eval(null);
        assertFalse(result, "Null value should not be allowed");
        result = validator.eval("");
        assertFalse(result, "Empty value should not be allowed");
        result = validator.eval(" ");
        assertFalse(result, "Blank value should not be allowed");
        result = validator.eval("\t");
        assertFalse(result, "Tab value should not be allowed");
        result = validator.eval("\n");
        assertFalse(result, "Newline value should not be allowed");
        result = validator.eval("\r");
        assertFalse(result, "Carriage return value should not be allowed");
    }
}