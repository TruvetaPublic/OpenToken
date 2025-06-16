/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.io.csv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truveta.opentoken.io.PersonAttributesWriter;

/**
 * The PersonAttributeCSVWriter class is responsible for writing person
 * attributes to a CSV file.
 * It implements the {@link PersonAttributeWriter} interface.
 */
public class PersonAttributesCSVWriter implements PersonAttributesWriter {
    private static final Logger logger = LoggerFactory.getLogger(PersonAttributesCSVWriter.class);

    private final BufferedWriter fileWriter;
    private final CSVPrinter csvPrinter;
    private boolean headerWritten = false;
    private final Map<String, Object> metadata = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initialize the class with the output file in CSV format.
     * 
     * @param filePath the output file path
     * @throws IOException if an I/O error occurs
     */
    public PersonAttributesCSVWriter(String filePath) throws IOException {
        fileWriter = new BufferedWriter(new FileWriter(filePath));
        csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);

        metadata.put("java_version", System.getProperty("java.version"));
        metadata.put("library_revision", "1.0.0");
        metadata.put("output_format", "CSV");
    }

    @Override
    public void writeAttributes(Map<String, String> data) {

        try {
            if (!headerWritten) {
                // Write the header
                csvPrinter.printRecord(data.keySet());
                headerWritten = true;
            }

            csvPrinter.printRecord(data.values());

        } catch (IOException e) {
            logger.error("Error in writing CSV file: {}", e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        this.csvPrinter.close();
        this.fileWriter.close();
    }

    @Override
    public void setMetadataFields(int totalRows, Long invalidAttributeCount,
            Map<String, Long> invalidAttributesByType) throws IOException {
        metadata.put("total_rows", totalRows);
        metadata.put("total_rows_with_invalid_attributes", invalidAttributeCount);
        metadata.put("invalid_attributes_by_type", new HashMap<>(invalidAttributesByType));
        fileWriter.write("# " + objectMapper.writeValueAsString(metadata) + "\n");
    }
}
