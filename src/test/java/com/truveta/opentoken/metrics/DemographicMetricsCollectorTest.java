/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

class DemographicMetricsCollectorTest {

    private DemographicMetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new DemographicMetricsCollector();
    }

    @Test
    void testRecordPersonMetrics_ValidData() {
        Map<String, String> recordData = new HashMap<>();
        recordData.put("Sex", "Male");
        recordData.put("BirthDate", "1990-05-15");
        recordData.put("PostalCode", "12345");

        collector.recordPersonMetrics(recordData, 100L, true);

        BiasDetectionMetrics metrics = collector.getMetrics();

        assertEquals(1L, metrics.getTotalRecordsProcessed());
        assertEquals(1L, metrics.getTotalRecordsSuccessful());
        assertEquals(0L, metrics.getTotalRecordsFailed());
        assertEquals(1.0, metrics.getOverallSuccessRate());

        assertTrue(metrics.getSexDistribution().containsKey("Male"));
        assertEquals(1L, metrics.getSexDistribution().get("Male").longValue());

        assertTrue(metrics.getAgeGroupDistribution().containsKey("19-35"));
        assertEquals(1L, metrics.getAgeGroupDistribution().get("19-35").longValue());

        assertTrue(metrics.getGeographicDistribution().containsKey("US-Northeast"));
        assertEquals(1L, metrics.getGeographicDistribution().get("US-Northeast").longValue());
    }

    @Test
    void testRecordPersonMetrics_InvalidData() {
        Map<String, String> recordData = new HashMap<>();
        recordData.put("Sex", "Invalid");
        recordData.put("BirthDate", "invalid-date");
        recordData.put("PostalCode", "invalid");

        collector.recordPersonMetrics(recordData, 50L, false);

        BiasDetectionMetrics metrics = collector.getMetrics();

        assertEquals(1L, metrics.getTotalRecordsProcessed());
        assertEquals(0L, metrics.getTotalRecordsSuccessful());
        assertEquals(1L, metrics.getTotalRecordsFailed());
        assertEquals(0.0, metrics.getOverallSuccessRate());

        assertTrue(metrics.getSexDistribution().containsKey("Unknown"));
        assertTrue(metrics.getAgeGroupDistribution().containsKey("Unknown"));
        assertTrue(metrics.getGeographicDistribution().containsKey("Other"));
    }

    @Test
    void testRecordPersonMetrics_CanadianPostalCode() {
        Map<String, String> recordData = new HashMap<>();
        recordData.put("Sex", "Female");
        recordData.put("BirthDate", "1985-12-01");
        recordData.put("PostalCode", "K1A 0A9");

        collector.recordPersonMetrics(recordData, 75L, true);

        BiasDetectionMetrics metrics = collector.getMetrics();

        assertTrue(metrics.getGeographicDistribution().containsKey("CA-Ontario"));
        assertEquals(1L, metrics.getGeographicDistribution().get("CA-Ontario").longValue());
    }

    @Test
    void testRecordPersonMetrics_FutureDate() {
        Map<String, String> recordData = new HashMap<>();
        recordData.put("Sex", "Female");
        recordData.put("BirthDate", "2050-01-01");
        recordData.put("PostalCode", "90210");

        collector.recordPersonMetrics(recordData, 60L, true);

        BiasDetectionMetrics metrics = collector.getMetrics();

        assertTrue(metrics.getAgeGroupDistribution().containsKey("Future"));
        assertEquals(1L, metrics.getAgeGroupDistribution().get("Future").longValue());
    }

    @Test
    void testRecordPersonMetrics_MultipleRecords() {
        // Record 1: Male, 30 years old, US West
        Map<String, String> record1 = new HashMap<>();
        record1.put("Sex", "Male");
        record1.put("BirthDate", "1994-01-01");
        record1.put("PostalCode", "90210");
        collector.recordPersonMetrics(record1, 100L, true);

        // Record 2: Female, 45 years old, US Northeast
        Map<String, String> record2 = new HashMap<>();
        record2.put("Sex", "Female");
        record2.put("BirthDate", "1979-06-15");
        record2.put("PostalCode", "10001");
        collector.recordPersonMetrics(record2, 120L, true);

        // Record 3: Failed record
        Map<String, String> record3 = new HashMap<>();
        record3.put("Sex", "");
        record3.put("BirthDate", "");
        record3.put("PostalCode", "");
        collector.recordPersonMetrics(record3, 50L, false);

        BiasDetectionMetrics metrics = collector.getMetrics();

        assertEquals(3L, metrics.getTotalRecordsProcessed());
        assertEquals(2L, metrics.getTotalRecordsSuccessful());
        assertEquals(1L, metrics.getTotalRecordsFailed());
        assertEquals(2.0 / 3.0, metrics.getOverallSuccessRate(), 0.001);

        // Check distributions
        assertEquals(1L, metrics.getSexDistribution().get("Male").longValue());
        assertEquals(1L, metrics.getSexDistribution().get("Female").longValue());
        assertEquals(1L, metrics.getSexDistribution().get("Unknown").longValue());

        assertEquals(1L, metrics.getAgeGroupDistribution().get("19-35").longValue());
        assertEquals(1L, metrics.getAgeGroupDistribution().get("36-50").longValue());
        assertEquals(1L, metrics.getAgeGroupDistribution().get("Unknown").longValue());
    }

    @Test
    void testGenerateBiasReport() {
        Map<String, String> recordData = new HashMap<>();
        recordData.put("Sex", "Male");
        recordData.put("BirthDate", "1990-05-15");
        recordData.put("PostalCode", "12345");

        collector.recordPersonMetrics(recordData, 100L, true);

        String report = collector.generateBiasReport();

        assertNotNull(report);
        assertTrue(report.contains("DEMOGRAPHIC BIAS DETECTION REPORT"));
        assertTrue(report.contains("OVERALL STATISTICS"));
        assertTrue(report.contains("Total Records Processed: 1"));
        assertTrue(report.contains("DEMOGRAPHIC DISTRIBUTIONS"));
        assertTrue(report.contains("Sex Distribution"));
        assertTrue(report.contains("Male: 1 (100.00%)"));
        assertTrue(report.contains("POTENTIAL BIAS INDICATORS"));
    }

    @Test
    void testBiasDetection_WithSignificantDisparity() {
        // Create records with 100% success rate for one group
        for (int i = 0; i < 15; i++) {
            Map<String, String> record = new HashMap<>();
            record.put("Sex", "Male");
            record.put("BirthDate", "1990-01-01");
            record.put("PostalCode", "12345");
            collector.recordPersonMetrics(record, 100L, true);
        }

        // Create records with 50% success rate for another group
        for (int i = 0; i < 10; i++) {
            Map<String, String> record = new HashMap<>();
            record.put("Sex", "Female");
            record.put("BirthDate", "1990-01-01");
            record.put("PostalCode", "12345");
            collector.recordPersonMetrics(record, 100L, true);
        }
        for (int i = 0; i < 10; i++) {
            Map<String, String> record = new HashMap<>();
            record.put("Sex", "Female");
            record.put("BirthDate", "1990-01-01");
            record.put("PostalCode", "12345");
            collector.recordPersonMetrics(record, 100L, false);
        }

        String report = collector.generateBiasReport();

        // Should detect bias due to significant difference in success rates
        assertTrue(report.contains("WARNING"));
    }
}