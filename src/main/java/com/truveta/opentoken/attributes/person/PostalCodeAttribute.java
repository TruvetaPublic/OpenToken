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
 * Represents the postal code of a person.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * postal code fields. It recognizes "PostalCode" and "ZipCode" as valid aliases
 * for this attribute type.
 * 
 * The attribute performs normalization on input values, converting them to a
 * standard format. Supports both US ZIP codes (5 digits) and Canadian postal
 * codes (A1A 1A1 format).
 */
public class PostalCodeAttribute extends BaseAttribute {

    private static final String NAME = "PostalCode";
    private static final String[] ALIASES = new String[] { NAME, "ZipCode" };
    private static final String US_ZIP_REGEX = "^\\s*\\d{5}(-\\d{4})?\\s*$";
    private static final String CANADIAN_POSTAL_REGEX = "^\\s*[A-Za-z]\\d[A-Za-z]\\s*\\d[A-Za-z]\\d\\s*$";
    private static final String POSTAL_CODE_REGEX = "(" + US_ZIP_REGEX + ")|(" + CANADIAN_POSTAL_REGEX + ")";

    public PostalCodeAttribute() {
        super(List.of(
                new RegexValidator(POSTAL_CODE_REGEX),
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
                                "98765",
                                // Canadian postal code placeholders
                                "A1A 1A1",
                                "K1A 0A6", // Valid but used for Canadian government
                                "H0H 0H0", // Santa Claus postal code
                                "X0X 0X0",
                                "Y0Y 0Y0",
                                "Z0Z 0Z0",
                                "A0A 0A0",
                                "B1B 1B1",
                                "C2C 2C2"))));
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
     * Normalizes a postal code to standard format.
     * 
     * For US ZIP codes: returns the first 5 digits (e.g., "12345-6789" becomes "12345")
     * For Canadian postal codes: returns uppercase format with space (e.g., "k1a0a6" becomes "K1A 0A6")
     * If the input value is null or doesn't match expected patterns, the original value is returned.
     *
     * @param value The postal code to normalize
     * @return The normalized postal code or the original value if normalization isn't applicable
     */
    @Override
    public String normalize(String value) {
        if (value == null) {
            return value;
        }
        
        String trimmed = value.trim().replaceAll(AttributeUtilities.WHITESPACE.pattern(), StringUtils.EMPTY);
        
        // Check if it's a US ZIP code (5 or 9 digits)
        if (trimmed.matches("\\d{5}(-?\\d{4})?")) {
            return trimmed.length() >= 5 ? trimmed.substring(0, 5) : trimmed;
        }
        
        // Check if it's a Canadian postal code (6 alphanumeric characters)
        if (trimmed.matches("[A-Za-z]\\d[A-Za-z]\\d[A-Za-z]\\d")) {
            String upper = trimmed.toUpperCase();
            return upper.substring(0, 3) + " " + upper.substring(3, 6);
        }
        
        // For values that are too short or don't match patterns, return as-is or truncate to 5
        if (trimmed.length() < 5) {
            return trimmed;
        }
        return trimmed.substring(0, 5);
    }

}