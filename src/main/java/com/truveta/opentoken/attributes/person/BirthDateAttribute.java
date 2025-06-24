/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.BirthDateRangeValidator;
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

    /**
     * Regular expression pattern for validating birth date formats.
     * 
     * This regex supports two formats:
     * 1. "YYYY-MM-DD" or "YYYY/MM/DD" - where the year is represented by 4 digits,
     *    followed by a hyphen or slash, then a 2-digit month, another hyphen or slash,
     *    and finally a 2-digit day.
     * 2. "MM-DD-YYYY" or "MM.DD.YYYY" or "MM/DD/YYYY" - where the month is represented
     *    by 2 digits, followed by a hyphen, dot, or slash, then a 2-digit day, and
     *    finally a 4-digit year.
     * 
     * This ensures that the input matches common date formats while allowing for
     * flexibility in the delimiter used (hyphen, slash, or dot).
     */
    private static final String BIRTHDATE_REGEX = "^((\\d{4}[-/]\\d{2}[-/]\\d{2})|(\\d{2}[-/.]\\d{2}[-/.]\\d{4}))$";

    private static final String NORMALIZED_FORMAT = "yyyy-MM-dd";
    private static final String[] POSSIBLE_INPUT_FORMATS = new String[] {
            NORMALIZED_FORMAT, "yyyy/MM/dd", "MM/dd/yyyy",
            "MM-dd-yyyy", "dd.MM.yyyy" };

    // Thread-safe date formatter
    private static final DateTimeFormatter NORMALIZED_DATE_FORMATTER = DateTimeFormatter.ofPattern(NORMALIZED_FORMAT);

    public BirthDateAttribute() {
        super(List.of(
                new RegexValidator(BIRTHDATE_REGEX),
                new BirthDateRangeValidator()));
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
            // We still need to use DateUtils to parse the various formats
            Date date = DateUtils.parseDateStrictly(value, Locale.ENGLISH, POSSIBLE_INPUT_FORMATS);

            // Convert Date to LocalDate and format using thread-safe DateTimeFormatter
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            // Format the LocalDate to the normalized format
            return NORMALIZED_DATE_FORMATTER.format(localDate);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

}
