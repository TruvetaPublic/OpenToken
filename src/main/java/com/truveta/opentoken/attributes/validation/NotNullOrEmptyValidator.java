/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.validation;

/**
 * A Validator that asserts the value is <b>NOT</b> <code>null</code>
 * and blank.
 */
public final class NotNullOrEmptyValidator implements AttributeValidator {

    /**
     * Validates that the attribute value is not <code>null</code> or blank.
     */
    @Override
    public boolean eval(String value) {
        return value != null && !value.isBlank();
    }

}