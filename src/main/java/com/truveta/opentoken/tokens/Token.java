/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import java.util.ArrayList;
import com.truveta.opentoken.attributes.AttributeExpression;

/**
 * A token is a collection of attribute expressions that are concatenated
 * together to get the token signature
 * and a unique identifier.
 */
public interface Token {

    /**
     * Get the unique identifier for the token.
     * 
     * @return the unique identifier for the token
     */
    String getIdentifier();

    /**
     * Get the list of attribute expressions that define the token.
     * 
     * @return the list of attribute expressions that define the token
     */
    ArrayList<AttributeExpression> getDefinition();
}
