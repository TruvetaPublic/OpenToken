/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.truveta.opentoken.attributes.Attribute;
import com.truveta.opentoken.attributes.general.RecordIdAttribute;
import com.truveta.opentoken.attributes.person.BirthDateAttribute;
import com.truveta.opentoken.attributes.person.FirstNameAttribute;
import com.truveta.opentoken.attributes.person.LastNameAttribute;
import com.truveta.opentoken.attributes.person.PostalCodeAttribute;
import com.truveta.opentoken.attributes.person.SexAttribute;
import com.truveta.opentoken.attributes.person.SocialSecurityNumberAttribute;
import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.tokens.TokenGenerator;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;
import com.truveta.opentoken.Metadata;

@ExtendWith(MockitoExtension.class)
class PersonAttributesProcessorTest {

    @Mock
    private PersonAttributesReader reader;

    @Mock
    private PersonAttributesWriter writer;

    @Mock
    private TokenGenerator tokenGenerator;

    @Test
    void testProcess_HappyPath() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections
                .singletonList(mock(HashTokenTransformer.class));
        Map<Class<? extends Attribute>, String> data = Map.of(RecordIdAttribute.class, "TestRecordId",
                FirstNameAttribute.class,
                "John",
                LastNameAttribute.class, "Spencer");

        when(reader.hasNext()).thenReturn(true, false);
        when(reader.next()).thenReturn(data);

        Map<String, Object> metadataMap = new Metadata().initialize();
        PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

        verify(reader).next();
        verify(writer, times(5)).writeAttributes(any());

