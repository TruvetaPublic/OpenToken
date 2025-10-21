/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens.definitions;

import java.util.ArrayList;
import com.truveta.opentoken.attributes.AttributeExpression;
import com.truveta.opentoken.attributes.person.BirthDateAttribute;
import com.truveta.opentoken.attributes.person.FirstNameAttribute;
import com.truveta.opentoken.attributes.person.LastNameAttribute;
import com.truveta.opentoken.attributes.person.SexAttribute;
import com.truveta.opentoken.tokens.Token;

/**
 * Represents the token definition for token T3.
 * 
 * <p>
 * It is a collection of attribute expressions
 * that are concatenated together to get the token signature.
 * The token signature is as follows:
 * U(last-name)|U(first-name)|U(gender)|birth-date
 * </p>
 * 
 * @see com.truveta.opentoken.tokens.Token Token
 */
public class T3Token implements Token {
    private static final String ID = "T3";

    private final ArrayList<AttributeExpression> definition = new ArrayList<>();

    public T3Token() {
        definition.add(new AttributeExpression(LastNameAttribute.class, "T|U"));
        definition.add(new AttributeExpression(FirstNameAttribute.class, "T|U"));
        definition.add(new AttributeExpression(SexAttribute.class, "T|U"));
        definition.add(new AttributeExpression(BirthDateAttribute.class, "T|D"));
    }

    @Override
    public String getIdentifier() {
        return ID;
    }

    @Override
    public ArrayList<AttributeExpression> getDefinition() {
        return definition;
    }
}
