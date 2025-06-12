/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;
import java.util.regex.Pattern;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.utilities.AttributeUtilities;
import com.truveta.opentoken.attributes.validation.NotInValidator;

/**
 * Represents the first name of a person.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * first name fields. It recognizes "FirstName" and "GivenName" as valid aliases
 * for this attribute type.
 * 
 * The attribute performs no normalization on input values, returning them
 * unchanged.
 */
public class FirstNameAttribute extends BaseAttribute {

    private static final String NAME = "FirstName";
    private static final String[] ALIASES = new String[] { NAME, "GivenName" };
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "(?i)^(mr|mrs|ms|miss|dr|prof|capt|sir|col|gen|cmdr|lt|rabbi|father|brother|sister|hon|honorable|reverend|rev|doctor)\\.?\\s+");

    // Pattern to match trailing periods and middle initials in names
    private static final Pattern TRAILING_PERIOD_AND_INITIAL_PATTERN = Pattern.compile("\\s[^\\s]\\.?$");

    public FirstNameAttribute() {
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

        // remove common titles and title abbreviations
        String valueWithoutTitle = TITLE_PATTERN.matcher(normalizedValue).replaceAll("");

        // if the title removal doesn't results in an empty string, use the title-less
        // value, otherwise use title as first name
        if (!valueWithoutTitle.isEmpty()) {
            normalizedValue = valueWithoutTitle;
        }

        String valueWithoutSuffix = AttributeUtilities.GENERATIONAL_SUFFIX_PATTERN.matcher(normalizedValue)
                .replaceAll("");

        // if the generational suffix removal doesn't result in an empty string,
        // continue with the value without suffix, otherwise use the value with suffix
        // as first name
        if (!valueWithoutSuffix.isEmpty()) {
            normalizedValue = valueWithoutSuffix;
        }

        // trim trailing periods
        // remove trailing periods and middle initials
        normalizedValue = TRAILING_PERIOD_AND_INITIAL_PATTERN.matcher(normalizedValue).replaceAll("");

        // remove dashes, spaces and other non-alphanumeric characters
        normalizedValue = AttributeUtilities.NON_ALPHABETIC_PATTERN.matcher(normalizedValue).replaceAll("");

        return normalizedValue;
    }
}
