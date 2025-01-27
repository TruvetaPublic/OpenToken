/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the token definitions.
 * 
 * <p>
 * The tokens are generated using some token generation rules. This class
 * encapsulates the definition of those rules. Together, they are commonly
 * referred to as <b>token definitions</b> or <b>rule definitions</b>.
 * 
 * <p>
 * Each token/rule definition is a collection of
 * <code>AttributeExpression</code> that are concatenated together to get
 * the token signature.
 * 
 * @see com.truveta.opentoken.tokens.AttributeExpression AttributeExpression
 */
public class TokenDefinition implements BaseTokenDefinition {
    private final Map<String, List<AttributeExpression>> definitions;

    /**
     * Initializes the token definitions.
     */
    public TokenDefinition() {
        // Token 1
        List<AttributeExpression> t1 = new ArrayList<>();
        t1.add(new AttributeExpression(LAST_NAME, "T|U"));
        t1.add(new AttributeExpression(FIRST_NAME, "T|S(0,1)|U"));
        t1.add(new AttributeExpression(GENDER, "T|U"));
        t1.add(new AttributeExpression(BIRTH_DATE, "T|D"));
        t1 = Collections.unmodifiableList(t1);

        // Token 2
        List<AttributeExpression> t2 = new ArrayList<>();
        t2.add(new AttributeExpression(LAST_NAME, "T|U"));
        t2.add(new AttributeExpression(FIRST_NAME, "T|U"));
        t2.add(new AttributeExpression(BIRTH_DATE, "T|D"));
        t2.add(new AttributeExpression(POSTAL_CODE, "T|S(0,3)|U"));
        t2 = Collections.unmodifiableList(t2);

        // Token 3
        List<AttributeExpression> t3 = new ArrayList<>();
        t3.add(new AttributeExpression(LAST_NAME, "T|U"));
        t3.add(new AttributeExpression(FIRST_NAME, "T|U"));
        t3.add(new AttributeExpression(GENDER, "T|U"));
        t3.add(new AttributeExpression(BIRTH_DATE, "T|D"));
        t3 = Collections.unmodifiableList(t3);

        // Token 4
        List<AttributeExpression> t4 = new ArrayList<>();
        t4.add(new AttributeExpression(SOCIAL_SECURITY_NUMBER, "T|M(\\d+)"));
        t4.add(new AttributeExpression(GENDER, "T|U"));
        t4.add(new AttributeExpression(BIRTH_DATE, "T|D"));
        t4 = Collections.unmodifiableList(t4);

        // Token 5
        List<AttributeExpression> t5 = new ArrayList<>();
        t5.add(new AttributeExpression(LAST_NAME, "T|U"));
        t5.add(new AttributeExpression(FIRST_NAME, "T|S(0,3)|U"));
        t5.add(new AttributeExpression(GENDER, "T|U"));
        t5 = Collections.unmodifiableList(t5);

        Map<String, List<AttributeExpression>> definitionMap = new HashMap<>();
        definitionMap.put("T1", t1);
        definitionMap.put("T2", t2);
        definitionMap.put("T3", t3);
        definitionMap.put("T4", t4);
        definitionMap.put("T5", t5);
        this.definitions = Collections.unmodifiableMap(definitionMap);
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public Set<String> getTokenIdentifiers() {
        return definitions.keySet();
    }

    @Override
    public List<AttributeExpression> getTokenDefinition(String tokenId) {
        return definitions.get(tokenId);
    }
}