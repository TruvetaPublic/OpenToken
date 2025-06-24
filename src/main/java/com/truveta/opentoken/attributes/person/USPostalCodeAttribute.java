/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.utilities.AttributeUtilities;
import com.truveta.opentoken.attributes.validation.NotStartsWithValidator;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents US postal codes (ZIP codes).
 * 
 * This class handles validation and normalization of US ZIP codes, supporting
 * both 5-digit format (12345) and 5+4 format (12345-6789).
 */
public class USPostalCodeAttribute extends BaseAttribute {

    private static final String NAME = "USPostalCode";
    private static final String[] ALIASES = new String[] { NAME, "USZipCode" };
    private static final String US_ZIP_REGEX = "^\\s*(\\d{5}(-\\d{4})?|\\d{9})\\s*$";

    public USPostalCodeAttribute() {
        super(List.of(
                new RegexValidator(US_ZIP_REGEX),
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

    /**
     * Normalizes a US ZIP code to standard 5-digit format.
     * 
     * For US ZIP codes: returns the first 5 digits (e.g., "12345-6789" becomes
     * "12345")
     * If the input value is null or doesn't match US ZIP pattern, the original
     * trimmed value is returned.
     *
     * @param value The ZIP code to normalize
     * @return The normalized ZIP code or the original trimmed value if
     *         normalization
     *         isn't applicable
     */
    @Override
    public String normalize(String value) {
        if (value == null) {
            return value;
        }

        String trimmed = value.trim().replaceAll(AttributeUtilities.WHITESPACE.pattern(), StringUtils.EMPTY);

        // Check if it's a US ZIP code (5 digits, 5+4 with dash, or 9 digits without dash)
        if (trimmed.matches("\\d{5}(-?\\d{4})?") || trimmed.matches("\\d{9}")) {
            return trimmed.substring(0, 5);
        }

        // For values that don't match US ZIP patterns, return trimmed original
        return value.trim();
    }
}