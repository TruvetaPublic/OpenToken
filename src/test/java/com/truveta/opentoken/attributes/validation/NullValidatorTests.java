/**
 * Copyright (c) Truveta. All rights reserved.
 */

package com.truveta.opentoken.attributes.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.truveta.opentoken.tokens.BaseTokenDefinition;

public class NullValidatorTests {

    @Test
    public void invalidTests() {
        var validator = new NullValidator("*");

        var result = validator.eval(BaseTokenDefinition.FIRST_NAME, null);
        Assertions.assertEquals(false, result);
        result = validator.eval(BaseTokenDefinition.LAST_NAME, "");
        Assertions.assertEquals(false, result);
        result = validator.eval(BaseTokenDefinition.BIRTH_DATE, "   ");
        Assertions.assertEquals(false, result);
        result = validator.eval(BaseTokenDefinition.POSTAL_CODE, "\t");
        Assertions.assertEquals(false, result);
        result = validator.eval(BaseTokenDefinition.FIRST_NAME, "\n");
        Assertions.assertEquals(false, result);
    }
}