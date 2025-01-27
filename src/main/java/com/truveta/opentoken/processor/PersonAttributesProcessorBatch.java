/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.tokens.TokenDefinition;
import com.truveta.opentoken.tokens.TokenGenerator;
import com.truveta.opentoken.tokentransformer.TokenTransformer;

/**
 * Process all person attributes.
 * <p>
 * This class is used to read person attributes from input source,
 * generate tokens for each person record and write the tokens back
 * to the output data source.
 */
public final class PersonAttributesProcessorBatch {

    private static final String TOKEN = "Token";
    private static final String RULE_ID = "RuleId";
    private static final String RECORD_ID = "RecordId";
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_PARALLELISM = Runtime.getRuntime().availableProcessors();

    private static final Logger logger = LoggerFactory.getLogger(PersonAttributesProcessorBatch.class.getName());

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
        process(reader, writer, tokenTransformerList, DEFAULT_BATCH_SIZE, DEFAULT_PARALLELISM);
    }

    public static void process(PersonAttributesReader reader, PersonAttributesWriter writer,
            List<TokenTransformer> tokenTransformerList, int batchSize, int parallelism) {
        TokenGenerator tokenGenerator = new TokenGenerator(new TokenDefinition(), tokenTransformerList);

        List<Map<String, String>> batch = new ArrayList<>();
        AtomicInteger rowCounter = new AtomicInteger(0);
        AtomicInteger rowIssueCounter = new AtomicInteger(0);

        ForkJoinPool customThreadPool = new ForkJoinPool(parallelism);

        try {
            while (reader.hasNext()) {
                batch.add(reader.next());

                if (batch.size() >= batchSize) {
                    processBatch(batch, tokenGenerator, writer, rowCounter, rowIssueCounter);
                    batch = new ArrayList<>();
                }
            }

            // Process remaining records
            if (!batch.isEmpty()) {
                processBatch(batch, tokenGenerator, writer, rowCounter, rowIssueCounter);
            }

        } finally {
            customThreadPool.shutdown();
        }

        logger.info("Processed a total of {} records", String.format("%,d", rowCounter.get()));
        logger.info("Total number of records with issues: {}", String.format("%,d", rowIssueCounter.get()));
    }

    private static void processBatch(List<Map<String, String>> batch, TokenGenerator tokenGenerator,
            PersonAttributesWriter writer, AtomicInteger rowCounter, AtomicInteger rowIssueCounter) {
        batch.parallelStream().forEach(row -> {
            Map<String, String> tokens = tokenGenerator.getAllTokens(row);
            Set<String> invalidAttributes = tokenGenerator.getInvalidPersonAttributes(row);

            if (!invalidAttributes.isEmpty()) {
                logger.info("Invalid Attributes for row {}: {}",
                        String.format("%,d", rowCounter.incrementAndGet()),
                        invalidAttributes);
                rowIssueCounter.incrementAndGet();
            }

            List<String> tokenIds = tokens.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            List<Map<String, String>> rowResults = new ArrayList<>();
            for (String tokenId : tokenIds) {
                var rowResult = new HashMap<String, String>();
                rowResult.put(RECORD_ID, row.get(RECORD_ID));
                rowResult.put(RULE_ID, tokenId);
                rowResult.put(TOKEN, tokens.get(tokenId));
                rowResults.add(rowResult);
            }

            synchronized (writer) {
                try {
                    for (Map<String, String> rowResult : rowResults) {
                        writer.writeAttributes(rowResult);
                    }
                } catch (IOException e) {
                    logger.error("Error writing attributes to file for row {}",
                            String.format("%,d", rowCounter.get()), e);
                }
            }

            int currentCount = rowCounter.incrementAndGet();
            if (currentCount % 10000 == 0) {
                logger.info("Processed {} records", String.format("%,d", currentCount));
            }
        });
    }
}