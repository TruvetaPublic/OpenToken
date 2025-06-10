/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
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
    private static final String POSTAL_CODE_REGEX = "^\\s*\\d{5}(-\\d{4})?\\s*$";

    public PostalCodeAttribute() {
        super(List.of(new RegexValidator(POSTAL_CODE_REGEX)));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }

    /**
     * Normalizes a postal code by taking the first 5 characters.
     * 
     * This method returns the first 5 characters of the input postal code string,
     * which corresponds to the standard 5-digit format for US ZIP codes.
     * If the input value is null or less than 5 characters in length,
     * the original value is returned unchanged.
     *
     * @param value The postal code to normalize
     * @return The normalized postal code (first 5 digits) or the original value if
     *         too short
     */
    @Override
    public String normalize(String value) {
        if (value == null) {
            return value;
        }
        value = value.trim();
        if (value.length() < 5) {
            return value; // Return original value if less than 5 characters
        }
        return value.substring(0, 5);
    }

}
