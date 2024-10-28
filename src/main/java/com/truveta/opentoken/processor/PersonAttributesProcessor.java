// Copyright (c) Truveta. All rights reserved.
package com.truveta.opentoken.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final Logger logger = LoggerFactory.getLogger(PersonAttributesProcessor.class.getName());

    /**
     * Reads person attributes from the input data source, generates token, and
     * write the result back to the output data source. The tokens can be optionally
     * transformed before writing.
     * 
     * @param reader the reader initialized with the input data source.
     * @param writer the writer initialized with the output data source.
     * @param tokenTransformerList a list of token transformers.
     * 
     * @see com.truveta.opentoken.io.PersonAttributesReader PersonAttributesReader
     * @see com.truveta.opentoken.io.PersonAttributesWriter PersonAttributesWriter
     * @see com.truveta.opentoken.tokentransformer.TokenTransformer TokenTransformer
     */
    public static void process(PersonAttributesReader reader, PersonAttributesWriter writer, List<TokenTransformer> tokenTransformerList) { 
        writeAttributes(writer, readAttributes(reader, tokenTransformerList));  
    }

    /**
     * Reads person attributes from the input data source and generates tokens. The
     * tokens can be optionally transformed.
     * 
     * @param reader the reader initialized with the input data source.
     * @param tokenTransformerList a list of token transformers.
     * 
     * @see com.truveta.opentoken.io.PersonAttributesReader PersonAttributesReader
     * @see com.truveta.opentoken.tokentransformer.TokenTransformer TokenTransformer
     * 
     * @return a list of person attributes map ready to be written to the output data source.
     */
    public static ArrayList<Map<String, String>> readAttributes(PersonAttributesReader reader, List<TokenTransformer> tokenTransformerList) {
        // TokenGenerator code
        TokenGenerator tokenGenerator = new TokenGenerator(new TokenDefinition(), tokenTransformerList);

        var result = new ArrayList<Map<String, String>>();

        try {
            List<Map<String,String>> data = reader.readAttributes();
            data.forEach(row -> {
                Map<String, String> tokens = tokenGenerator.getAllTokens(row);
                Set<String> invalidAttributes = tokenGenerator.getInvalidPersonAttributes(row);
    
                List<String> tokenIds = new ArrayList<>(tokens.keySet());
                Collections.sort(tokenIds);
                for (String tokenId : tokenIds) {
                    var rowResult = new HashMap<String, String>();
                    rowResult.put("RecordId", row.get("RecordId"));
                    rowResult.put("RuleId", tokenId);
                    rowResult.put("Token", tokens.get(tokenId));
                    result.add(rowResult);
                }
                
                logger.info("Tokens: {}", tokens);
                if (!invalidAttributes.isEmpty()) {
                    logger.info("Invalid Attributes: {}", invalidAttributes);
                }
            });
        } catch (IOException e) {
            logger.error("Error reading attributes from file", e);
        }

        return result;
    }

    /**
     * Write the person attributes to the output data source.
     * 
     * @param writer the writer initialized with the output data source.
     * @param result a list of person attributes map.
     * 
     * @see com.truveta.opentoken.io.PersonAttributesWriter PersonAttributesWriter
     */
    public static void writeAttributes(PersonAttributesWriter writer, ArrayList<Map<String, String>> result) {
        try {
            writer.writeAttributes(result);
        } catch (IOException e) {
            logger.error("Error writing attributes to file", e);
        }
    }
}