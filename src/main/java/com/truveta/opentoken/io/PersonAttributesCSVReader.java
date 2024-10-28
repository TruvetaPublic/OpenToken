/**
 * Copyright (c) Truveta. All rights reserved.
 * Reads person attributes from a CSV file.
 * Implements the {@link PersonAttributeReader} interface and the {@link Iterable} interface.
 */
package com.truveta.opentoken.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A person attributes reader class for the input source in CSV format.
 */
public class PersonAttributesCSVReader implements PersonAttributesReader {
    private static final Logger logger = LoggerFactory.getLogger(PersonAttributesCSVReader.class.getName());
    private final String filePath;

    /**
     * Initialize the class with the input file in CSV format.
     * @param filePath the input file path
     */
    public PersonAttributesCSVReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<Map<String, String>> readAttributes() throws IOException {
        List<Map<String, String>> data = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(Paths.get(filePath));
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> row = new HashMap<>();
                for (String header : csvParser.getHeaderMap().keySet()) {
                    row.put(header, csvRecord.get(header));
                }
                data.add(row);
            }
        } catch (IOException e) {
            logger.error("Error reading CSV file", e);
            throw e;
        }

        return data;
    }
}
