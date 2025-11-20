/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.parquet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.schema.GroupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.io.TokenReader;
import com.truveta.opentoken.processor.TokenConstants;

/**
 * Reads encrypted tokens from a Parquet file for decryption.
 * Expected columns: RuleId, Token, RecordId
 */
public class TokenParquetReader implements TokenReader {
    private static final Logger logger = LoggerFactory.getLogger(TokenParquetReader.class);
    
    private final ParquetReader<Group> reader;
    private Group currentGroup;
    private boolean hasNextCalled = false;
    private boolean hasNextResult = false;
    
    /**
     * Initialize the class with the input file in Parquet format.
     * 
     * @param filePath The input file path.
     * @throws IOException If an I/O error occurs.
     */
    public TokenParquetReader(String filePath) throws IOException {
        Configuration conf = new Configuration();
        Path path = new Path(filePath);
        
        GroupReadSupport readSupport = new GroupReadSupport();
        this.reader = ParquetReader.builder(readSupport, path).withConf(conf).build();
        
        // Read the first group to validate schema
        currentGroup = reader.read();
        if (currentGroup != null) {
            GroupType schema = currentGroup.getType();
            
            // Validate required columns
            boolean hasRuleId = false;
            boolean hasToken = false;
            boolean hasRecordId = false;
            
            for (org.apache.parquet.schema.Type field : schema.getFields()) {
                String fieldName = field.getName();
                if (TokenConstants.RULE_ID.equals(fieldName)) {
                    hasRuleId = true;
                } else if (TokenConstants.TOKEN.equals(fieldName)) {
                    hasToken = true;
                } else if (TokenConstants.RECORD_ID.equals(fieldName)) {
                    hasRecordId = true;
                }
            }
            
            if (!hasRuleId || !hasToken || !hasRecordId) {
                throw new IOException("Missing required columns: RuleId, Token, or RecordId");
            }
            
            hasNextCalled = true;
            hasNextResult = true;
        }
    }
    
    @Override
    public boolean hasNext() {
        if (hasNextCalled) {
            return hasNextResult;
        }
        
        try {
            currentGroup = reader.read();
            hasNextResult = currentGroup != null;
            hasNextCalled = true;
            return hasNextResult;
        } catch (IOException e) {
            logger.error("Error reading next group", e);
            hasNextResult = false;
            hasNextCalled = true;
            return false;
        }
    }
    
    @Override
    public Map<String, String> next() {
        if (!hasNextCalled || !hasNextResult) {
            if (!hasNext()) {
                throw new NoSuchElementException("No more rows available");
            }
        }
        
        hasNextCalled = false;
        
        Map<String, String> row = new HashMap<>();
        GroupType schema = currentGroup.getType();
        
        for (org.apache.parquet.schema.Type field : schema.getFields()) {
            String fieldName = field.getName();
            try {
                String value = currentGroup.getString(fieldName, 0);
                row.put(fieldName, value != null ? value : "");
            } catch (Exception e) {
                row.put(fieldName, "");
            }
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
