/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.validation;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A Validator that asserts the value is <b>NOT</b> <code>null</code>
 * and blank.
 */
@AllArgsConstructor
@Getter
@Setter
public final class NullValidator implements AttributeValidator {

    @NotNull
    private String attributeName;

    /**
     * Validates that the attribute value is not <code>null</code> and blank.
     */
    @Override
    public boolean eval(String name, String value) {
        // * represents wildcard (we don't care what the attribute is, we just want to
        // see if it's null)
        return (!attributeName.equals("*") && !name.equals(attributeName)) || (value != null && !value.isBlank());
    }

}