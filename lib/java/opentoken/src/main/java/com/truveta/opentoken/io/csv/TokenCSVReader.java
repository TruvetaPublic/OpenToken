/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.csv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.io.TokenReader;
import com.truveta.opentoken.processor.TokenConstants;

/**
 * Reads encrypted tokens from a CSV file for decryption.
 * Expected columns: RuleId, Token, RecordId
 */
public class TokenCSVReader implements TokenReader {
    private static final Logger logger = LoggerFactory.getLogger(TokenCSVReader.class);
    
    private final BufferedReader reader;
    private final String[] headers;
    private String nextLine;
    
    /**
     * Initialize the class with the input file in CSV format.
     * 
     * @param filePath The input file path.
     * @throws IOException If an I/O error occurs.
     */
    public TokenCSVReader(String filePath) throws IOException {
        this.reader = new BufferedReader(new FileReader(filePath));
        
        // Read header line
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IOException("Empty CSV file");
        }
        
        this.headers = headerLine.split(",");
        
        // Validate required columns
        boolean hasRuleId = false;
        boolean hasToken = false;
        boolean hasRecordId = false;
        
        for (String header : headers) {
            String trimmed = header.trim();
            if (TokenConstants.RULE_ID.equals(trimmed)) {
                hasRuleId = true;
            } else if (TokenConstants.TOKEN.equals(trimmed)) {
                hasToken = true;
            } else if (TokenConstants.RECORD_ID.equals(trimmed)) {
                hasRecordId = true;
            }
        }
        
        if (!hasRuleId || !hasToken || !hasRecordId) {
            throw new IOException("Missing required columns: RuleId, Token, or RecordId");
        }
        
        // Read first data line
        this.nextLine = reader.readLine();
    }
    
    @Override
    public boolean hasNext() {
        return nextLine != null;
    }
    
    @Override
    public Map<String, String> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more rows available");
        }
        
        String currentLine = nextLine;
        try {
            nextLine = reader.readLine();
        } catch (IOException e) {
            logger.error("Error reading next line", e);
            nextLine = null;
        }
        
        // Parse the line
        String[] values = currentLine.split(",", -1);
        Map<String, String> row = new HashMap<>();
        
        for (int i = 0; i < headers.length && i < values.length; i++) {
            row.put(headers[i].trim(), values[i].trim());
        }
        
        return row;
    }
    
    @Override
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }
}
