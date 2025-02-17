/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.unit.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import com.truveta.opentoken.attributes.person.FirstNameAttribute;
import com.truveta.opentoken.attributes.person.LastNameAttribute;
import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.processor.PersonAttributesProcessor;
import com.truveta.opentoken.tokens.TokenGenerator;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;

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
        List<TokenTransformer> tokenTransformerList = Collections.singletonList(mock(HashTokenTransformer.class));
        Map<Class<? extends Attribute>, String> data = Map.of(RecordIdAttribute.class, "TestRecordId",
                FirstNameAttribute.class,
                "John",
                LastNameAttribute.class, "Spencer");

        when(reader.hasNext()).thenReturn(true, false);
        when(reader.next()).thenReturn(data);

        PersonAttributesProcessor.process(reader, writer, tokenTransformerList);

        verify(reader).next();
        verify(writer).writeAttributes(any());
    }

    @Test
    public void testProcess_IOExceptionWritingAttributes() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections.singletonList(mock(TokenTransformer.class));
        Map<Class<? extends Attribute>, String> data = Map.of(RecordIdAttribute.class, "TestRecordId",
                FirstNameAttribute.class,
                "John",
                LastNameAttribute.class, "Spencer");

        when(reader.hasNext()).thenReturn(true, false);
        when(reader.next()).thenReturn(data);

        doThrow(new IOException("Test Exception")).when(writer).writeAttributes(any());

        assertDoesNotThrow(() -> PersonAttributesProcessor.process(reader, writer,
                tokenTransformerList));

        verify(reader).next();
        verify(writer, atLeastOnce()).writeAttributes(any());
    }
}
