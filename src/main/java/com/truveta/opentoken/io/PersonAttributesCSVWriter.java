/**
 * Copyright (c) Truveta. All rights reserved.
 * The PersonAttributeCSVWriter class is responsible for writing person attributes to a CSV file.
 * It implements the PersonAttributeWriter interface.
 */
package com.truveta.opentoken.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A person attributes writer class for the output source in CSV format.
 */
public class PersonAttributesCSVWriter implements PersonAttributesWriter {
    private static final Logger logger = LoggerFactory.getLogger(PersonAttributesCSVReader.class.getName());
    private final String filePath;

    /**
     * Initialize the class with the output file in CSV format.
     * @param filePath the output file path
     */
    public PersonAttributesCSVWriter(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void writeAttributes(List<Map<String, String>> data) throws IOException {
        FileWriter fileWriter = null;
        CSVPrinter csvPrinter = null;

        try {
            fileWriter = new FileWriter(filePath);
            csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);

            // Write the header
            csvPrinter.printRecord(data.get(0).keySet());

            // Write the rows
            for (Map<String, String> row : data) {
                csvPrinter.printRecord(row.values());
            }

            logger.info("CSV file written successfully.");
        } catch (IOException e) {
            logger.error("Error in writing CSV file: " + e.getMessage());
        } finally {
            try {
                if (csvPrinter != null) {
                    csvPrinter.flush();
                    csvPrinter.close();
                }
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                logger.error("Error in closing the file writer: " + e.getMessage());
            }
        }
    }
}
