// Copyright (c) Truveta. All rights reserved.
package com.truveta.opentoken.unit.tokens;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.truveta.opentoken.tokens.AttributeValidator;
import com.truveta.opentoken.tokens.BaseTokenDefinition;
import com.truveta.opentoken.tokens.NotInValidator;
import com.truveta.opentoken.tokens.NullValidator;
import com.truveta.opentoken.tokens.RegexValidator;
import com.truveta.opentoken.tokens.ValidationRules;

import java.util.List;

public class ValidationRulesTest {

    @Test
    public void testCreateValidationRules() {
        List<AttributeValidator> validationRules = new ValidationRules().getValidationRules();

        Assertions.assertEquals(5, validationRules.size());

        // Validate NullValidator
        AttributeValidator nullValidator = validationRules.get(0);
        Assertions.assertTrue(nullValidator instanceof NullValidator);
        Assertions.assertEquals("*", ((NullValidator) nullValidator).getAttributeName());

        // Validate NotInValidator
        AttributeValidator notInValidator = validationRules.get(1);
        Assertions.assertTrue(notInValidator instanceof NotInValidator);
        Assertions.assertEquals(BaseTokenDefinition.SOCIAL_SECURITY_NUMBER, ((NotInValidator) notInValidator).getAttributeName());
        Assertions.assertArrayEquals(new String[] {
                "000-00-0000",
                "111-11-1111",
                "222-22-2222",
                "333-33-3333",
                "444-44-4444",
                "555-55-5555",
                "666-66-6666",
                "777-77-7777",
                "888-88-8888",
                "999-99-9999"
        }, ((NotInValidator) notInValidator).getInvalidValues());

        // Validate RegexValidator for SocialSecurityNumber
        AttributeValidator ssnValidator = validationRules.get(2);
        Assertions.assertTrue(ssnValidator instanceof RegexValidator);
        Assertions.assertEquals(BaseTokenDefinition.SOCIAL_SECURITY_NUMBER, ((RegexValidator) ssnValidator).getAttributeName());
        Assertions.assertEquals("^(?!0{3})(?!6{3})[0-8]\\d{2}-(?!0{2})\\d{2}-(?!0{4})\\d{4}$", ((RegexValidator) ssnValidator).getPattern());

        // Validate RegexValidator for Gender
        AttributeValidator genderValidator = validationRules.get(3);
        Assertions.assertTrue(genderValidator instanceof RegexValidator);
        Assertions.assertEquals(BaseTokenDefinition.GENDER, ((RegexValidator) genderValidator).getAttributeName());
        Assertions.assertEquals("^(Male|Female)$", ((RegexValidator) genderValidator).getPattern());

        // Validate RegexValidator for PostalCode
        AttributeValidator postalCodeValidator = validationRules.get(4);
        Assertions.assertTrue(postalCodeValidator instanceof RegexValidator);
        Assertions.assertEquals(BaseTokenDefinition.POSTAL_CODE, ((RegexValidator) postalCodeValidator).getAttributeName());
        Assertions.assertEquals("^\\d{5}(-\\d{4})?$", ((RegexValidator) postalCodeValidator).getPattern());
    }
}