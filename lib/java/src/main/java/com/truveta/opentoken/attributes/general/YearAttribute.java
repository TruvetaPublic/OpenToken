/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.general;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;
import com.truveta.opentoken.attributes.validation.SerializableAttributeValidator;

/**
 * Represents a generic year attribute.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * year fields. It recognizes "Year" as a valid alias for this attribute type.
 * 
 * The attribute performs normalization on input values by trimming whitespace
 * and validates that the year is a 4-digit year format.
 */
public class YearAttribute extends BaseAttribute {

    private static final String NAME = "Year";
    private static final String[] ALIASES = new String[] { NAME };

    /**
     * Regular expression pattern for validating year format.
     * 
     * This regex validates that the input is:
     * - A 4-digit year
     * - Optionally with leading/trailing whitespace
     */
    private static final String YEAR_REGEX = "^\\s*\\d{4}\\s*$";
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{4}");

    /**
     * Constructor for YearAttribute with no additional validators.
     */
    public YearAttribute() {
        this(null);
    }

    /**
     * Protected constructor allowing subclasses to add additional validators.
     * 
     * @param additionalValidators list of additional validators to apply, or null
     */
    protected YearAttribute(List<SerializableAttributeValidator> additionalValidators) {
        super(buildValidators(additionalValidators));
    }

    /**
     * Builds the list of validators by combining the base regex validator with any additional validators.
     * 
     * @param additionalValidators optional additional validators
     * @return combined list of validators
     */
    private static List<SerializableAttributeValidator> buildValidators(
            List<SerializableAttributeValidator> additionalValidators) {
        List<SerializableAttributeValidator> validators = new ArrayList<>();
        validators.add(new RegexValidator(YEAR_REGEX));
        if (additionalValidators != null) {
            validators.addAll(additionalValidators);
        }
        return validators;
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
            throw new IllegalArgumentException("Year value cannot be null");
        }

        // Trim whitespace and validate it's a number
        String trimmed = value.trim();

        // Check if it matches the 4-digit year format using pre-compiled Pattern
        if (!YEAR_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid year format: " + value);
        }

        try {
            int year = Integer.parseInt(trimmed);
            // Return the year as a string
            return String.valueOf(year);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid year format: " + value);
        }
    }

}
