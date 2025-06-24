/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;

/**
 * Represents an assigned sex of a person.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * these type of fields. It recognizes "Sex" or "Gender" as valid aliases for
 * this attribute type.
 * 
 * The attribute performs normalization on input values, converting them to a
 * standard format (M or F).
 */
public class SexAttribute extends BaseAttribute {

    private static final String NAME = "Sex";
    private static final String[] ALIASES = new String[] { NAME, "Gender" };

    private static final String VALIDATE_REGEX = "^([Mm](ale)?|[Ff](emale)?)$";

    public SexAttribute() {
        super(
                List.of(
                        new RegexValidator(VALIDATE_REGEX)));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String normalize(String value) {
        switch (value.charAt(0)) {
            case 'M':
            case 'm':
                return "Male";
            case 'F':
            case 'f':
                return "Female";
            default:
                return null;
        }
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }
}
