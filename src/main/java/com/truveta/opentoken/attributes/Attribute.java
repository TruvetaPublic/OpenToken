/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes;

public interface Attribute {

    String getName();

    String[] getAliases();

    String normalize(String value);

    boolean validate(String value);
}
