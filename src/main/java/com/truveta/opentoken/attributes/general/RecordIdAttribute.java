/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.general;

import java.util.List;

import com.truveta.opentoken.attributes.BaseAttribute;

public class RecordIdAttribute extends BaseAttribute {

    private static final String NAME = "RecordId";
    private static final String[] ALIASES = new String[] { NAME, "Id" };

    protected RecordIdAttribute() {
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
