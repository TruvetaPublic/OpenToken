/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.validation;

import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * A Validator that is designed for validating with regex expressions.
 */
@Getter
@Setter
public final class RegexValidator implements AttributeValidator {

    @NotNull
    private String attributeName;
    private final Pattern compiledPattern;

    public RegexValidator(@NotNull String attributeName, @NotNull String pattern) {
        this.attributeName = attributeName;
        this.compiledPattern = Pattern.compile(pattern);
    }

    /**
     * Validates that the value matches the regex pattern.
     */
    @Override
    public boolean eval(String name, String value) {
        return (!name.equals(attributeName)) ||
                (value != null && compiledPattern.matcher(value).matches());
    }
}