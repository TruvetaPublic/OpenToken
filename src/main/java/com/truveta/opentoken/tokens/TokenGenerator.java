/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.tokentransformer.TokenTransformer;

/**
 * Generates both the token signature and the token itself.
 */
@Getter
@Setter
public class TokenGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TokenGenerator.class.getName());
    private SHA256Tokenizer tokenizer;
    private List<TokenTransformer> tokenTransformerList;
    private BaseTokenDefinition tokenDefinition;
    private ValidationRules validationRules;

    /**
     * Initializes the token generator.
     * 
     * @param tokenDefinition      the token definition.
     * @param tokenTransformerList a list of token transformers.
     */
    public TokenGenerator(BaseTokenDefinition tokenDefinition, List<TokenTransformer> tokenTransformerList) {
        this.tokenDefinition = tokenDefinition;
        this.validationRules = new ValidationRules();
        this.tokenTransformerList = tokenTransformerList;
        try {
            this.tokenizer = new SHA256Tokenizer(tokenTransformerList);
        } catch (Exception e) {
            logger.error("Error initializing tokenizer with hashing secret", e);
        }
    }

    /*
     * Get the token signature for a given token identifier.
     *
     * @param tokenId the token identifier. Possible values are in the range { T1,
     * T2, T3, T4, T5 }
     * 
     * @param personAttributes The person attributes. It is a map of the person
     * attribute
     * name to value. This version of the library supports the following person
     * attributes -
     * FirstName, LastName, Sex, BirthDate, PostalCode, SocialSecurityNumber.
     * 
     * @return the token signature using the token definition for the given token
     * identifier.
     */
    private String getTokenSignature(String tokenId, Map<String, String> personAttributes) {
        var definition = tokenDefinition.getTokenDefinition(tokenId);
        if (personAttributes == null) {
            throw new IllegalArgumentException("Person attributes cannot be null.");
        }

        var values = new ArrayList<String>(definition.size());

        for (AttributeExpression attribute : definition) {
            if (personAttributes.containsKey(attribute.getName())) {
                if (!validationRules.validate(personAttributes, attribute.getName())) {
                    return null;
                }
                try {
                    String attributeValue = attribute.getEffectiveValue(personAttributes.get(attribute.getName()));
                    values.add(attributeValue);
                } catch (IllegalArgumentException e) {
                    logger.error(e.getMessage());
                    return null;
                }
            }
        }

        return Stream.of(values.toArray(new String[0])).filter(s -> null != s && !s.isBlank())
                .collect(Collectors.joining("|"));
    }

    /**
     * Get the token signatures for all token/rule identifiers. This is mostly a
     * debug/logging/test method.
     * 
     * @param personAttributes the person attributes map.
     * 
     * @return A map of token/rule identifier to the token signature.
     */
    public Map<String, String> getAllTokenSignatures(Map<String, String> personAttributes) {
        var signatures = new HashMap<String, String>();
        for (String tokenId : tokenDefinition.getTokenIdentifiers()) {
            try {
                var signature = getTokenSignature(tokenId, personAttributes);
                if (signature != null) {
                    signatures.put(tokenId, signature);
                }
            } catch (Exception e) {
                logger.error("Error generating token signature for token id: " + tokenId, e);
            }
        }
        return signatures;
    }

    /*
     * Get token for a given token identifier.
     *
     * @param tokenId the token identifier. Possible values are in the range { T1,
     * T2, T3, T4, T5 }
     * 
     * @param personAttributes the person attributes map.
     * 
     * @return the token using the token definition for the given token identifier.
     * 
     * @throws Exception in case of failure to generate the token.
     */
    private String getToken(String tokenId, Map<String, String> personAttributes) throws Exception {
        var signature = getTokenSignature(tokenId, personAttributes);
        try {
            return tokenizer.tokenize(signature);
        } catch (Exception e) {
            logger.error("Error generating token for token id: " + tokenId, e);
            throw new Exception("Error generating token");
        }
    }

    /**
     * Get the tokens for all token/rule identifiers.
     * 
     * @param personAttributes the person attributes map.
     * 
     * @return A map of token/rule identifier to the token value.
     */
    public Map<String, String> getAllTokens(Map<String, String> personAttributes) {
        var tokens = new HashMap<String, String>();
        for (String tokenId : tokenDefinition.getTokenIdentifiers()) {
            try {
                var token = getToken(tokenId, personAttributes);
                if (token != null) {
                    tokens.put(tokenId, token);
                }
            } catch (Exception e) {
                logger.error("Error generating token for token id: " + tokenId, e);
            }
        }

        return tokens;
    }

    /**
     * Get invalid person attribute names.
     * 
     * @param personAttributes the person attributes map.
     * 
     * @return A set of invalid person attribute names.
     */
    public Set<String> getInvalidPersonAttributes(Map<String, String> personAttributes) {
        String[] attributeNames = {
                BaseTokenDefinition.FIRST_NAME,
                BaseTokenDefinition.LAST_NAME,
                BaseTokenDefinition.SEX,
                BaseTokenDefinition.BIRTH_DATE,
                BaseTokenDefinition.POSTAL_CODE,
                BaseTokenDefinition.SOCIAL_SECURITY_NUMBER
        };

        var response = new HashSet<String>();

        for (String attributeName : attributeNames) {
            if (!validationRules.validate(personAttributes, attributeName)) {
                response.add(attributeName);
            }
        }

        return response;
    }
}
