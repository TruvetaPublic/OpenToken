/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.validation;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;

import lombok.NoArgsConstructor;

/**
 * A Validator that asserts that the birth date value is within
 * an acceptable range (between 1910/1/1 and today).
 * 
 * This validator checks that birth dates are:
 * - Not before January 1, 1910
 * - Not after today's date
 * 
 * If the date is outside this range, the validation fails.
 */
@NoArgsConstructor
public final class BirthDateRangeValidator implements SerializableAttributeValidator {

    private static final long serialVersionUID = 1L;

    // Supported date formats for parsing
    private static final String[] POSSIBLE_INPUT_FORMATS = new String[] {
            "yyyy-MM-dd", "yyyy/MM/dd", "MM/dd/yyyy",
            "MM-dd-yyyy", "dd.MM.yyyy"
    };

    /**
     * Validates that the birth date value is within acceptable range.
     * 
     * @param value the birth date string to validate
     * @return true if the date is within acceptable range (1910/1/1 to today),
     *         false otherwise
     */
    @Override
    public boolean eval(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            // Parse the date using various supported formats
            Date date = DateUtils.parseDateStrictly(value, Locale.ENGLISH, POSSIBLE_INPUT_FORMATS);

            // Convert Date to LocalDate
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            // Check if date is not before 1910/1/1 and not after today
            return !localDate.isBefore(LocalDate.of(1910, 1, 1)) && !localDate.isAfter(LocalDate.now());

        } catch (ParseException e) {
            // If the date cannot be parsed, it's invalid
            return false;
        }
    }
}
