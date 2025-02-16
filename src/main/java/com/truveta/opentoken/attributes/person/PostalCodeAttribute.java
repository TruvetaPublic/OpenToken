/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;

public class PostalCodeAttribute extends BaseAttribute {

    private static final String NAME = "PostalCode";
    private static final String[] ALIASES = new String[] { NAME, "ZipCode" };
    private static final String POSTAL_CODE_REGEX = "^\\d{5}(-\\d{4})?$";

    protected PostalCodeAttribute() {
        super(List.of(new RegexValidator(NAME, POSTAL_CODE_REGEX)));
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
        return value;
    }

}
