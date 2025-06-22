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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.truveta.opentoken.metrics.DemographicMetricsCollector;
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

    private static final String TOKEN = "Token";
    private static final String RULE_ID = "RuleId";
    private static final String RECORD_ID = "RecordId";

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
            List<TokenTransformer> tokenTransformerList) {
        process(reader, writer, tokenTransformerList, null);
    }

    /**
     * Reads person attributes from the input data source, generates token, and
     * write the result back to the output data source. The tokens can be optionally
     * transformed before writing. Optionally collects demographic bias metrics.
     * 
     * @param reader               the reader initialized with the input data
     *                             source.
     * @param writer               the writer initialized with the output data
     *                             source.
     * @param tokenTransformerList a list of token transformers.
     * @param metricsCollector     optional demographics metrics collector for bias
     *                             detection.
     * 
     * @see com.truveta.opentoken.io.PersonAttributesReader PersonAttributesReader
     * @see com.truveta.opentoken.io.PersonAttributesWriter PersonAttributesWriter
     * @see com.truveta.opentoken.tokentransformer.TokenTransformer TokenTransformer
     */
    public static void process(PersonAttributesReader reader, PersonAttributesWriter writer,
            List<TokenTransformer> tokenTransformerList, DemographicMetricsCollector metricsCollector) {

        // TokenGenerator code
        TokenGenerator tokenGenerator = new TokenGenerator(new TokenDefinition(), tokenTransformerList);

        Map<Class<? extends Attribute>, String> row;
        TokenGeneratorResult tokenGeneratorResult;

        int rowCounter = 0;
        Map<String, Long> invalidAttributeCount = new HashMap<>();

        while (reader.hasNext()) {
            long recordStartTime = System.currentTimeMillis();

            row = reader.next();
            rowCounter++;

            tokenGeneratorResult = tokenGenerator.getAllTokens(row);
            logger.debug("Tokens: {}", tokenGeneratorResult.getTokens());

            keepTrackOfInvalidAttributes(tokenGeneratorResult, rowCounter,
                    invalidAttributeCount);

            writeTokens(writer, row, rowCounter, tokenGeneratorResult);

            // Collect demographic metrics if collector is provided
            if (metricsCollector != null) {
                long processingTime = System.currentTimeMillis() - recordStartTime;
                boolean tokenGenerationSuccessful = !tokenGeneratorResult.getTokens().isEmpty();

                // Convert row to string map for metrics collection
                Map<String, String> recordData = convertRowToStringMap(row);
                metricsCollector.recordPersonMetrics(recordData, processingTime, tokenGenerationSuccessful);
            }

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
        logger.info(String.format("Total number of records with invalid attributes: %,d", rowIssueCounter));

        // Generate and log bias detection report if metrics collector is provided
        if (metricsCollector != null) {
            logger.info("=== DEMOGRAPHIC BIAS DETECTION REPORT ===");
            String biasReport = metricsCollector.generateBiasReport();
            logger.info("\n{}", biasReport);
        }
    }

    private static void writeTokens(PersonAttributesWriter writer, Map<Class<? extends Attribute>, String> row,
            int rowCounter, TokenGeneratorResult tokenGeneratorResult) {

        Set<String> tokenIds = new TreeSet<>(tokenGeneratorResult.getTokens().keySet());

        for (String tokenId : tokenIds) {
            var rowResult = new HashMap<String, String>();
            rowResult.put(RECORD_ID, row.get(RecordIdAttribute.class));
            rowResult.put(RULE_ID, tokenId);
            rowResult.put(TOKEN, tokenGeneratorResult.getTokens().get(tokenId));

            try {
                writer.writeAttributes(rowResult);
            } catch (IOException e) {
                logger.error(String.format("Error writing attributes to file for row %,d", rowCounter), e);
            }
        }
    }

    private static void keepTrackOfInvalidAttributes(TokenGeneratorResult tokenGeneratorResult, int rowCounter,
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

    /**
     * Converts the row map from Class keys to String keys for metrics collection.
     * 
     * @param row The row data with Class keys
     * @return Map with string keys matching attribute names
     */
    private static Map<String, String> convertRowToStringMap(Map<Class<? extends Attribute>, String> row) {
        Map<String, String> stringMap = new HashMap<>();

        // Convert each known attribute class to its string key
        if (row.containsKey(RecordIdAttribute.class)) {
            stringMap.put("RecordId", row.get(RecordIdAttribute.class));
        }
        if (row.containsKey(FirstNameAttribute.class)) {
            stringMap.put("FirstName", row.get(FirstNameAttribute.class));
        }
        if (row.containsKey(LastNameAttribute.class)) {
            stringMap.put("LastName", row.get(LastNameAttribute.class));
        }
        if (row.containsKey(SexAttribute.class)) {
            stringMap.put("Sex", row.get(SexAttribute.class));
        }
        if (row.containsKey(BirthDateAttribute.class)) {
            stringMap.put("BirthDate", row.get(BirthDateAttribute.class));
        }
        if (row.containsKey(PostalCodeAttribute.class)) {
            stringMap.put("PostalCode", row.get(PostalCodeAttribute.class));
        }
        if (row.containsKey(SocialSecurityNumberAttribute.class)) {
            stringMap.put("SocialSecurityNumber", row.get(SocialSecurityNumberAttribute.class));
        }

        return stringMap;
    }
}