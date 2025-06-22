/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemographicUtilsTest {

    @Test
    void testCategorizeAgeGroup_ValidDates() {
        assertEquals("19-35", DemographicUtils.categorizeAgeGroup("1990-01-01"));
        assertEquals("0-18", DemographicUtils.categorizeAgeGroup("2010-01-01"));
        assertEquals("36-50", DemographicUtils.categorizeAgeGroup("1980-01-01"));
        assertEquals("51-65", DemographicUtils.categorizeAgeGroup("1970-01-01"));
        assertEquals("65+", DemographicUtils.categorizeAgeGroup("1950-01-01"));
    }

    @Test
    void testCategorizeAgeGroup_DifferentFormats() {
        assertEquals("19-35", DemographicUtils.categorizeAgeGroup("1990/01/01"));
        assertEquals("19-35", DemographicUtils.categorizeAgeGroup("01/01/1990"));
        assertEquals("19-35", DemographicUtils.categorizeAgeGroup("01-01-1990"));
        assertEquals("19-35", DemographicUtils.categorizeAgeGroup("01.01.1990"));
    }

    @Test
    void testCategorizeAgeGroup_InvalidData() {
        assertEquals("Unknown", DemographicUtils.categorizeAgeGroup(null));
        assertEquals("Unknown", DemographicUtils.categorizeAgeGroup(""));
        assertEquals("Unknown", DemographicUtils.categorizeAgeGroup("invalid-date"));
        assertEquals("Future", DemographicUtils.categorizeAgeGroup("2050-01-01"));
    }

    @Test
    void testNormalizeSex_ValidValues() {
        assertEquals("Male", DemographicUtils.normalizeSex("Male"));
        assertEquals("Male", DemographicUtils.normalizeSex("male"));
        assertEquals("Male", DemographicUtils.normalizeSex("M"));
        assertEquals("Male", DemographicUtils.normalizeSex("m"));

        assertEquals("Female", DemographicUtils.normalizeSex("Female"));
        assertEquals("Female", DemographicUtils.normalizeSex("female"));
        assertEquals("Female", DemographicUtils.normalizeSex("F"));
        assertEquals("Female", DemographicUtils.normalizeSex("f"));
    }

    @Test
    void testNormalizeSex_InvalidValues() {
        assertEquals("Unknown", DemographicUtils.normalizeSex(null));
        assertEquals("Unknown", DemographicUtils.normalizeSex(""));
        assertEquals("Unknown", DemographicUtils.normalizeSex("Other"));
        assertEquals("Unknown", DemographicUtils.normalizeSex("X"));
    }

    @Test
    void testCategorizeGeographicRegion_USZipCodes() {
        assertEquals("US-Northeast", DemographicUtils.categorizeGeographicRegion("01234"));
        assertEquals("US-Northeast", DemographicUtils.categorizeGeographicRegion("12345"));
        assertEquals("US-Mid-Atlantic", DemographicUtils.categorizeGeographicRegion("23456"));
        assertEquals("US-Southeast", DemographicUtils.categorizeGeographicRegion("34567"));
        assertEquals("US-Southeast", DemographicUtils.categorizeGeographicRegion("45678"));
        assertEquals("US-Midwest", DemographicUtils.categorizeGeographicRegion("56789"));
        assertEquals("US-South-Central", DemographicUtils.categorizeGeographicRegion("67890"));
        assertEquals("US-South-Central", DemographicUtils.categorizeGeographicRegion("78901"));
        assertEquals("US-Mountain", DemographicUtils.categorizeGeographicRegion("89012"));
        assertEquals("US-West", DemographicUtils.categorizeGeographicRegion("90123"));
    }

    @Test
    void testCategorizeGeographicRegion_USZipPlus4() {
        assertEquals("US-Northeast", DemographicUtils.categorizeGeographicRegion("01234-5678"));
        assertEquals("US-West", DemographicUtils.categorizeGeographicRegion("90210-1234"));
    }

    @Test
    void testCategorizeGeographicRegion_CanadianPostalCodes() {
        assertEquals("CA-Atlantic", DemographicUtils.categorizeGeographicRegion("A1A 1A1"));
        assertEquals("CA-Atlantic", DemographicUtils.categorizeGeographicRegion("B2B2B2"));
        assertEquals("CA-Quebec", DemographicUtils.categorizeGeographicRegion("G3G 3G3"));
        assertEquals("CA-Quebec", DemographicUtils.categorizeGeographicRegion("H4H4H4"));
        assertEquals("CA-Ontario", DemographicUtils.categorizeGeographicRegion("K5K 5K5"));
        assertEquals("CA-Ontario", DemographicUtils.categorizeGeographicRegion("M6M6M6"));
        assertEquals("CA-Manitoba", DemographicUtils.categorizeGeographicRegion("R7R 7R7"));
        assertEquals("CA-Saskatchewan", DemographicUtils.categorizeGeographicRegion("S8S 8S8"));
        assertEquals("CA-Alberta", DemographicUtils.categorizeGeographicRegion("T9T 9T9"));
        assertEquals("CA-British-Columbia", DemographicUtils.categorizeGeographicRegion("V0V 0V0"));
        assertEquals("CA-Territories", DemographicUtils.categorizeGeographicRegion("X1X 1X1"));
        assertEquals("CA-Territories", DemographicUtils.categorizeGeographicRegion("Y2Y 2Y2"));
    }

    @Test
    void testCategorizeGeographicRegion_InvalidData() {
        assertEquals("Unknown", DemographicUtils.categorizeGeographicRegion(null));
        assertEquals("Unknown", DemographicUtils.categorizeGeographicRegion(""));
        assertEquals("Other", DemographicUtils.categorizeGeographicRegion("invalid"));
        assertEquals("Other", DemographicUtils.categorizeGeographicRegion("1234"));
        assertEquals("Other", DemographicUtils.categorizeGeographicRegion("123456"));
    }

    @Test
    void testCreateDemographicKey() {
        String key = DemographicUtils.createDemographicKey("Male", "19-35", "US-West");
        assertEquals("Male_19-35_US-West", key);

        String keyWithUnknown = DemographicUtils.createDemographicKey("Unknown", "Unknown", "Other");
        assertEquals("Unknown_Unknown_Other", keyWithUnknown);
    }
}