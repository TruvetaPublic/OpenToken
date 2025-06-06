/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes;

import java.util.ArrayList;
import java.util.List;
import com.truveta.opentoken.attributes.validation.NotNullOrEmptyValidator;
import com.truveta.opentoken.attributes.validation.SerializableAttributeValidator;

/**
 * A base implementation of the {@link SerializableAttribute} interface.
 * 
 * <p>
 * This class provides a default implementation of the
 * {@link Attribute#validate(String)}
 * method that validates the attribute value against a set of validation rules.
 * </p>
 * 
 * <p>
 * The default validation rules are:
 * <ul>
 * <li>Not null or empty</li>
 * </ul>
 * </p>
 */
public abstract class BaseAttribute implements SerializableAttribute {

    private static final long serialVersionUID = 1L;

    private final List<SerializableAttributeValidator> validationRules;

    protected BaseAttribute(List<SerializableAttributeValidator> validationRules) {
        ArrayList<SerializableAttributeValidator> ruleList = new ArrayList<>();
        ruleList.add(new NotNullOrEmptyValidator());
        ruleList.addAll(validationRules);
        this.validationRules = List.copyOf(ruleList);
    }

    /**
     * Validates the attribute value against a set of validation rules.
     */
    @Override
    public boolean validate(String value) {
        if (value == null) {
            return false;
        }

        return validationRules.stream().allMatch(rule -> rule.eval(value));
    }
}
