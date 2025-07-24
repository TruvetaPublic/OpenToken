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
 * Represents Canadian postal codes.
 * 
 * This class handles validation and normalization of Canadian postal codes,
 * supporting the A1A 1A1 format (letter-digit-letter space digit-letter-digit).
 */
public class CanadianPostalCodeAttribute extends BaseAttribute {

    private static final String NAME = "CanadianPostalCode";
    private static final String[] ALIASES = new String[] { NAME, "CanadianZipCode" };

    /**
     * Regular expression pattern for validating Canadian postal codes.
     *
     * The pattern matches Canadian postal codes in the format "A1A 1A1" or "A1A1A1",
     * where 'A' represents an uppercase or lowercase letter and '1' represents a digit.
     *
     * Breakdown:
     *   ^\\s*        - Allows optional leading whitespace.
     *   [A-Za-z]     - Matches a single letter (case-insensitive).
     *   \\d          - Matches a single digit.
     *   [A-Za-z]     - Matches a single letter (case-insensitive).
     *   \\s?         - Allows an optional space between the two segments.
     *   \\d          - Matches a single digit.
     *   [A-Za-z]     - Matches a single letter (case-insensitive).
     *   \\d          - Matches a single digit.
     *   \\s*$        - Allows optional trailing whitespace.
     *
     * This pattern ensures that the postal code follows the Canadian standard,
     * optionally surrounded by whitespace and with an optional space in the middle.
     */
    private static final String CANADIAN_POSTAL_REGEX = "^\\s*[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d\\s*$";

    private static final Set<String> INVALID_ZIP_CODES = Set.of(
            // Canadian postal code placeholders
            "A1A 1A1",
            "K1A 0A6", // Valid but used for Canadian government
            "H0H 0H0", // Santa Claus postal code
            "X0X 0X0",
            "Y0Y 0Y0",
            "Z0Z 0Z0",
            "A0A 0A0",
            "B1B 1B1",
            "C2C 2C2");

    public CanadianPostalCodeAttribute() {
        super(List.of(
                new RegexValidator(CANADIAN_POSTAL_REGEX),
                new NotStartsWithValidator(INVALID_ZIP_CODES)));
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
     * Normalizes a Canadian postal code to standard A1A 1A1 format.
     * 
     * For Canadian postal codes: returns uppercase format with space (e.g.,
     * "k1a0a6" becomes "K1A 0A6")
     * If the input value is null or doesn't match Canadian postal pattern, the
     * original
     * trimmed value is returned.
     *
     * @param value The postal code to normalize
     * @return The normalized postal code or the original trimmed value if
     *         normalization
     *         isn't applicable
     */
    @Override
    public String normalize(String value) {
        if (value == null) {
            return value;
        }

        String trimmed = value.trim().replaceAll(AttributeUtilities.WHITESPACE.pattern(), StringUtils.EMPTY);

        // Check if it's a Canadian postal code (6 alphanumeric characters)
        if (trimmed.matches("[A-Za-z]\\d[A-Za-z]\\d[A-Za-z]\\d")) {
            String upper = trimmed.toUpperCase();
            return upper.substring(0, 3) + " " + upper.substring(3, 6);
        }

        // For values that don't match Canadian postal patterns, return trimmed original
        return value.trim();
    }
}