/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;
import java.util.regex.Pattern;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.utilities.AttributeUtilities;

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
    private static final Pattern SUFFIX_PATTERN = Pattern
            .compile("(?i)\\s+(jr\\.?|junior|sr\\.?|senior|I{1,3}|IV|V|VI{0,3}|IX|X|\\d+(st|nd|rd|th))$");

    public LastNameAttribute() {
        super(List.of());
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
        String normalized = AttributeUtilities.normalizeDiacritics(value);

        // remove generational suffix
        normalized = SUFFIX_PATTERN.matcher(normalized).replaceAll("");

        // remove dashes, spaces and other non-alphanumeric characters
        normalized = AttributeUtilities.NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll("");

        return normalized;
    }
}
