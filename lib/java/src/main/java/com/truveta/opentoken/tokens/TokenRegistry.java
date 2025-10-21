/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import com.truveta.opentoken.attributes.AttributeExpression;
import java.util.ServiceLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenRegistry {

    private TokenRegistry() {

    }

    public static Map<String, List<AttributeExpression>> loadAllTokens() {
        Map<String, List<AttributeExpression>> tokensMap = new HashMap<>();
        ServiceLoader<Token> loader = ServiceLoader.load(Token.class);
        for (Token token : loader) {
            tokensMap.put(token.getIdentifier(), token.getDefinition());
        }
        return tokensMap;
    }
}
