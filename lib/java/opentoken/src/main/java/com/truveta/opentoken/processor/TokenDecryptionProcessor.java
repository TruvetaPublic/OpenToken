/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.processor;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.io.TokenReader;
import com.truveta.opentoken.io.TokenWriter;
import com.truveta.opentoken.tokens.Token;
import com.truveta.opentoken.tokentransformer.DecryptTokenTransformer;

/**
 * Process encrypted tokens for decryption.
 * <p>
 * This class is used to read encrypted tokens from input source,
 * decrypt them, and write the decrypted tokens to the output data source.
 */
public final class TokenDecryptionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TokenDecryptionProcessor.class);

    private TokenDecryptionProcessor() {
    }

    /**
     * Reads encrypted tokens from the input data source, decrypts them, and
     * writes the result back to the output data source.
     * 
     * @param reader    TokenReader providing encrypted token rows
     * @param writer    TokenWriter for output
     * @param decryptor The decryption transformer
     * @throws IOException if an I/O error occurs
     */
    public static void process(TokenReader reader,
                                TokenWriter writer,
                                DecryptTokenTransformer decryptor) throws IOException {
        long rowCounter = 0;
        long decryptedCounter = 0;
        long errorCounter = 0;

        while (reader.hasNext()) {
            Map<String, String> row = reader.next();
            rowCounter++;
            
            String token = row.get(TokenConstants.TOKEN);
            
            // Decrypt the token if it's not blank
            if (token != null && !token.isEmpty() && !Token.BLANK.equals(token)) {
                try {
                    String decryptedToken = decryptor.transform(token);
                    row.put(TokenConstants.TOKEN, decryptedToken);
                    decryptedCounter++;
                } catch (Exception e) {
                    logger.error("Failed to decrypt token for RecordId {}, RuleId {}: {}", 
                               row.get(TokenConstants.RECORD_ID), row.get(TokenConstants.RULE_ID), e.getMessage());
                    errorCounter++;
                    // Keep the encrypted token in case of error
                }
            }
            
            // Write token
            writer.writeToken(row);

            if (rowCounter % 10000 == 0) {
                logger.info(String.format("Processed \"%,d\" tokens", rowCounter));
            }
        }

        logger.info(String.format("Processed a total of %,d tokens", rowCounter));
        logger.info(String.format("Successfully decrypted %,d tokens", decryptedCounter));
        if (errorCounter > 0) {
            logger.warn(String.format("Failed to decrypt %,d tokens", errorCounter));
        }
    }
}

