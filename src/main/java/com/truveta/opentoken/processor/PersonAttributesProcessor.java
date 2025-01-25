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
public final class PersonAttributesProcessor {

    private static final String TOKEN = "Token";
    private static final String RULE_ID = "RuleId";
    private static final String RECORD_ID = "RecordId";

    private static final Logger logger = LoggerFactory.getLogger(PersonAttributesProcessor.class.getName());

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

        // TokenGenerator code
        TokenGenerator tokenGenerator = new TokenGenerator(new TokenDefinition(), tokenTransformerList);

        Map<String, String> row;
        Map<String, String> tokens;
        Set<String> invalidAttributes;

        int rowCounter = 0;
        int rowIssueCounter = 0;

        while (reader.hasNext()) {
            row = reader.next();
            rowCounter++;

            tokens = tokenGenerator.getAllTokens(row);
            logger.debug("Tokens: {}", tokens);

            invalidAttributes = tokenGenerator.getInvalidPersonAttributes(row);
            if (!invalidAttributes.isEmpty()) {
                logger.info("Invalid Attributes for row {}: {}", String.format("%,d", rowCounter),
                        invalidAttributes);
                rowIssueCounter++;
            }
            List<String> tokenIds = tokens.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            for (String tokenId : tokenIds) {
                var rowResult = new HashMap<String, String>();
                rowResult.put(RECORD_ID, row.get(RECORD_ID));
                rowResult.put(RULE_ID, tokenId);
                rowResult.put(TOKEN, tokens.get(tokenId));

                try {
                    writer.writeAttributes(rowResult);
                } catch (IOException e) {
                    logger.error("Error writing attributes to file for row {}",
                            String.format("%,d", rowCounter), e);
                }
            }

            if (rowCounter % 10000 == 0) {
                logger.info("Processed {} records", String.format("%,d", rowCounter));
            }
        }

        logger.info("Processed a total of {} records", String.format("%,d", rowCounter));
        logger.info("Total number of records with issues: {}", String.format("%,d", rowIssueCounter));
    }
}