/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

/**
 * A generic interface for attribute validation.
 */
public interface AttributeValidator {
    /**
     * Validates the attribute name and value.
     * 
     * @param name  the attribute name.
     * @param value the attribute value.
     * 
     * @return <code>true</code> if the attribute is valid; <code>false</code>
     *         otherwise.
     */
    boolean eval(String name, String value);
}