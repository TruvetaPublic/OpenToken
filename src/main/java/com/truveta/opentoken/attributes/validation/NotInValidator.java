/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.validation;

import java.util.List;

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
    private String attributeName;
    @NotNull
    private List<String> invalidValues;

    /**
     * Validates that the attribute value is not in the list of invalid values.
     */
    @Override
    public boolean eval(String name, String value) {
        return (!name.equals(attributeName)) || !invalidValues.contains(value);
    }

}