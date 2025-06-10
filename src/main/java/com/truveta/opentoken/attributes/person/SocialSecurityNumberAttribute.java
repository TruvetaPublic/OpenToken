/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.NotInValidator;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents the social security number attribute.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * social security number fields. It recognizes "SocialSecurityNumber" and
 * "NationalIdentificationNumber" as valid aliases for this attribute type.
 * 
 * The attribute performs normalization on input values, converting them to a
 * standard format (xxx-xx-xxxx).
 * 
 * The attribute also performs validation on input values, ensuring they match
 * the following format:
 * - xxx-xx-xxxx
 * - xxxxxxxxx
 */
public class SocialSecurityNumberAttribute extends BaseAttribute {

    private static final String NAME = "SocialSecurityNumber";
    private static final String[] ALIASES = new String[] { NAME, "NationalIdentificationNumber" };
    private static final String DASH = "-";
    private static final String SSN_FORMAT = "%09d";
    private static final String WHITESPACE_REGEX = "\\s+";
    private static final String DECIMAL_POINT = ".";

    private static final int MIN_SSN_LENGTH = 7;
    private static final int SSN_LENGTH = 9;

    // Accepts SSNs in xxx-xx-xxxx or xxxxxxxxx format. Rejects: area 000/666/9xx,
    // group 00, serial 0000
    private static final String SSN_REGEX = "^(?!0{3})(?!6{3})[0-8]\\d{2}-?(?!0{2})\\d{2}-?(?!0{4})\\d{4}$";

    public SocialSecurityNumberAttribute() {
        super(List.of(
                new NotInValidator(
                        Set.of(
                                "111-11-1111",
                                "222-22-2222",
                                "333-33-3333",
                                "444-44-4444",
                                "555-55-5555",
                                "777-77-7777",
                                "888-88-8888")),
                new RegexValidator(SSN_REGEX)));
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
     * Normalize the social security number value. Remove any dashes and format the
     * value as xxx-xx-xxxx.
     * 
     * @param value the social security number value.
     */
    @Override
    public String normalize(String value) {

        if (value == null || value.isEmpty()) {
            return value;
        }

        // Remove any whitespace
        value = value.trim().replaceAll(WHITESPACE_REGEX, StringUtils.EMPTY);

        // Remove decimal point/separator and all following numbers if present
        // Remove the decimal portion only if it occurs after the 7th digit,
        // as a SSN interpreted as a number would need to be at least 7 digits long
        // (non-zero leading digits)
        int decimalIndex = value.indexOf(DECIMAL_POINT);
        if (decimalIndex != -1 && decimalIndex >= MIN_SSN_LENGTH) {
            value = value.substring(0, decimalIndex);
        }

        // Remove any dashes for now
        value = value.replace(DASH, StringUtils.EMPTY);

        value = padWithZeros(value);

        return formatWithDashes(value);
    }

    // If SSN is between 7-8 digits, pad with leading zeros to reach 9 digits
    // Examples:
    // "1234567" -> "001234567"
    // "12345678" -> "012345678"
    private String padWithZeros(String value) {
        if (value.length() >= MIN_SSN_LENGTH && value.length() < SSN_LENGTH) {
            value = String.format(SSN_FORMAT, Long.parseLong(value));
        }
        return value;
    }

    private String formatWithDashes(String value) {
        String areaNumber = value.substring(0, 3);
        String groupNumber = value.substring(3, 5);
        String serialNumber = value.substring(5);
        value = String.join(DASH,
                areaNumber,
                groupNumber,
                serialNumber);
        return value;
    }
}