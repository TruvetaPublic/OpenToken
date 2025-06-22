/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.metrics;

import com.truveta.opentoken.attributes.person.BirthDateAttribute;
import com.truveta.opentoken.attributes.person.PostalCodeAttribute;
import com.truveta.opentoken.attributes.person.SexAttribute;

import java.util.Map;

/**
 * Collector for demographic bias detection metrics.
 * 
 * This class provides methods to collect and analyze demographic data
 * for bias detection in the OpenToken processing system.
 */
public class DemographicMetricsCollector {

    private final BiasDetectionMetrics metrics;
    private final SexAttribute sexAttribute;
    private final BirthDateAttribute birthDateAttribute;
    private final PostalCodeAttribute postalCodeAttribute;

    public DemographicMetricsCollector() {
        this.metrics = new BiasDetectionMetrics();
        this.sexAttribute = new SexAttribute();
        this.birthDateAttribute = new BirthDateAttribute();
        this.postalCodeAttribute = new PostalCodeAttribute();
    }

    /**
     * Records demographic information and validation results for a person record.
     * 
     * @param recordData                Map containing person attributes
     * @param processingTimeMs          Time taken to process this record
     * @param tokenGenerationSuccessful Whether token generation was successful
     */
    public void recordPersonMetrics(Map<String, String> recordData, long processingTimeMs,
            boolean tokenGenerationSuccessful) {
        // Extract and normalize demographic attributes
        String rawSex = recordData.get("Sex");
        String rawBirthDate = recordData.get("BirthDate");
        String rawPostalCode = recordData.get("PostalCode");

        String normalizedSex = DemographicUtils.normalizeSex(rawSex);
        String ageGroup = DemographicUtils.categorizeAgeGroup(rawBirthDate);
        String region = DemographicUtils.categorizeGeographicRegion(rawPostalCode);
        String demographicKey = DemographicUtils.createDemographicKey(normalizedSex, ageGroup, region);

        // Record distribution metrics
        metrics.incrementSexDistribution(normalizedSex);
        metrics.incrementAgeGroupDistribution(ageGroup);
        metrics.incrementGeographicDistribution(region);

        // Record validation metrics
        boolean sexValid = validateAttribute(sexAttribute, rawSex);
        boolean birthDateValid = validateAttribute(birthDateAttribute, rawBirthDate);
        boolean postalCodeValid = validateAttribute(postalCodeAttribute, rawPostalCode);

        metrics.recordSexValidation(normalizedSex, sexValid);
        metrics.recordAgeGroupValidation(ageGroup, birthDateValid);
        metrics.recordGeographicValidation(region, postalCodeValid);

        // Record token generation metrics
        metrics.recordTokenGeneration(demographicKey, tokenGenerationSuccessful);

        // Record processing time
        metrics.recordProcessingTime(demographicKey, processingTimeMs);

        // Update total counters
        metrics.incrementTotalRecords();
        if (tokenGenerationSuccessful) {
            metrics.incrementSuccessfulRecords();
        } else {
            metrics.incrementFailedRecords();
        }
    }

    /**
     * Generates a comprehensive bias detection report.
     * 
     * @return Formatted report string
     */
    public String generateBiasReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== DEMOGRAPHIC BIAS DETECTION REPORT ===\n\n");

        // Overall statistics
        report.append("OVERALL STATISTICS:\n");
        report.append(String.format("Total Records Processed: %d\n", metrics.getTotalRecordsProcessed()));
        report.append(String.format("Successful Records: %d\n", metrics.getTotalRecordsSuccessful()));
        report.append(String.format("Failed Records: %d\n", metrics.getTotalRecordsFailed()));
        report.append(String.format("Overall Success Rate: %.2f%%\n\n", metrics.getOverallSuccessRate() * 100));

        // Distribution analysis
        report.append("DEMOGRAPHIC DISTRIBUTIONS:\n");
        appendDistributionSection(report, "Sex Distribution", metrics.getSexDistribution());
        appendDistributionSection(report, "Age Group Distribution", metrics.getAgeGroupDistribution());
        appendDistributionSection(report, "Geographic Distribution", metrics.getGeographicDistribution());

