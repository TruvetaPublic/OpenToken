/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;
import java.util.Set;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.NotInValidator;
import com.truveta.opentoken.attributes.validation.NotStartsWithValidator;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents the postal code of a person.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * postal code fields. It recognizes "PostalCode" and "ZipCode" as valid aliases
 * for this attribute type.
 * 
 * The attribute performs normalization on input values, converting them to a
 * standard format (5 digits).
 */
public class PostalCodeAttribute extends BaseAttribute {

    private static final String NAME = "PostalCode";
    private static final String[] ALIASES = new String[] { NAME, "ZipCode" };
    private static final String POSTAL_CODE_REGEX = "^\\d{5}(-\\d{4})?$";

    public PostalCodeAttribute() {
        super(List.of(
                new RegexValidator(POSTAL_CODE_REGEX),
                new NotStartsWithValidator(
                        Set.of(
                                "00000",
                                "11111",
                                "22222",
                                "33333",
                                "55555",
                                "66666",
                                "77777",
                                "88888", // Valid but assigned to the North Pole
                                "99999",
                                // Commonly used placeholders
                                "01234",
                                "12345",
                                "54321",
                                "98765"))));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }

    @Override
    public String normalize(String value) {
        return value.substring(0, 5);
    }

}
