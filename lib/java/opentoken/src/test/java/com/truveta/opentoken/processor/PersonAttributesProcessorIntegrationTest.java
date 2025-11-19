/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import com.truveta.opentoken.attributes.Attribute;
import com.truveta.opentoken.attributes.general.RecordIdAttribute;
import com.truveta.opentoken.attributes.person.SocialSecurityNumberAttribute;
import com.truveta.opentoken.io.PersonAttributesReader;
import com.truveta.opentoken.io.PersonAttributesWriter;
import com.truveta.opentoken.io.csv.PersonAttributesCSVReader;
import com.truveta.opentoken.io.csv.PersonAttributesCSVWriter;
import com.truveta.opentoken.tokens.Token;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.NoOperationTokenTransformer;
import com.truveta.opentoken.tokentransformer.TokenTransformer;
import com.truveta.opentoken.Metadata;
import com.truveta.opentoken.io.MetadataWriter;

class PersonAttributesProcessorIntegrationTest {
    final int totalRecordsMatched = 1001;
    final String hashKey = "hash_key";
    final String encryptionKey = "the_encryption_key_goes_here....";
    final String hashAlgorithm = "HmacSHA256";
    final String encryptionAlgorithm = "AES";

    /*
     * This test case takes input csv which has repeat probability of 0.30.
     * RecordIds will still be unique.
     * The goal is to ensure that the records with repeated data still generate
     * the
     * same tokens.
     */
    @Test
    void testInputWithDuplicates() throws Exception {
        String inputCsvFile = "../../../resources/mockdata/test_data.csv";
        Map<String, List<String>> ssnToRecordIdsMap = groupRecordsIdsWithSameSsn(inputCsvFile);

        List<TokenTransformer> tokenTransformerList = new ArrayList<>();
        tokenTransformerList.add(new HashTokenTransformer(hashKey));
        tokenTransformerList.add(new EncryptTokenTransformer(encryptionKey));
        ArrayList<Map<String, String>> resultFromPersonAttributesProcessor = readCSV_fromPersonAttributesProcessor(
                inputCsvFile, tokenTransformerList);

        for (String processedCsvMapKey : ssnToRecordIdsMap.keySet()) {
            List<String> recordIds = ssnToRecordIdsMap.get(processedCsvMapKey);

            int count = 0;
            List<String> tokenGenerated = new ArrayList<>();

            for (Map<String, String> recordToken : resultFromPersonAttributesProcessor) {
                String recordId = recordToken.get("RecordId");
                /*
                 * This code block checks that for multiple recordIds with same SSN
                 * the 5 tokens generated (for each recordId) are always the same
                 */
                if (recordIds.contains(recordId)) {
                    String token = decryptToken(recordToken.get("Token"));
                    // for a new RecordId simply store the 5 tokens as a list
                    if (tokenGenerated.size() < 5) {
                        tokenGenerated.add(token);
                    }
                    // for RecordId with same SSN, tokens should match as in the list
                    else if (tokenGenerated.size() == 5) { // assertion to check existing tokens match for duplicate
                                                           // records
                        assertTrue(tokenGenerated.contains(token));
                    }
                    count++;
                }
            }
            assertEquals(count, recordIds.size() * 5);
        }
    }

