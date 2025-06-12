/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;
import java.util.regex.Pattern;
import java.util.Set;

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
                        Set.of(
                                "Unknown", // Placeholder for unknown first names
                                "N/A", // Not applicable
                                "None", // No first name provided
                                "Test", // Commonly used in testing scenarios
                                "Sample", // Sample data placeholder
                                "Donor", // Placeholder for donor records
                                "Patient", // Placeholder for patient records
                                "Automation Test", // Placeholder for automation tests
                                "Automationtest", // Another variation of automation test
                                "patient not found", // Placeholder for cases where patient data is not found
                                "patientnotfound", // Another variation of patient not found
                                "<masked>", // Placeholder for masked data
                                "Anonymous", // Placeholder for anonymous records
                                "zzztrash", // Placeholder for test or trash data
                                "Missing", // Placeholder for missing data
                                "Unavailable", // Placeholder for unavailable data
                                "Not Available", // Placeholder for data not available
                                "NotAvailable" // Placeholder for data not available (no spaces)
                        ))));
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

        String withoutSuffix = AttributeUtilities.GENERATIONAL_SUFFIX_PATTERN.matcher(normalized).replaceAll("");

        // if the generational suffix removal results in an empty string, use the
        // original value
        if (!withoutSuffix.isEmpty()) {
            normalized = withoutSuffix;
        }

        // trim trailing periods
        // remove trailing periods and middle initials
        normalized = TRAILING_PERIOD_AND_INITIAL_PATTERN.matcher(normalized).replaceAll("");

        // remove dashes, spaces and other non-alphanumeric characters
        normalized = AttributeUtilities.NON_ALPHABETIC_PATTERN.matcher(normalized).replaceAll("");

        return normalized;
    }
}
