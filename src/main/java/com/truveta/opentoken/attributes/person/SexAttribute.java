/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;

public class SexAttribute extends BaseAttribute {

    private static final String NAME = "Sex";
    private static final String[] ALIASES = new String[] { NAME, "Gender" };

    private static final String VALIDATE_REGEX = "^(M(ale)?|F(emale)?)$";

    public SexAttribute() {
        super(
                List.of(
                        new RegexValidator(NAME, VALIDATE_REGEX)));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }
}
