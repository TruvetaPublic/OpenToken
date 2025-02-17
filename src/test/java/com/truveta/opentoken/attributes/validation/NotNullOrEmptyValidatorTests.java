/**
* Copyright (c) Truveta. All rights reserved.
*/

package com.truveta.opentoken.attributes.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NotNullOrEmptyValidatorTests {

    @Test
    void invalidTests() {
        var validator = new NotNullOrEmptyValidator();

        var result = validator.eval(null);
        Assertions.assertEquals(false, result);
        result = validator.eval("");
        Assertions.assertEquals(false, result);
        result = validator.eval(" ");
        Assertions.assertEquals(false, result);
        result = validator.eval("\t");
        Assertions.assertEquals(false, result);
        result = validator.eval("\n");
        Assertions.assertEquals(false, result);
    }
}