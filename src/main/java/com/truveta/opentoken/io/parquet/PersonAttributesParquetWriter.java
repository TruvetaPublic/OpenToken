/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.column.ParquetProperties.WriterVersion;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

import com.truveta.opentoken.io.PersonAttributesWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;

/**
 * The PersonAttributeParquetWriter class is responsible for writing person
 * attributes to a Parquet file.
 * It implements the {@link PersonAttributeWriter} interface.
 */
public class PersonAttributesParquetWriter implements PersonAttributesWriter {
    private ParquetWriter<Group> writer;
    private MessageType schema;
    private final String filepath;
    private final Configuration conf;
    private boolean initialized = false;
    private final Map<String, String> metadata = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initialize the class with the output file in Parquet format.
     * 
     * @param filePath the output file path
     * @throws IOException if an I/O error occurs
     */
    public PersonAttributesParquetWriter(String filepath) throws IOException {
        this.filepath = filepath;
        this.conf = new Configuration();
    }

    @Override
    public void writeAttributes(Map<String, String> attributes) throws IOException {
        if (!initialized) {
            initializeWriter(attributes);
        }

        SimpleGroup group = new SimpleGroup(schema);
        attributes.forEach((key, value) -> {
            if (schema.containsField(key) && value != null) {
                group.add(key, value);
            }
        });

        writer.write(group);
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    private void initializeWriter(Map<String, String> firstRecord) throws IOException {
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("message Person {\n");

        firstRecord.forEach((key, value) -> {
            if (value != null) {
                schemaBuilder.append("  required binary ").append(key).append(" (UTF8);\n");
            }
        });

        schemaBuilder.append("}");

        this.schema = MessageTypeParser.parseMessageType(schemaBuilder.toString());
        GroupWriteSupport.setSchema(schema, conf);
        Path path = new Path(filepath);

        writer = ExampleParquetWriter.builder(path)
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                .withWriterVersion(WriterVersion.PARQUET_2_0)
                .withConf(conf)
                .withType(schema)
                .build();

        initialized = true;
    }

    @Override
    public void setMetadataFields(int rowCount, Long invalidAttributeCount, Map<String, Long> invalidAttributesByType) throws IOException {
        metadata.put("java_version", System.getProperty("java.version"));
        metadata.put("library_revision", "1.0.0");
        metadata.put("output_format", "Parquet");
        metadata.put("total_rows", String.valueOf(rowCount));
        metadata.put("total_rows_with_invalid_attributes", String.valueOf(invalidAttributeCount));
        metadata.put("invalid_attributes_by_type", invalidAttributesByType.toString());
        
        Files.write(
            Paths.get(filepath + ".metadata.json"),
            objectMapper.writeValueAsBytes(metadata)
            );
        
    }
}
