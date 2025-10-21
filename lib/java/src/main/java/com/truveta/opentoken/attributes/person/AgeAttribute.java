/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.AgeRangeValidator;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents the age attribute.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * age fields. It recognizes "Age" as the primary alias for this attribute type.
 * 
 * The attribute performs normalization on input values, trimming whitespace and
 * ensuring the value is a valid integer.
 * 
 * The attribute also performs validation on input values, ensuring they:
 * - Are numeric (integers only)
 * - Fall within an acceptable range (0-120)
 */
public class AgeAttribute extends BaseAttribute {

    private static final String NAME = "Age";
    private static final String[] ALIASES = new String[] { NAME };

    /**
     * Regular expression pattern for validating age format.
     * 
     * This regex validates that the input is:
     * - A positive integer (no decimals)
     * 
     * Note: Leading/trailing whitespace is allowed by the regex,
     * but is removed during normalization, not validation.
     */
    private static final String AGE_REGEX = "^\\s*\\d+\\s*$";

    public AgeAttribute() {
        super(List.of(
                new RegexValidator(AGE_REGEX),
                new AgeRangeValidator()));
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
        if (value == null) {
            throw new IllegalArgumentException("Age value cannot be null");
        }

        // Trim whitespace and validate it's a number
        String trimmed = value.trim();

        try {
            int age = Integer.parseInt(trimmed);
            // Return the trimmed integer as a string
            return String.valueOf(age);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid age format: " + value);
        }
    }

}
