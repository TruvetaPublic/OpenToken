/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.attributes.Attribute;
import com.truveta.opentoken.attributes.general.RecordIdAttribute;
import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.tokens.TokenDefinition;
import com.truveta.opentoken.tokens.TokenGenerator;
import com.truveta.opentoken.tokens.TokenGeneratorResult;
import com.truveta.opentoken.tokentransformer.TokenTransformer;

/**
 * Process all person attributes.
 * <p>
 * This class is used to read person attributes from input source,
 * generate tokens for each person record and write the tokens back
 * to the output data source.
 */
public final class PersonAttributesProcessor {

    public static final String TOTAL_ROWS = "TotalRows";
    public static final String TOTAL_ROWS_WITH_INVALID_ATTRIBUTES = "TotalRowsWithInvalidAttributes";
    public static final String INVALID_ATTRIBUTES_BY_TYPE = "InvalidAttributesByType";
    public static final String BLANK_TOKENS_BY_RULE = "BlankTokensByRule";

    private static final Logger logger = LoggerFactory.getLogger(PersonAttributesProcessor.class);

    PersonAttributesProcessor() {
    }

    /**
     * Reads person attributes from the input data source, generates token, and
     * write the result back to the output data source. The tokens can be optionally
     * transformed before writing.
     * 
     * @param reader               the reader initialized with the input data
     *                             source.
     * @param writer               the writer initialized with the output data
     *                             source.
     * @param tokenTransformerList a list of token transformers.
     * 
     * @see com.truveta.opentoken.io.PersonAttributesReader PersonAttributesReader
     * @see com.truveta.opentoken.io.PersonAttributesWriter PersonAttributesWriter
     * @see com.truveta.opentoken.tokentransformer.TokenTransformer TokenTransformer
     */
    public static void process(PersonAttributesReader reader, PersonAttributesWriter writer,
            List<TokenTransformer> tokenTransformerList, Map<String, Object> metadataMap) throws IOException {

        // TokenGenerator code
        TokenDefinition tokenDefinition = new TokenDefinition();
        TokenGenerator tokenGenerator = new TokenGenerator(tokenDefinition, tokenTransformerList);

        Map<Class<? extends Attribute>, String> row;
        TokenGeneratorResult tokenGeneratorResult;

        long rowCounter = 0;
        Map<String, Long> invalidAttributeCount = initializeInvalidAttributeCount();
        Map<String, Long> blankTokensByRuleCount = initializeBlankTokensByRuleCount(tokenDefinition);

        while (reader.hasNext()) {
            row = reader.next();
            rowCounter++;

            tokenGeneratorResult = tokenGenerator.getAllTokens(row);
            logger.debug("Tokens: {}", tokenGeneratorResult.getTokens());

            keepTrackOfInvalidAttributes(tokenGeneratorResult, rowCounter,
                    invalidAttributeCount);

            keepTrackOfBlankTokens(tokenGeneratorResult, rowCounter,
                    blankTokensByRuleCount);

            writeTokens(writer, row, rowCounter, tokenGeneratorResult);

            if (rowCounter % 10000 == 0) {
                logger.info(String.format("Processed \"%,d\" records", rowCounter));
            }
        }

        logger.info(String.format("Processed a total of %,d records", rowCounter));

        invalidAttributeCount
                .forEach((key, value) -> logger
                        .info(String.format("Total invalid Attribute count for [%s]: %,d", key, value)));
        long rowIssueCounter = invalidAttributeCount.values().stream()
                .collect(Collectors.summarizingLong(Long::longValue)).getSum();

        metadataMap.put(TOTAL_ROWS, rowCounter);
        metadataMap.put(TOTAL_ROWS_WITH_INVALID_ATTRIBUTES, rowIssueCounter);
        // Alphabetize attribute and token rule keys for deterministic metadata output
        metadataMap.put(INVALID_ATTRIBUTES_BY_TYPE, new java.util.TreeMap<>(invalidAttributeCount));
        metadataMap.put(BLANK_TOKENS_BY_RULE, new java.util.TreeMap<>(blankTokensByRuleCount));
        logger.info(String.format("Total number of records with invalid attributes: %,d", rowIssueCounter));

        blankTokensByRuleCount
                .forEach((key, value) -> logger
                        .info(String.format("Total blank tokens for rule [%s]: %,d", key, value)));
        long blankTokensTotal = blankTokensByRuleCount.values().stream()
                .collect(Collectors.summarizingLong(Long::longValue)).getSum();
        logger.info(String.format("Total blank tokens generated: %,d", blankTokensTotal));
    }

