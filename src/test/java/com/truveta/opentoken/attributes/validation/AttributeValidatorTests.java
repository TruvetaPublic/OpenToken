/**
 * Copyright (c) Truveta. All rights reserved.
 */

package com.truveta.opentoken.attributes.validation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AttributeValidatorTests {

    @ParameterizedTest
    @CsvSource({
            "SocialSecurityNumber, 123-45-6789, true",
            "RecordId, 10, true",
            "SocialSecurityNumber, 000-32-1123, false",
            "SocialSecurityNumber, 123456789, true",
            "SocialSecurityNumber, 000-00-0000, false",
            "SocialSecurityNumber, 111-11-1111, false",
            "SocialSecurityNumber, 222-22-2222, false",
            "SocialSecurityNumber, 333-33-3333, false",
            "SocialSecurityNumber, 444-44-4444, false",
            "SocialSecurityNumber, 555-55-5555, false",
            "SocialSecurityNumber, 666-66-6666, false",
            "SocialSecurityNumber, 777-77-7777, false",
            "SocialSecurityNumber, 888-88-8888, false",
            "SocialSecurityNumber, 999-99-9999, false",
            "LastName, , false",
            "PostalCode, 98033, true",
            "PostalCode, 9803, false",
            "BirthDate, 01/01/2000, true",
            "Gender, Male, true",
            "Gender, male, false",
            "Gender, M, false"

    })
    public void eval_Attribute_Validation_Works(String attribute, String value, boolean expectedResult) {
        var validationRules = new ValidationRules().getValidationRules();

        var result = true;

        for (AttributeValidator validator : validationRules) {
            var currResult = validator.eval(attribute, value);
            if (!currResult)
                result = false;
        }

        Assertions.assertEquals(expectedResult, result);

    }
}