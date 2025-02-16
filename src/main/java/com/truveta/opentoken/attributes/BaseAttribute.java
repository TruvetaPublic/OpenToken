/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes;

import java.util.ArrayList;
import java.util.List;
import com.truveta.opentoken.attributes.validation.AttributeValidator;
import com.truveta.opentoken.attributes.validation.NullValidator;

public abstract class BaseAttribute implements Attribute {

    private final List<AttributeValidator> validationRules;

    protected BaseAttribute(List<AttributeValidator> validationRules) {
        ArrayList<AttributeValidator> ruleList = new ArrayList<>();
        ruleList.add(new NullValidator("*"));
        ruleList.addAll(validationRules);
        this.validationRules = List.copyOf(ruleList);
    }

    @Override
    public boolean validate(String value) {
        return validationRules.stream().allMatch(rule -> rule.eval(getName(), value));
    }
}
