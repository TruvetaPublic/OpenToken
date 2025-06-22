/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

/**
 * Data structure for collecting and storing bias detection metrics.
 * 
 * This class provides a thread-safe way to collect demographic distribution
 * and quality metrics to detect potential bias in data processing.
 */
public class BiasDetectionMetrics {

    // Distribution metrics
    private final Map<String, AtomicLong> sexDistribution = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ageGroupDistribution = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> geographicDistribution = new ConcurrentHashMap<>();

    // Quality metrics by demographic
    private final Map<String, AtomicLong> sexValidationSuccess = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> sexValidationFailure = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ageGroupValidationSuccess = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ageGroupValidationFailure = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> geographicValidationSuccess = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> geographicValidationFailure = new ConcurrentHashMap<>();

    // Token generation metrics
    private final Map<String, AtomicLong> tokenGenerationSuccess = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> tokenGenerationFailure = new ConcurrentHashMap<>();

    // Processing time metrics
    private final Map<String, AtomicLong> processingTimeSum = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> processingTimeCount = new ConcurrentHashMap<>();

    // Total counters
    private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
    private final AtomicLong totalRecordsSuccessful = new AtomicLong(0);
    private final AtomicLong totalRecordsFailed = new AtomicLong(0);

    public void incrementSexDistribution(String sex) {
        sexDistribution.computeIfAbsent(sex, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementAgeGroupDistribution(String ageGroup) {
        ageGroupDistribution.computeIfAbsent(ageGroup, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementGeographicDistribution(String region) {
        geographicDistribution.computeIfAbsent(region, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordSexValidation(String sex, boolean successful) {
        if (successful) {
            sexValidationSuccess.computeIfAbsent(sex, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            sexValidationFailure.computeIfAbsent(sex, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public void recordAgeGroupValidation(String ageGroup, boolean successful) {
        if (successful) {
            ageGroupValidationSuccess.computeIfAbsent(ageGroup, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            ageGroupValidationFailure.computeIfAbsent(ageGroup, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public void recordGeographicValidation(String region, boolean successful) {
        if (successful) {
            geographicValidationSuccess.computeIfAbsent(region, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            geographicValidationFailure.computeIfAbsent(region, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public void recordTokenGeneration(String demographicKey, boolean successful) {
        if (successful) {
            tokenGenerationSuccess.computeIfAbsent(demographicKey, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            tokenGenerationFailure.computeIfAbsent(demographicKey, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    public void recordProcessingTime(String demographicKey, long timeMs) {
        processingTimeSum.computeIfAbsent(demographicKey, k -> new AtomicLong(0)).addAndGet(timeMs);
        processingTimeCount.computeIfAbsent(demographicKey, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementTotalRecords() {
        totalRecordsProcessed.incrementAndGet();
    }

    public void incrementSuccessfulRecords() {
        totalRecordsSuccessful.incrementAndGet();
    }

    public void incrementFailedRecords() {
        totalRecordsFailed.incrementAndGet();
    }

    // Getters for metrics
    public Map<String, Long> getSexDistribution() {
        return convertToLongMap(sexDistribution);
    }

    public Map<String, Long> getAgeGroupDistribution() {
        return convertToLongMap(ageGroupDistribution);
    }

    public Map<String, Long> getGeographicDistribution() {
        return convertToLongMap(geographicDistribution);
    }

    public Map<String, Long> getSexValidationSuccess() {
        return convertToLongMap(sexValidationSuccess);
    }

    public Map<String, Long> getSexValidationFailure() {
        return convertToLongMap(sexValidationFailure);
    }

    public Map<String, Long> getAgeGroupValidationSuccess() {
        return convertToLongMap(ageGroupValidationSuccess);
    }

    public Map<String, Long> getAgeGroupValidationFailure() {
        return convertToLongMap(ageGroupValidationFailure);
    }

    public Map<String, Long> getGeographicValidationSuccess() {
        return convertToLongMap(geographicValidationSuccess);
    }

    public Map<String, Long> getGeographicValidationFailure() {
        return convertToLongMap(geographicValidationFailure);
    }

    public Map<String, Long> getTokenGenerationSuccess() {
        return convertToLongMap(tokenGenerationSuccess);
    }

    public Map<String, Long> getTokenGenerationFailure() {
        return convertToLongMap(tokenGenerationFailure);
    }

    public Map<String, Double> getAverageProcessingTimes() {
        Map<String, Double> averages = new ConcurrentHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : processingTimeSum.entrySet()) {
            String key = entry.getKey();
            long sum = entry.getValue().get();
            long count = processingTimeCount.getOrDefault(key, new AtomicLong(0)).get();
            if (count > 0) {
                averages.put(key, (double) sum / count);
            }
        }
        return averages;
    }

    public long getTotalRecordsProcessed() {
        return totalRecordsProcessed.get();
    }

    public long getTotalRecordsSuccessful() {
        return totalRecordsSuccessful.get();
    }

    public long getTotalRecordsFailed() {
        return totalRecordsFailed.get();
    }

    public double getOverallSuccessRate() {
        long total = totalRecordsProcessed.get();
        if (total == 0)
            return 0.0;
        return (double) totalRecordsSuccessful.get() / total;
    }

    private Map<String, Long> convertToLongMap(Map<String, AtomicLong> atomicMap) {
        Map<String, Long> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : atomicMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }
}