    private static void writeTokens(PersonAttributesWriter writer, Map<Class<? extends Attribute>, String> row,
            long rowCounter, TokenGeneratorResult tokenGeneratorResult) {

        Set<String> tokenIds = new TreeSet<>(tokenGeneratorResult.getTokens().keySet());

        // Generate a UUID for RecordId if it's not present in the input data
        String recordId = row.get(RecordIdAttribute.class);
        if (recordId == null || recordId.isEmpty()) {
            recordId = UUID.randomUUID().toString();
        }

        for (String tokenId : tokenIds) {
            var rowResult = new HashMap<String, String>();
            rowResult.put(TokenConstants.RECORD_ID, recordId);
            rowResult.put(TokenConstants.RULE_ID, tokenId);
            rowResult.put(TokenConstants.TOKEN, tokenGeneratorResult.getTokens().get(tokenId));

            try {
                writer.writeAttributes(rowResult);
            } catch (IOException e) {
                logger.error(String.format("Error writing attributes to file for row %,d", rowCounter), e);
            }
        }
    }

    private static void keepTrackOfInvalidAttributes(TokenGeneratorResult tokenGeneratorResult, long rowCounter,
            Map<String, Long> invalidAttributeCount) {

        if (!tokenGeneratorResult.getInvalidAttributes().isEmpty()) {
            logger.info("Invalid Attributes for row {}: {}", String.format("%,d", rowCounter),
                    tokenGeneratorResult.getInvalidAttributes());

            for (String invalidAttribute : tokenGeneratorResult.getInvalidAttributes()) {
                if (invalidAttributeCount.containsKey(invalidAttribute)) {
                    invalidAttributeCount.put(invalidAttribute, invalidAttributeCount.get(invalidAttribute) + 1);
                } else {
                    invalidAttributeCount.put(invalidAttribute, 1L);
                }
            }
        }
    }

    private static void keepTrackOfBlankTokens(TokenGeneratorResult tokenGeneratorResult, long rowCounter,
            Map<String, Long> blankTokensByRuleCount) {

        if (!tokenGeneratorResult.getBlankTokensByRule().isEmpty()) {
            logger.debug("Blank tokens for row {}: {}", String.format("%,d", rowCounter),
                    tokenGeneratorResult.getBlankTokensByRule());

            for (String ruleId : tokenGeneratorResult.getBlankTokensByRule()) {
                blankTokensByRuleCount.merge(ruleId, 1L, Long::sum);
            }
        }
    }

    /**
     * Initialize the invalid attribute count map with all registered attributes set to 0.
     * This ensures that all attribute types appear in the metadata even in happy path scenarios.
     *
     * @return a map with all attribute names initialized to 0
     */
    private static Map<String, Long> initializeInvalidAttributeCount() {
        Map<String, Long> invalidAttributeCount = new HashMap<>();
        Set<Attribute> attributes = com.truveta.opentoken.attributes.AttributeLoader.load();
        for (Attribute attribute : attributes) {
            invalidAttributeCount.put(attribute.getName(), 0L);
        }
        return invalidAttributeCount;
    }

    /**
     * Initialize the blank tokens by rule count map with all token identifiers set to 0.
     * This ensures that all token rules appear in the metadata even in happy path scenarios.
     *
     * @param tokenDefinition the token definition containing all token identifiers
     * @return a map with all token identifiers initialized to 0
     */
    private static Map<String, Long> initializeBlankTokensByRuleCount(TokenDefinition tokenDefinition) {
        Map<String, Long> blankTokensByRuleCount = new HashMap<>();
        for (String tokenId : tokenDefinition.getTokenIdentifiers()) {
            blankTokensByRuleCount.put(tokenId, 0L);
        }
        return blankTokensByRuleCount;
    }
}