/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A Validator that is designed for validating with regex expressions.
 */
@AllArgsConstructor
@Getter
@Setter
public final class RegexValidator implements AttributeValidator {

    @NotNull
    private String attributeName;
    @NotNull
    private String pattern;

    /**
     * Validates that the value matches the regex pattern.
     */
    @Override
    public boolean eval(String name, String value) {
        return (!name.equals(attributeName)) ||
                (value != null && Pattern.compile(pattern).matcher(value).matches());
    }

}