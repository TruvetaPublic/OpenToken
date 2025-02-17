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

import com.truveta.opentoken.attributes.Attribute;
import com.truveta.opentoken.attributes.AttributeExpression;
import com.truveta.opentoken.attributes.person.FirstNameAttribute;
import com.truveta.opentoken.attributes.person.LastNameAttribute;
//import com.truveta.opentoken.attributes.validation.ValidationRules;
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

    @InjectMocks
    private TokenGenerator tokenGenerator;

    @BeforeEach
    void setUp() throws Exception {
        tokenDefinition = mock(BaseTokenDefinition.class);
        tokenTransformerList = new ArrayList<>();
        tokenizer = mock(SHA256Tokenizer.class);

        tokenGenerator = new TokenGenerator(tokenDefinition, tokenTransformerList);
        tokenGenerator.setTokenizer(tokenizer); // Inject mock tokenizer

    }

    @Test
    void testGetAllTokens_validTokensWithExpressions() throws Exception {
        when(tokenDefinition.getTokenIdentifiers()).thenReturn(Set.of("token1",
                "token2"));

        AttributeExpression attrExpr1 = new AttributeExpression(FirstNameAttribute.class, "U");
        AttributeExpression attrExpr2 = new AttributeExpression(LastNameAttribute.class,
                "R('MacDonald','Donald')");

        ArrayList<AttributeExpression> attributeExpressions1 = new ArrayList<>();
        attributeExpressions1.add(attrExpr1);
        ArrayList<AttributeExpression> attributeExpressions2 = new ArrayList<>();
        attributeExpressions2.add(attrExpr2);

        when(tokenDefinition.getTokenDefinition("token1")).thenReturn(attributeExpressions1);
        when(tokenDefinition.getTokenDefinition("token2")).thenReturn(attributeExpressions2);

        Map<Class<? extends Attribute>, String> personAttributes = new HashMap<>();
        personAttributes.put(FirstNameAttribute.class, "John");
        personAttributes.put(LastNameAttribute.class, "Old MacDonald");

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

        AttributeExpression attrExpr = new AttributeExpression(FirstNameAttribute.class, "U");

        ArrayList<AttributeExpression> attributeExpressions = new ArrayList<>();
        attributeExpressions.add(attrExpr);
        when(tokenDefinition.getTokenDefinition("token1")).thenReturn(attributeExpressions);

        Map<Class<? extends Attribute>, String> personAttributes = new HashMap<>(); // Person attributes (invalid case
                                                                                    // with missing name)
        personAttributes.put(LastNameAttribute.class, "MacDonald");

        Map<String, String> tokens = tokenGenerator.getAllTokens(personAttributes);

        // Validate that no tokens are generated
        assertTrue(tokens.isEmpty(), "Expected no tokens to be generated due to validation failure");
    }

    @Test
    void testGetAllTokens_errorInTokenGeneration_logsError() throws Exception {
        when(tokenDefinition.getTokenIdentifiers()).thenReturn(Set.of("token1"));

        AttributeExpression attrExpr = new AttributeExpression(FirstNameAttribute.class, "U");

        ArrayList<AttributeExpression> attributeExpressions = new ArrayList<>();
        attributeExpressions.add(attrExpr);
        when(tokenDefinition.getTokenDefinition("token1")).thenReturn(attributeExpressions);

        Map<Class<? extends Attribute>, String> personAttributes = new HashMap<>();
        personAttributes.put(FirstNameAttribute.class, "John");

        // Simulate error during tokenization
        when(tokenizer.tokenize(anyString())).thenThrow(new RuntimeException("Tokenization error"));

        Map<String, String> tokens = tokenGenerator.getAllTokens(personAttributes);

        // Validate that no tokens are generated due to tokenization error
        assertTrue(tokens.isEmpty(), "Expected no tokens to be generated due to tokenization error");
    }
}
