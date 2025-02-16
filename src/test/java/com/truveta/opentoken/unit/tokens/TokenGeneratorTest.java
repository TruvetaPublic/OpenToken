// Copyright (c) Truveta. All rights reserved.
package com.truveta.opentoken.unit.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.truveta.opentoken.attributes.AttributeExpression;
import com.truveta.opentoken.attributes.validation.ValidationRules;
import com.truveta.opentoken.tokens.BaseTokenDefinition;
import com.truveta.opentoken.tokens.SHA256Tokenizer;
import com.truveta.opentoken.tokens.TokenGenerator;
import com.truveta.opentoken.tokentransformer.TokenTransformer;

public class TokenGeneratorTest {
    @Mock
    private SHA256Tokenizer tokenizer;

    @Mock
    private List<TokenTransformer> tokenTransformerList;

    @Mock
    private BaseTokenDefinition tokenDefinition;

    @Mock
    private ValidationRules validationRules;

    @InjectMocks
    private TokenGenerator tokenGenerator;

    @BeforeEach
    void setUp() throws Exception {
        tokenDefinition = mock(BaseTokenDefinition.class);
        tokenTransformerList = new ArrayList<>();
        validationRules = mock(ValidationRules.class);
        tokenizer = mock(SHA256Tokenizer.class);

        tokenGenerator = new TokenGenerator(tokenDefinition, tokenTransformerList);
        tokenGenerator.setTokenizer(tokenizer); // Inject mock tokenizer
        tokenGenerator.setValidationRules(validationRules); // Inject mock validation rules
    }

    @Test
    void testGetAllTokens_validTokensWithExpressions() throws Exception {
        when(tokenDefinition.getTokenIdentifiers()).thenReturn(Set.of("token1", "token2"));

        AttributeExpression attrExpr1 = new AttributeExpression("name", "U");
        AttributeExpression attrExpr2 = new AttributeExpression("address", "R('Street','St')");

        ArrayList<AttributeExpression> attributeExpressions1 = new ArrayList<>();
        attributeExpressions1.add(attrExpr1);
        ArrayList<AttributeExpression> attributeExpressions2 = new ArrayList<>();
        attributeExpressions2.add(attrExpr2);

        when(tokenDefinition.getTokenDefinition("token1")).thenReturn(attributeExpressions1);
        when(tokenDefinition.getTokenDefinition("token2")).thenReturn(attributeExpressions2);

        Map<String, String> personAttributes = new HashMap<>();
        personAttributes.put("name", "John");
        personAttributes.put("address", "123 Main Street");

        when(validationRules.validate(anyMap(), anyString())).thenReturn(true);
        when(tokenizer.tokenize(anyString())).thenReturn("hashedToken");

        Map<String, String> tokens = tokenGenerator.getAllTokens(personAttributes);

        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("hashedToken", tokens.get("token1"));
        assertEquals("hashedToken", tokens.get("token2"));
    }

    @Test
    void testGetAllTokens_invalidAttribute_skipsTokenGeneration() throws Exception {
        when(tokenDefinition.getTokenIdentifiers()).thenReturn(Set.of("token1"));

        AttributeExpression attrExpr = new AttributeExpression("name", "U");

        ArrayList<AttributeExpression> attributeExpressions = new ArrayList<>();
        attributeExpressions.add(attrExpr);
        when(tokenDefinition.getTokenDefinition("token1")).thenReturn(attributeExpressions);

        Map<String, String> personAttributes = new HashMap<>(); // Person attributes (invalid case with missing name)
        personAttributes.put("address", "123 Main Street");

        // Validation should fail since 'name' is missing
        when(validationRules.validate(personAttributes, "name")).thenReturn(false);

        Map<String, String> tokens = tokenGenerator.getAllTokens(personAttributes);

        // Validate that no tokens are generated
        assertTrue(tokens.isEmpty(), "Expected no tokens to be generated due to validation failure");
    }

    @Test
    void testGetAllTokens_errorInTokenGeneration_logsError() throws Exception {
        when(tokenDefinition.getTokenIdentifiers()).thenReturn(Set.of("token1"));

        AttributeExpression attrExpr = new AttributeExpression("name", "U");

        ArrayList<AttributeExpression> attributeExpressions = new ArrayList<>();
        attributeExpressions.add(attrExpr);
        when(tokenDefinition.getTokenDefinition("token1")).thenReturn(attributeExpressions);

        Map<String, String> personAttributes = new HashMap<>();
        personAttributes.put("name", "John");

        when(validationRules.validate(personAttributes, "name")).thenReturn(true);

        // Simulate error during tokenization
        when(tokenizer.tokenize(anyString())).thenThrow(new RuntimeException("Tokenization error"));

        Map<String, String> tokens = tokenGenerator.getAllTokens(personAttributes);

        // Validate that no tokens are generated due to tokenization error
        assertTrue(tokens.isEmpty(), "Expected no tokens to be generated due to tokenization error");
    }
}
