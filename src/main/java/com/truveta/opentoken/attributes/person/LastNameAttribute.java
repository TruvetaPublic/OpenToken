/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.utilities.AttributeUtilities;
import com.truveta.opentoken.attributes.validation.NotInValidator;

/**
 * Represents the last name of a person.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * last name fields. It recognizes "LastName" and "Surname" as valid aliases for
 * this attribute type.
 * 
 * The attribute performs no normalization on input values, returning them
 * unchanged.
 */
public class LastNameAttribute extends BaseAttribute {

    private static final String NAME = "LastName";
    private static final String[] ALIASES = new String[] { NAME, "Surname" };

    public LastNameAttribute() {
        super(List.of(
                new NotInValidator(
                        AttributeUtilities.COMMON_PLACEHOLDER_NAMES)));
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
        String normalizedValue = AttributeUtilities.normalizeDiacritics(value);

        String valueWithoutSuffix = AttributeUtilities.GENERATIONAL_SUFFIX_PATTERN.matcher(normalizedValue)
                .replaceAll("");

        // if the generational suffix removal doesn't result in an empty string,
        // continue with the value without suffix, otherwise use the value with suffix
        // as last name
        if (!valueWithoutSuffix.isEmpty()) {
            normalizedValue = valueWithoutSuffix;
        }

        // remove generational suffix

        normalizedValue = AttributeUtilities.GENERATIONAL_SUFFIX_PATTERN.matcher(normalizedValue).replaceAll("");

        // remove dashes, spaces and other non-alphanumeric characters
        normalizedValue = AttributeUtilities.NON_ALPHABETIC_PATTERN.matcher(normalizedValue).replaceAll("");

        return normalizedValue;
    }
}
