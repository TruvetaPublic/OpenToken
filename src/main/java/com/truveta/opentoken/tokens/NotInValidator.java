/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import java.util.Arrays;
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
    private String[] invalidValues;

    /**
     * Validates that the attribute value is not in the list of invalid values.
     */
    @Override
    public boolean eval(String name, String value) {
        return (!name.equals(attributeName)) || !Arrays.asList(invalidValues).contains(value);
    }

}