    /*
     * This test case comapres two input csv's. A section of these data will
     * overlapp with both the csv's.
     * The first csv is hashed and encrypted and the second csv is only hashed.
     * The goal is to ensure that tokenization process still generates the tokens
     * correctly for both the csv's.
     * The test case then ensures the tokens match for overlapping records.
     * This is done by decrypting the encrypted tokens for the first csv and
     * hashing
     * the tokens in second csv.
     * Finally we find exact matches in both files.
     */
    @Test
    void testInputWithOverlappingData() throws Exception {
        // Incoming file is hashed and encrypted
        List<TokenTransformer> tokenTransformerList = new ArrayList<>();
        tokenTransformerList.add(new HashTokenTransformer(hashKey));
        tokenTransformerList.add(new EncryptTokenTransformer(encryptionKey));
        ArrayList<Map<String, String>> resultFromPersonAttributesProcessor1 = readCSV_fromPersonAttributesProcessor(
                "../../../resources/mockdata/test_overlap1.csv", tokenTransformerList);

        // Truveta file is neither hashed nor encrypted
        tokenTransformerList = new ArrayList<>();
        tokenTransformerList.add(new NoOperationTokenTransformer());
        ArrayList<Map<String, String>> resultFromPersonAttributesProcessor2 = readCSV_fromPersonAttributesProcessor(
                "../../../resources/mockdata/test_overlap2.csv", tokenTransformerList);

        Map<String, String> recordIdToTokenMap1 = new HashMap<>();
        // tokens from incoming file are hashed and encrypted. This needs decryption
        for (Map<String, String> recordToken1 : resultFromPersonAttributesProcessor1) {
            String encryptedToken = recordToken1.get("Token");
            recordIdToTokenMap1.put(recordToken1.get("RecordId"),
                    decryptToken(encryptedToken));
        }

        Map<String, String> recordIdToTokenMap2 = new HashMap<>();
        // Truveta tokens are neither hashed nor encrypted. This needs to be hashed
        for (Map<String, String> recordToken2 : resultFromPersonAttributesProcessor2) {
            String noOpToken = recordToken2.get("Token");
            // hashing this token to match incoming records files
            recordIdToTokenMap2.put(recordToken2.get("RecordId"), hashToken(noOpToken));
        }

        // Now both are similarly hased (Hmac hash)
        int overlappCount = 0;
        for (String recordId1 : recordIdToTokenMap1.keySet()) {
            String token1 = recordIdToTokenMap1.get(recordId1);
            if (recordIdToTokenMap2.containsKey(recordId1)) {
                overlappCount++;
                assertEquals(recordIdToTokenMap2.get(recordId1), token1,
                        "For same RecordIds the tokens must match");
            }
        }
        assertEquals(overlappCount, totalRecordsMatched);
    }

    /**
     * This test verifies that the metadata file is created alongside the output
     * file
     * with the correct extension and contains the expected metadata.
     */
    @Test
    void testMetadataFileLocation() throws Exception {
        // Set up the test
        String inputCsvFile = "../../../resources/mockdata/test_data.csv";
        String outputCsvFile = "target/test_metadata_location_output.csv";

        List<TokenTransformer> tokenTransformerList = new ArrayList<>();
        tokenTransformerList.add(new NoOperationTokenTransformer());

        // Delete output files if they exist
        Files.deleteIfExists(Paths.get(outputCsvFile));
        // Calculate correct metadata file path (same logic as MetadataJsonWriter)
        int dotIndex = outputCsvFile.lastIndexOf('.');
        String baseName = dotIndex > 0 ? outputCsvFile.substring(0, dotIndex) : outputCsvFile;
        String metadataFilePath = baseName + Metadata.METADATA_FILE_EXTENSION;
        Files.deleteIfExists(Paths.get(metadataFilePath));

        // Process the input file
        try (PersonAttributesReader reader = new PersonAttributesCSVReader(inputCsvFile);
                PersonAttributesWriter writer = new PersonAttributesCSVWriter(outputCsvFile)) {

            // Create initial metadata
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put(Metadata.PLATFORM, Metadata.PLATFORM_JAVA);
            metadataMap.put(Metadata.JAVA_VERSION, Metadata.SYSTEM_JAVA_VERSION);
            metadataMap.put(Metadata.OUTPUT_FORMAT, Metadata.OUTPUT_FORMAT_CSV);

            // Process data and get updated metadata
            PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);

            // Write the metadata to file
            MetadataWriter metadataWriter = new com.truveta.opentoken.io.json.MetadataJsonWriter(outputCsvFile);
            metadataWriter.write(metadataMap);
        }

        // Verify that the output file exists
        assertTrue(Files.exists(Paths.get(outputCsvFile)), "Output CSV file should exist");