        // Validation analysis
        report.append("VALIDATION SUCCESS RATES BY DEMOGRAPHIC:\n");
        appendValidationSection(report, "Sex Validation", metrics.getSexValidationSuccess(),
                metrics.getSexValidationFailure());
        appendValidationSection(report, "Age Group Validation", metrics.getAgeGroupValidationSuccess(),
                metrics.getAgeGroupValidationFailure());
        appendValidationSection(report, "Geographic Validation", metrics.getGeographicValidationSuccess(),
                metrics.getGeographicValidationFailure());

        // Token generation analysis
        report.append("TOKEN GENERATION SUCCESS RATES:\n");
        appendValidationSection(report, "Token Generation", metrics.getTokenGenerationSuccess(),
                metrics.getTokenGenerationFailure());

        // Performance analysis
        report.append("AVERAGE PROCESSING TIMES (ms):\n");
        Map<String, Double> avgTimes = metrics.getAverageProcessingTimes();
        for (Map.Entry<String, Double> entry : avgTimes.entrySet()) {
            report.append(String.format("  %s: %.2f ms\n", entry.getKey(), entry.getValue()));
        }
        report.append("\n");

        // Bias indicators
        report.append("POTENTIAL BIAS INDICATORS:\n");
        appendBiasIndicators(report);

        return report.toString();
    }

    /**
     * Gets the current metrics object for programmatic access.
     * 
     * @return BiasDetectionMetrics instance
     */
    public BiasDetectionMetrics getMetrics() {
        return metrics;
    }

    private boolean validateAttribute(com.truveta.opentoken.attributes.BaseAttribute attribute, String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            return attribute.validate(value);
        } catch (Exception e) {
            return false;
        }
    }

    private void appendDistributionSection(StringBuilder report, String sectionName, Map<String, Long> distribution) {
        report.append(String.format("%s:\n", sectionName));
        long total = distribution.values().stream().mapToLong(Long::longValue).sum();
        for (Map.Entry<String, Long> entry : distribution.entrySet()) {
            double percentage = total > 0 ? (entry.getValue().doubleValue() / (double) total) * 100 : 0;
            report.append(String.format("  %s: %d (%.2f%%)\n", entry.getKey(), entry.getValue(), percentage));
        }
        report.append("\n");
    }

    private void appendValidationSection(StringBuilder report, String sectionName, Map<String, Long> success,
            Map<String, Long> failure) {
        report.append(String.format("%s:\n", sectionName));
        for (String key : success.keySet()) {
            long successCount = success.getOrDefault(key, 0L);
            long failureCount = failure.getOrDefault(key, 0L);
            long total = successCount + failureCount;
            double successRate = total > 0 ? ((double) successCount / (double) total) * 100 : 0;
            report.append(String.format("  %s: %.2f%% (%d/%d)\n", key, successRate, successCount, total));
        }
        report.append("\n");
    }

    private void appendBiasIndicators(StringBuilder report) {
        // Check for significant disparities in success rates
        Map<String, Long> tokenSuccess = metrics.getTokenGenerationSuccess();
        Map<String, Long> tokenFailure = metrics.getTokenGenerationFailure();

        double overallSuccessRate = metrics.getOverallSuccessRate();
        boolean foundBias = false;

        for (String demographicKey : tokenSuccess.keySet()) {
            long successCount = tokenSuccess.getOrDefault(demographicKey, 0L);
            long failureCount = tokenFailure.getOrDefault(demographicKey, 0L);
            long total = successCount + failureCount;

            if (total > 10) { // Only consider groups with sufficient sample size
                double groupSuccessRate = (double) successCount / total;
                double difference = Math.abs(groupSuccessRate - overallSuccessRate);

                if (difference > 0.1) { // 10% or more difference
                    report.append(
                            String.format("  WARNING: %s has success rate %.2f%% (%.2f%% difference from overall)\n",
                                    demographicKey, groupSuccessRate * 100, difference * 100));
                    foundBias = true;
                }
            }
        }

        if (!foundBias) {
            report.append("  No significant bias indicators detected.\n");
        }

        report.append("\n");
    }
}