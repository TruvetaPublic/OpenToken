/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;
import java.util.regex.Pattern;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.utilities.AttributeUtilities;

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
    private static final Pattern TRAILING_PATTERN = Pattern.compile("\\s[^\\s]\\.?$");

    public FirstNameAttribute() {
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

        // remove common titles and title abbreviations
        String withoutTitle = TITLE_PATTERN.matcher(normalized).replaceAll("");

        // if the title removal results in an empty string, use the original value
        if (!withoutTitle.isEmpty()) {
            normalized = withoutTitle;
        }

        // trim trailing periods
        // remove trailing periods and middle initials
        normalized = TRAILING_PATTERN.matcher(normalized).replaceAll("");

        // remove dashes, spaces and other non-alphanumeric characters
        normalized = AttributeUtilities.NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll("");

        return normalized;
    }
}