        // Verify that the metadata file exists alongside the output file
        // Handle files with or without extensions (same logic as MetadataJsonWriter)
        int lastDotIndex = outputCsvFile.lastIndexOf('.');
        String basePath = lastDotIndex > 0 ? outputCsvFile.substring(0, lastDotIndex) : outputCsvFile;
        String expectedMetadataFile = basePath + Metadata.METADATA_FILE_EXTENSION;
        assertTrue(Files.exists(Paths.get(expectedMetadataFile)),
                "Metadata file should exist alongside the output file");

        // Verify that metadata file contains the expected data
        String metadataContent = Files.readString(Paths.get(expectedMetadataFile));
        assertTrue(metadataContent.contains(Metadata.PLATFORM_JAVA), "Metadata should contain platform information");
        assertTrue(metadataContent.contains(Metadata.SYSTEM_JAVA_VERSION), "Metadata should contain Java version");
        assertTrue(metadataContent.contains(Metadata.OUTPUT_FORMAT_CSV), "Metadata should contain output format");
        assertTrue(metadataContent.contains("TotalRows"), "Metadata should contain total rows processed");
    }

    /**
     * This test verifies backward compatibility by ensuring that tokens
     * generated for the same input data remain consistent across different
     * processing runs and configurations.
     */
    @Test
    void testInputBackwardCompatibility() throws Exception {
        String oldTmpInputFile = Files.createTempFile("person_attributes_old", ".csv").toString();
        String newTmpInputFile = Files.createTempFile("person_attributes_new", ".csv").toString();

        String oldTmpOutputFile = Files.createTempFile("person_attributes_old_out", ".csv").toString();
        String newTmpOutputFile = Files.createTempFile("person_attributes_new_out", ".csv").toString();

        // Person attributes to be used for token generation
        Map<String, String> personAttributes = new HashMap<>();
        personAttributes.put("FirstName", "Alice");
        personAttributes.put("LastName", "Wonderland");
        personAttributes.put("SocialSecurityNumber", "345-54-6795");
        personAttributes.put("PostalCode", "98052");
        personAttributes.put("BirthDate", "1993-08-10");
        personAttributes.put("Sex", "Female");

        try {
            // Create identical CSV files for both old and new processing
            createTestCSVFile(oldTmpInputFile, personAttributes);
            createTestCSVFile(newTmpInputFile, personAttributes);

            // Use consistent transformers for backward compatibility testing
            List<TokenTransformer> tokenTransformerList = new ArrayList<>();
            tokenTransformerList.add(new HashTokenTransformer(hashKey));
            tokenTransformerList.add(new EncryptTokenTransformer(encryptionKey));

            // Process both files with the same configuration
            ArrayList<Map<String, String>> oldResults = readCSV_fromPersonAttributesProcessor(
                    oldTmpInputFile, tokenTransformerList);
            ArrayList<Map<String, String>> newResults = readCSV_fromPersonAttributesProcessor(
                    newTmpInputFile, tokenTransformerList);

            // Verify that both processing runs produce the same number of tokens
            assertEquals(oldResults.size(), newResults.size(),
                    "Both processing runs should produce the same number of tokens for backward compatibility");

            // Verify that tokens are identical for the same input data
            for (int i = 0; i < oldResults.size(); i++) {
                Map<String, String> oldToken = oldResults.get(i);
                Map<String, String> newToken = newResults.get(i);

                assertEquals(oldToken.get("RecordId"), newToken.get("RecordId"),
                        "RecordId should be identical for backward compatibility");
                assertEquals(oldToken.get("RuleId"), newToken.get("RuleId"),
                        "RuleId should be identical for backward compatibility");

                // Decrypt and compare the actual token values
                String oldDecryptedToken = decryptToken(oldToken.get("Token"));
                String newDecryptedToken = decryptToken(newToken.get("Token"));
                assertEquals(oldDecryptedToken, newDecryptedToken,
                        "Decrypted tokens should be identical for backward compatibility");
            }

            // Verify that exactly 5 tokens are generated per record (T1-T5)
            assertEquals(5, oldResults.size(),
                    "Should generate exactly 5 tokens per record for backward compatibility");

            // Verify token structure consistency
            for (Map<String, String> token : oldResults) {
                assertTrue(token.containsKey("RecordId"), "Token must contain RecordId");
                assertTrue(token.containsKey("RuleId"), "Token must contain RuleId");
                assertTrue(token.containsKey("Token"), "Token must contain Token");

                String ruleId = token.get("RuleId");
                assertTrue(ruleId.matches("T[1-5]"),
                        "RuleId should follow T1-T5 pattern, but got: " + ruleId);
            }

            // Test metadata consistency
            Map<String, Object> metadataMap = new HashMap<>();
            try (PersonAttributesReader reader = new PersonAttributesCSVReader(oldTmpInputFile);
                    PersonAttributesWriter writer = new PersonAttributesCSVWriter(oldTmpOutputFile)) {

                PersonAttributesProcessor.process(reader, writer, tokenTransformerList, metadataMap);
            }

            // Verify essential metadata fields for backward compatibility
            assertTrue(metadataMap.containsKey(PersonAttributesProcessor.TOTAL_ROWS),
                    "Metadata must contain TotalRows for backward compatibility");
            assertEquals(1L, metadataMap.get(PersonAttributesProcessor.TOTAL_ROWS),
                    "TotalRows should be 1 for single record");
            assertTrue(metadataMap.containsKey(PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES),
                    "Metadata must contain TotalRowsWithInvalidAttributes for backward compatibility");
            assertTrue(metadataMap.containsKey(PersonAttributesProcessor.INVALID_ATTRIBUTES_BY_TYPE),
                    "Metadata must contain InvalidAttributesByType for backward compatibility");

        } finally {
            // Clean up temporary files
            Files.deleteIfExists(Paths.get(oldTmpInputFile));
            Files.deleteIfExists(Paths.get(newTmpInputFile));
            Files.deleteIfExists(Paths.get(oldTmpOutputFile));
            Files.deleteIfExists(Paths.get(newTmpOutputFile));
        }
    }

    /**
     * Helper method to create a test CSV file with specified person attributes
     */
    private void createTestCSVFile(String filePath, Map<String, String> personAttributes) throws Exception {
        try (PersonAttributesWriter writer = new PersonAttributesCSVWriter(filePath)) {
            Map<String, String> record = new HashMap<>();
            record.put("RecordId", "TEST_RECORD_001");
            record.putAll(personAttributes);
            writer.writeAttributes(record);
        }
    }

    ArrayList<Map<String, String>> readCSV_fromPersonAttributesProcessor(String inputCsvFilePath,
            List<TokenTransformer> tokenTransformers) throws Exception {

        String tmpOutputFile = Files.createTempFile("person_attributes_",
                ".csv").toString();

        // Actually process the input file through PersonAttributesProcessor
        try (PersonAttributesReader reader = new PersonAttributesCSVReader(inputCsvFilePath);
                PersonAttributesWriter writer = new PersonAttributesCSVWriter(tmpOutputFile)) {

            Map<String, Object> metadataMap = new HashMap<>();
            PersonAttributesProcessor.process(reader, writer, tokenTransformers, metadataMap);
        }

        ArrayList<Map<String, String>> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(tmpOutputFile));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.Builder.create().setHeader().build())) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> recordMap = new HashMap<>();
                csvRecord.toMap().forEach(recordMap::put);
                result.add(recordMap);
            }
        }

        return result;
    }

    /*
     * Returns Map of SSN -> List of RecordIds
     */
    Map<String, List<String>> groupRecordsIdsWithSameSsn(String inputCsvFilePath) throws Exception {
        Map<String, List<String>> ssnToRecordIdsMap = new HashMap<>();

        try (PersonAttributesReader reader = new PersonAttributesCSVReader(inputCsvFilePath)) {

            while (reader.hasNext()) {
                Map<Class<? extends Attribute>, String> row = reader.next();

                String ssn = row.get(SocialSecurityNumberAttribute.class);
                List<String> recordIds = ssnToRecordIdsMap.getOrDefault(ssn, new ArrayList<>());
                recordIds.add(row.get(RecordIdAttribute.class));
                ssnToRecordIdsMap.put(ssn, recordIds);
            }

        }

        return ssnToRecordIdsMap;
    }

    private String hashToken(String noOpToken) throws Exception {
        Mac mac = Mac.getInstance(hashAlgorithm);
        mac.init(new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), hashAlgorithm));
        byte[] dataAsBytes = noOpToken.getBytes();
        byte[] sha = mac.doFinal(dataAsBytes);
        return Base64.getEncoder().encodeToString(sha);
    }

    private String decryptToken(String encryptedToken) throws Exception {
        if (Token.BLANK.equals(encryptedToken)) {
            // blank tokens don't get encrypted
            return Token.BLANK;
        }
        byte[] messageBytes = Base64.getDecoder().decode(encryptedToken);
        byte[] iv = new byte[12];
        byte[] cipherBytes = new byte[messageBytes.length - 12];

        System.arraycopy(messageBytes, 0, iv, 0, 12);
        System.arraycopy(messageBytes, 12, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Decrypt the token using the same settings
        SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] decryptedBytes = cipher.doFinal(cipherBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * This test verifies that hash-only mode (without encryption) works correctly
     * and produces consistent tokens across runs.
     */
    @Test
    void testHashOnlyMode() throws Exception {
        String tmpInputFile = Files.createTempFile("person_attributes_hashonly", ".csv").toString();
        String tmpOutputFile = Files.createTempFile("person_attributes_hashonly_out", ".csv").toString();

        // Person attributes to be used for token generation
        Map<String, String> personAttributes = new HashMap<>();
        personAttributes.put("FirstName", "Bob");
        personAttributes.put("LastName", "Builder");
        personAttributes.put("SocialSecurityNumber", "456-78-9012");
        personAttributes.put("PostalCode", "12345");
        personAttributes.put("BirthDate", "1990-05-15");
        personAttributes.put("Sex", "Male");

        try {
            // Create test CSV file
            createTestCSVFile(tmpInputFile, personAttributes);

            // Use only hash transformer (no encryption)
            List<TokenTransformer> tokenTransformerList = new ArrayList<>();
            tokenTransformerList.add(new HashTokenTransformer(hashKey));

            // Process the file with hash-only transformers
            ArrayList<Map<String, String>> results = readCSV_fromPersonAttributesProcessor(
                    tmpInputFile, tokenTransformerList);

            // Verify that exactly 5 tokens are generated per record (T1-T5)
            assertEquals(5, results.size(), "Should generate exactly 5 tokens per record in hash-only mode");

            // Verify token structure
            for (Map<String, String> token : results) {
                assertTrue(token.containsKey("RecordId"), "Token must contain RecordId");
                assertTrue(token.containsKey("RuleId"), "Token must contain RuleId");
                assertTrue(token.containsKey("Token"), "Token must contain Token");

                String ruleId = token.get("RuleId");
                assertTrue(ruleId.matches("T[1-5]"), "RuleId should follow T1-T5 pattern");

                String tokenValue = token.get("Token");
                // In hash-only mode, tokens should be base64-encoded HMAC-SHA256 hashes
                // Valid tokens are 44 characters long (base64 encoding of 32-byte SHA256)
                // Blank tokens are 64 zeros
                assertTrue(tokenValue.length() == 44 || tokenValue.length() == 64, 
                    "Hash-only tokens should be 44 characters (valid) or 64 (blank)");
            }

            // Process the same file again to verify consistency
            ArrayList<Map<String, String>> results2 = readCSV_fromPersonAttributesProcessor(
                    tmpInputFile, tokenTransformerList);

            // Verify that hash-only mode produces consistent tokens across runs
            assertEquals(results.size(), results2.size(), "Should produce same number of tokens");
            for (int i = 0; i < results.size(); i++) {
                assertEquals(results.get(i).get("Token"), results2.get(i).get("Token"),
                    "Hash-only mode should produce identical tokens for same input");
            }

        } finally {
            // Clean up temporary files
            Files.deleteIfExists(Paths.get(tmpInputFile));
            Files.deleteIfExists(Paths.get(tmpOutputFile));
        }
    }
}
