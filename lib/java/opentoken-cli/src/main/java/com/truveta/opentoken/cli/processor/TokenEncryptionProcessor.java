/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli.processor;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.cli.io.TokenReader;
import com.truveta.opentoken.cli.io.TokenWriter;
import com.truveta.opentoken.tokens.Token;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;

/**
 * Process hashed tokens for encryption.
 * <p>
 * This class is used to read hashed tokens from input source,
 * encrypt them, and write the encrypted tokens to the output data source.
 */
public final class TokenEncryptionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TokenEncryptionProcessor.class);

    private TokenEncryptionProcessor() {
    }

    /**
     * Reads hashed tokens from the input data source, encrypts them, and
     * writes the result back to the output data source.
     * 
     * @param reader    TokenReader providing hashed token rows
     * @param writer    TokenWriter for output
     * @param encryptor The encryption transformer
     * @throws IOException if an I/O error occurs
     */
    public static void process(TokenReader reader,
                                TokenWriter writer,
                                EncryptTokenTransformer encryptor) throws IOException {
        long rowCounter = 0;
        long encryptedCounter = 0;
        long errorCounter = 0;

        while (reader.hasNext()) {
            Map<String, String> row = reader.next();
            rowCounter++;
            
            String token = row.get(TokenConstants.TOKEN);
            
            // Encrypt the token if it's not blank
            if (token != null && !token.isEmpty() && !Token.BLANK.equals(token)) {
                try {
                    String encryptedToken = encryptor.transform(token);
                    row.put(TokenConstants.TOKEN, encryptedToken);
                    encryptedCounter++;
                } catch (Exception e) {
                    logger.error("Failed to encrypt token for RecordId {}, RuleId {}: {}", 
                               row.get(TokenConstants.RECORD_ID), row.get(TokenConstants.RULE_ID), e.getMessage());
                    errorCounter++;
                    // Keep the hashed token in case of error
                }
            }
            
            // Write token
            writer.writeToken(row);

            if (rowCounter % 10000 == 0) {
                logger.info(String.format("Processed \"%,d\" tokens", rowCounter));
            }
        }

        logger.info(String.format("Processed a total of %,d tokens", rowCounter));
        logger.info(String.format("Successfully encrypted %,d tokens", encryptedCounter));
        if (errorCounter > 0) {
            logger.warn(String.format("Failed to encrypt %,d tokens", errorCounter));
        }
    }
}
