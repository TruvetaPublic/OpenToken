// Copyright (c) Truveta. All rights reserved.
package com.truveta.opentoken.integration.tokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.truveta.opentoken.tokens.BaseTokenDefinition;
import com.truveta.opentoken.tokens.TokenDefinition;
import com.truveta.opentoken.tokens.TokenGenerator;
import com.truveta.opentoken.tokentransformer.TokenTransformer;

public class TokenGeneratorTest {

    @Mock
    private List<TokenTransformer> tokenTransformerList;

    @Mock
    private BaseTokenDefinition tokenDefinition;

    @InjectMocks
    private TokenGenerator tokenGenerator;
    
    @BeforeEach
    void setUp() throws Exception {
        // Setup real TokenDefinition and TokenTransformers for integration testing
        tokenDefinition = new TokenDefinition();
        tokenTransformerList = new ArrayList<>();

        tokenGenerator = new TokenGenerator(tokenDefinition, tokenTransformerList);
    }

    @Test
    void testGetAllTokens_validPersonAttributes_generatesTokens() throws Exception {
        // Define token identifiers and attribute expressions
        tokenDefinition = new TokenDefinition();

        // Person attributes to be used for token generation
        Map<String, String> personAttributes = new HashMap<>();
        personAttributes.put("FirstName", "Alice");
        personAttributes.put("LastName", "Wonderland");
        personAttributes.put("SocialSecurityNumber", "345-54-6795");
        personAttributes.put("PostalCode", "98052");
        personAttributes.put("BirthDate", "1993-08-10");

        // Generate all tokens
        Map<String, String> tokens = tokenGenerator.getAllTokens(personAttributes);

        // Validate the tokens
        assertNotNull(tokens);
        assertEquals(5, tokens.size(), "Expected 5 tokens to be generated");

        // Validate the actual tokens generated
        assertTrue(tokens.containsKey("T1"));
        assertTrue(tokens.containsKey("T2"));
        assertTrue(tokens.containsKey("T3"));
        assertTrue(tokens.containsKey("T4"));
        assertTrue(tokens.containsKey("T5"));

        assertTrue(tokens.get("T1").equals("812f4cec4ff577e90f6a0dce95361be59b3208892ffe46ce970649e35c1e923d"));
        assertTrue(tokens.get("T2").equals("786aba25e47fc44c9b6dce5ad9bc84d8dd488c67d89f5909011cfde703993918"));
        assertTrue(tokens.get("T3").equals("7d69eb47783a8ddc2783c26a70d63b0018f5018c845ee95087074fb6da2b3fb7"));
        assertTrue(tokens.get("T4").equals("67d996d74a0b7d452fede8b297a7b44362307f9706c10e0004c9e72b11a02723"));
        assertTrue(tokens.get("T5").equals("ffabff596628478fe4ebeef4a971f34c670e9ecff8b012ac54c660567932fbca"));

    }     
}
