// Copyright (c) Truveta. All rights reserved.
package com.truveta.opentoken.unit.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.processor.PersonAttributesProcessor;
import com.truveta.opentoken.tokens.TokenGenerator;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;

@ExtendWith(MockitoExtension.class)
public class PersonAttributesProcessorTest {
    @Mock
    private PersonAttributesReader reader;

    @Mock
    private PersonAttributesWriter writer;

    @Mock
    private TokenGenerator tokenGenerator;

    @Test
    public void testProcess_HappyPath() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections.singletonList(mock(HashTokenTransformer.class));
        List<Map<String, String>> data = Collections.singletonList(Map.of("RecordId", "TestRecordId", "FirstName", "John", "LastName", "Spencer"));
        when(reader.readAttributes()).thenReturn(data);

        PersonAttributesProcessor.process(reader, writer, tokenTransformerList);

        verify(reader).readAttributes();
        verify(writer).writeAttributes(any());
    }

    @Test
    public void testProcess_IOExceptionReadingAttributes() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections.singletonList(mock(TokenTransformer.class));
        when(reader.readAttributes()).thenThrow(new IOException("Test Exception"));

        assertDoesNotThrow(() -> PersonAttributesProcessor.process(reader, writer, tokenTransformerList));
        verify(reader).readAttributes();
    }

    @Test
    public void testProcess_IOExceptionWritingAttributes() throws IOException {
        List<TokenTransformer> tokenTransformerList = Collections.singletonList(mock(TokenTransformer.class));
        List<Map<String, String>> data = Collections.singletonList(Map.of("RecordId", "1", "Attribute1", "Value1"));
        when(reader.readAttributes()).thenReturn(data);
        doThrow(new IOException("Test Exception")).when(writer).writeAttributes(any());

        assertDoesNotThrow(() -> PersonAttributesProcessor.process(reader, writer, tokenTransformerList));
        verify(reader).readAttributes();
        verify(writer).writeAttributes(any());
    }
}