        // Verify metadata was populated
        assertFalse(metadataMap.isEmpty(), "Metadata map should not be empty after processing");
        assertTrue(metadataMap.containsKey(PersonAttributesProcessor.TOTAL_ROWS),
                "Metadata should contain totalRows key");
    }

    @Test
    void testProcess_IOExceptionWritingAttributes() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections.singletonList(mock(TokenTransformer.class));
        Map<Class<? extends Attribute>, String> data = Map.of(RecordIdAttribute.class, "TestRecordId",
                FirstNameAttribute.class,
                "John",
                LastNameAttribute.class, "Spencer");

        when(reader.hasNext()).thenReturn(true, false);
        when(reader.next()).thenReturn(data);

        doThrow(new IOException("Test Exception")).when(writer).writeAttributes(any());

        Map<String, Object> metadataMap = new Metadata().initialize();

        PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

        verify(reader).next();
        verify(writer, atLeastOnce()).writeAttributes(any());

        // Verify metadata was populated
        assertFalse(metadataMap.isEmpty(), "Metadata map should not be empty after processing");
        assertTrue(metadataMap.containsKey("TotalRows"), "Metadata should contain totalRows key");
    }

    @Test
    void testMetadataMap_ContainsCorrectValues() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections
                .singletonList(mock(HashTokenTransformer.class));
        Map<Class<? extends Attribute>, String> data = Map.of(RecordIdAttribute.class, "TestRecordId",
                FirstNameAttribute.class, "John",
                LastNameAttribute.class, "Spencer");

        when(reader.hasNext()).thenReturn(true, false);
        when(reader.next()).thenReturn(data);

        Map<String, Object> metadataMap = new Metadata().initialize();

        PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

        // Check that the metadata map contains all expected keys with correct values
        assertTrue(metadataMap.containsKey(PersonAttributesProcessor.TOTAL_ROWS),
                "Metadata should contain totalRows key");
        assertTrue(metadataMap.containsKey(PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES),
                "Metadata should contain totalRowsWithInvalidAttributes key");
        assertTrue(metadataMap.containsKey(PersonAttributesProcessor.INVALID_ATTRIBUTES_BY_TYPE),
                "Metadata should contain invalidAttributesByType key");
        assertTrue(metadataMap.containsKey(PersonAttributesProcessor.BLANK_TOKENS_BY_RULE),
                "Metadata should contain blankTokensByRule key");

        // Verify values
        assertEquals(1L, metadataMap.get(PersonAttributesProcessor.TOTAL_ROWS), "Total rows should be 1");
        assertEquals(0L, metadataMap.get(PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES),
                "Total rows with invalid attributes should be 0");

        // The invalid attributes map should contain all attributes with zero counts
        @SuppressWarnings("unchecked")
        Map<String, Long> invalidAttributesMap = (Map<String, Long>) metadataMap
                .get(PersonAttributesProcessor.INVALID_ATTRIBUTES_BY_TYPE);
        assertFalse(invalidAttributesMap.isEmpty(), "Invalid attributes map should contain all attributes initialized to 0");
        
        // Verify all invalid attribute values are 0 (no invalid attributes in this test)
        for (Long count : invalidAttributesMap.values()) {
            assertEquals(0L, count, "All attribute counts should be 0 with valid data");
        }
        
        // Verify blank tokens map contains all token rules
        @SuppressWarnings("unchecked")
        Map<String, Long> blankTokensMap = (Map<String, Long>) metadataMap
                .get(PersonAttributesProcessor.BLANK_TOKENS_BY_RULE);
        assertFalse(blankTokensMap.isEmpty(), "Blank tokens map should contain all token rules initialized to 0");
        
        // Note: This test data (FirstName, LastName only) will generate blank tokens
        // because required attributes like Sex, BirthDate, SSN, PostalCode are missing
        // So we just verify that the map is present and contains entries, not that all are 0
        assertTrue(blankTokensMap.size() > 0, "Blank tokens map should have entries for all token rules");
    }

    @Test
    void testMetadataMap_HappyPath_AllAttributesPresent() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections
                .singletonList(mock(HashTokenTransformer.class));
        // Provide all required attributes so no blank tokens are generated
        Map<Class<? extends Attribute>, String> data = Map.of(
                RecordIdAttribute.class, "TestRecordId",
                FirstNameAttribute.class, "John",
                LastNameAttribute.class, "Spencer",
                SocialSecurityNumberAttribute.class, "234-56-7890",
                BirthDateAttribute.class, "1990-01-15",
                SexAttribute.class, "Male",
                PostalCodeAttribute.class, "98052");

        when(reader.hasNext()).thenReturn(true, false);
        when(reader.next()).thenReturn(data);

        Map<String, Object> metadataMap = new Metadata().initialize();

        PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

        // Verify invalid attributes map contains all attributes with zero counts (happy path)
        @SuppressWarnings("unchecked")
        Map<String, Long> invalidAttributesMap = (Map<String, Long>) metadataMap
                .get(PersonAttributesProcessor.INVALID_ATTRIBUTES_BY_TYPE);
        assertFalse(invalidAttributesMap.isEmpty(), 
                "Invalid attributes map should contain all attributes initialized to 0");
        
        // Verify all invalid attribute values are 0 in the happy path
        for (Long count : invalidAttributesMap.values()) {
            assertEquals(0L, count, "All attribute counts should be 0 in happy path");
        }
        
        // Verify blank tokens map contains all token rules with zero counts (happy path)
        @SuppressWarnings("unchecked")
        Map<String, Long> blankTokensMap = (Map<String, Long>) metadataMap
                .get(PersonAttributesProcessor.BLANK_TOKENS_BY_RULE);
        assertFalse(blankTokensMap.isEmpty(), 
                "Blank tokens map should contain all token rules initialized to 0");
        
        // Verify all blank token counts are 0 in the happy path (all required attributes present)
        for (Long count : blankTokensMap.values()) {
            assertEquals(0L, count, "All token rule counts should be 0 in happy path");
        }
    }

    @Test
    void testMetadataMap_MultipleRows() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections
                .singletonList(mock(HashTokenTransformer.class));

        // Create three data records
        Map<Class<? extends Attribute>, String> data1 = Map.of(RecordIdAttribute.class, "TestRecordId1",
                FirstNameAttribute.class, "John",
                LastNameAttribute.class, "Spencer");

        when(reader.hasNext()).thenReturn(true, true, true, false);
        when(reader.next())
                .thenReturn(data1)
                .thenReturn(Map.of(RecordIdAttribute.class, "TestRecordId2",
                        FirstNameAttribute.class, "Jane",
                        LastNameAttribute.class, "Doe"))
                .thenReturn(Map.of(RecordIdAttribute.class, "TestRecordId3",
                        FirstNameAttribute.class, "Alex",
                        LastNameAttribute.class, "Smith"));

        Map<String, Object> metadataMap = new Metadata().initialize();

        // Execute
        PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

        // Verify
        assertEquals(3L, metadataMap.get(PersonAttributesProcessor.TOTAL_ROWS), "Total rows should be 3");
        assertEquals(0L, metadataMap.get(PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES),
                "Total rows with invalid attributes should be 0");
    }

    @Test
    void testMetadataMap_PreservesExistingEntries() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections
                .singletonList(mock(HashTokenTransformer.class));
        Map<Class<? extends Attribute>, String> data = Map.of(RecordIdAttribute.class, "TestRecordId",
                FirstNameAttribute.class, "John",
                LastNameAttribute.class, "Spencer");

        when(reader.hasNext()).thenReturn(true, false);
        when(reader.next()).thenReturn(data);

        Map<String, Object> metadataMap = new Metadata().initialize();
        metadataMap.put("ExistingKey1", "ExistingValue1");
        metadataMap.put("ExistingKey2", "ExistingValue2");

        PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

        // Verify original entries are preserved
        assertTrue(metadataMap.containsKey("ExistingKey1"), "Metadata should preserve existing key1");
        assertTrue(metadataMap.containsKey("ExistingKey2"), "Metadata should preserve existing key2");
        assertEquals("ExistingValue1", metadataMap.get("ExistingKey1"),
                "Value for existing key1 should be preserved");
        assertEquals("ExistingValue2", metadataMap.get("ExistingKey2"),
                "Value for existing key2 should be preserved");

        // And new entries are added
        assertTrue(metadataMap.containsKey("TotalRows"), "Metadata should contain totalRows key");
    }
}
