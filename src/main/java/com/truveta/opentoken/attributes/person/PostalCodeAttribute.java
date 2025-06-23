/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import java.util.List;

import com.truveta.opentoken.attributes.CombinedAttribute;
import com.truveta.opentoken.attributes.SerializableAttribute;

/**
 * Represents the postal code of a person.
 * 
 * This class combines US and Canadian postal code implementations to provide
 * functionality for working with postal code fields. It recognizes "PostalCode"
 * and "ZipCode" as valid aliases for this attribute type.
 * 
 * The attribute performs normalization on input values, converting them to a
 * standard format. Supports both US ZIP codes (5 digits) and Canadian postal
 * codes (A1A 1A1 format).
 */
public class PostalCodeAttribute extends CombinedAttribute {

    private static final String NAME = "PostalCode";
    private static final String[] ALIASES = new String[] { NAME, "ZipCode" };

    private final List<SerializableAttribute> implementations = List.of(
            new USPostalCodeAttribute(),
            new CanadianPostalCodeAttribute()
    );

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }

    @Override
    protected List<SerializableAttribute> getAttributeImplementations() {
        return implementations;
    }
}