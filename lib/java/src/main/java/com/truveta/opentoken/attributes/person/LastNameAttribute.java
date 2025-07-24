/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.utilities.AttributeUtilities;
import com.truveta.opentoken.attributes.validation.NotInValidator;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents the last name of a person.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * last name fields. It recognizes "LastName" and "Surname" as valid aliases for
 * this attribute type.
 * 
 * The attribute normalizes values by removing diacritics, generational suffixes,
 * and non-alphabetic characters. It validates that names are either:
 * - Longer than 2 characters, or
 * - Exactly 2 characters containing at least one vowel (including names with two vowels), or
 * - The specific last name "Ng"
 */
public class LastNameAttribute extends BaseAttribute {

    private static final String NAME = "LastName";
    private static final String[] ALIASES = new String[] { NAME, "Surname" };

    /**
     * Regular expression pattern for validating last names.
     *
     * This pattern matches:
     *  - Any name with 3 or more characters (including spaces within)
     *  - 2-character names with at least one vowel (consonant+vowel, vowel+consonant, or two vowels)
     *  - The special case "Ng" (case-insensitive)
     *  - Allows optional leading and trailing whitespace
     *
     * Breakdown of the regex:
     *   ^\s*                                 Start of string, optional leading whitespace
     *   (?:                                  Start of non-capturing group:
     *     (?:.{3,})                          Any name with 3+ characters
     *     |                                  OR
     *     (?:[^aeiouAEIOU\s][aeiouAEIOU])    2-char: consonant + vowel (no spaces)
     *     |                                  OR
     *     (?:[aeiouAEIOU][^aeiouAEIOU\s])    2-char: vowel + consonant (no spaces)
     *     |                                  OR
     *     (?:[aeiouAEIOU]{2})                2-char: two vowels (no spaces)
     *     |                                  OR
     *     (?:[Nn][Gg])                       Special case: "Ng" (case-insensitive)
     *   )
     *   \s*$                                 Optional trailing whitespace, end of string
     */
    private static final @NotNull String LAST_NAME_REGEX = "^\\s*(?:(?:.{3,})|(?:[^aeiouAEIOU\\s][aeiouAEIOU])|(?:[aeiouAEIOU][^aeiouAEIOU\\s])|(?:[aeiouAEIOU]{2})|(?:[Nn][Gg]))\\s*$";

    public LastNameAttribute() {
        super(List.of(
                new NotInValidator(
                        AttributeUtilities.COMMON_PLACEHOLDER_NAMES),
                new RegexValidator(LAST_NAME_REGEX)));
    }

    @Override
    public boolean validate(String value) {
        if (value == null) {
            return false;
        }

        // Trim the value to check its actual content
        String trimmedValue = value.trim();

        // Reject single letters (even if surrounded by whitespace)
        if (trimmedValue.length() == 1) {
            return false;
        }

        // Continue with the regular validation
        return super.validate(value);
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
