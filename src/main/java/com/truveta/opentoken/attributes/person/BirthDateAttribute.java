/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;

public class BirthDateAttribute extends BaseAttribute {

    private static final String NAME = "BirthDate";
    private static final String[] ALIASES = new String[] { NAME };
    private static final String BIRTHDATE_REGEX = "^((\\d{4}[-/]\\d{2}[-/]\\d{2})|(\\d{2}[-/.]\\d{2}[-/.]\\d{4}))$";
    private static final String NORMALIZED_FORMAT = "yyyy-MM-dd";
    private static final String[] POSSIBLE_INPUT_FORMATS = new String[] {
            NORMALIZED_FORMAT, "yyyy/MM/dd", "MM/dd/yyyy",
            "MM-dd-yyyy", "dd.MM.yyyy" };
    private SimpleDateFormat normalizedDateFormat;

    protected BirthDateAttribute() {
        super(List.of(
                new RegexValidator(NAME, BIRTHDATE_REGEX)));

        this.normalizedDateFormat = new SimpleDateFormat(NORMALIZED_FORMAT);
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
            Date date = DateUtils.parseDate(value, POSSIBLE_INPUT_FORMATS);
            return this.normalizedDateFormat.format(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }

}
