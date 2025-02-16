/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;

public class BirthDateAttribute extends BaseAttribute {

    private static final String NAME = "BirthDate";
    private static final String[] ALIASES = new String[] { NAME };
    private static final String BIRTHDATE_REGEX = "^(\\d{4}[-/]\\d{2}[-/]\\d{2})([T\\s]\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,3})?)?$";

    protected BirthDateAttribute() {
        super(List.of(
                new RegexValidator(NAME, BIRTHDATE_REGEX)));
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
            // convert value to Date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(value);
        } catch (Exception e) {
            // ignore
        }
        return value; // "yyyy-MM-dd"
    }

}
