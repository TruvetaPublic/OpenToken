/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.parquet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.truveta.opentoken.io.TokenWriter;
import com.truveta.opentoken.processor.TokenConstants;

/**
 * Writes decrypted tokens to a Parquet file.
 * Output columns: RuleId, Token, RecordId
 */
public class TokenParquetWriter implements TokenWriter {
    private static final Logger logger = LoggerFactory.getLogger(TokenParquetWriter.class);

    private final ParquetWriter<Group> writer;
    private final SimpleGroupFactory groupFactory;

    /**
     * Initialize the class with the output file in Parquet format.
     * 
     * @param filePath The output file path.
     * @throws IOException If an I/O error occurs.
     */
    public TokenParquetWriter(String filePath) throws IOException {
        // Create directory if it doesn't exist
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        // Define schema
        MessageType schema = Types.buildMessage()
                .required(PrimitiveType.PrimitiveTypeName.BINARY)
                .as(org.apache.parquet.schema.LogicalTypeAnnotation.stringType()).named(TokenConstants.RULE_ID)
                .required(PrimitiveType.PrimitiveTypeName.BINARY)
                .as(org.apache.parquet.schema.LogicalTypeAnnotation.stringType()).named(TokenConstants.TOKEN)
                .required(PrimitiveType.PrimitiveTypeName.BINARY)
                .as(org.apache.parquet.schema.LogicalTypeAnnotation.stringType()).named(TokenConstants.RECORD_ID)
                .named("TokenSchema");

        this.groupFactory = new SimpleGroupFactory(schema);

        Configuration conf = new Configuration();
        GroupWriteSupport.setSchema(schema, conf);

        this.writer = org.apache.parquet.hadoop.example.ExampleParquetWriter
                .builder(new org.apache.hadoop.fs.Path(filePath))
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withType(schema)
                .build();
    }

    /**
     * Write a token row to the Parquet file.
     * 
     * @param data A map containing RuleId, Token, and RecordId.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void writeToken(Map<String, String> data) throws IOException {
        String ruleId = data.getOrDefault(TokenConstants.RULE_ID, "");
        String token = data.getOrDefault(TokenConstants.TOKEN, "");
        String recordId = data.getOrDefault(TokenConstants.RECORD_ID, "");

        Group group = groupFactory.newGroup()
                .append(TokenConstants.RULE_ID, ruleId)
                .append(TokenConstants.TOKEN, token)
                .append(TokenConstants.RECORD_ID, recordId);

        writer.write(group);
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
