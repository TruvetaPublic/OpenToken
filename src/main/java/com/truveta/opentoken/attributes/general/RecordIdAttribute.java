/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.general;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;

/**
 * Represents an attribute for handling record identifiers.
 * 
 * This class extends BaseAttribute and provides functionality for working with
 * identifier fields. It recognizes "RecordId" and "Id" as valid aliases for
 * this attribute type.
 * 
 * The attribute performs no normalization on input values, returning them
 * unchanged.
 */
public class RecordIdAttribute extends BaseAttribute {

    private static final String NAME = "RecordId";
    private static final String[] ALIASES = new String[] { NAME, "Id" };

    public RecordIdAttribute() {
        super(List.of());
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
