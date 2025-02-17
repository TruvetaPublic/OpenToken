/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.validation;

import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A Validator that asserts that the attribute values is
 * <b>NOT IN</b> the list of invalid values.
 */
@AllArgsConstructor
@Getter
@Setter
public final class NotInValidator implements AttributeValidator {

    @NotNull
    private Set<String> invalidValues;

    /**
     * Validates that the attribute value is not in the list of invalid values.
     */
    @Override
    public boolean eval(String value) {
        return value != null && !invalidValues.contains(value);
    }

}