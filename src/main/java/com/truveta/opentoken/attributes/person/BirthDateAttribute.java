/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents the birth date attribute.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * birth date fields. It recognizes "BirthDate" as a valid alias for this
 * attribute type.
 * 
 * The attribute performs normalization on input values, converting them to a
 * standard format (yyyy-MM-dd).
 * 
 * The attribute also performs validation on input values, ensuring they match
 * one of the following formats:
 * - yyyy-MM-dd
 * - yyyy/MM/dd
 * - MM/dd/yyyy
 * - MM-dd-yyyy
 * - dd.MM.yyyy
 */
public class BirthDateAttribute extends BaseAttribute {

    private static final String NAME = "BirthDate";
    private static final String[] ALIASES = new String[] { NAME };

    private static final String BIRTHDATE_REGEX = "^((\\d{4}[-/]\\d{2}[-/]\\d{2})|(\\d{2}[-/.]\\d{2}[-/.]\\d{4}))$";

    private static final String NORMALIZED_FORMAT = "yyyy-MM-dd";
    private static final String[] POSSIBLE_INPUT_FORMATS = new String[] {
            NORMALIZED_FORMAT, "yyyy/MM/dd", "MM/dd/yyyy",
            "MM-dd-yyyy", "dd.MM.yyyy" };

    public BirthDateAttribute() {
        super(List.of(
                new RegexValidator(BIRTHDATE_REGEX)));
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
        try {
            Date date = DateUtils.parseDateStrictly(value, Locale.ENGLISH, POSSIBLE_INPUT_FORMATS);
            SimpleDateFormat normalizedDateFormat = new SimpleDateFormat(NORMALIZED_FORMAT);
            normalizedDateFormat.setLenient(false);
            return normalizedDateFormat.format(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

}
