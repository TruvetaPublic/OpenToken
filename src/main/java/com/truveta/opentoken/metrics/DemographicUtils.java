/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.metrics;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class for demographic categorization and analysis.
 */
public class DemographicUtils {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
    };

    /**
     * Categorizes age into demographic groups.
     * 
     * @param birthDate The birth date string
     * @return Age group category or "Unknown" if invalid
     */
    public static String categorizeAgeGroup(String birthDate) {
        if (birthDate == null || birthDate.trim().isEmpty()) {
            return "Unknown";
        }

        LocalDate birth = parseBirthDate(birthDate.trim());
        if (birth == null) {
            return "Unknown";
        }

        int age = Period.between(birth, LocalDate.now()).getYears();

        if (age < 0) {
            return "Future"; // Invalid future dates
        } else if (age <= 18) {
            return "0-18";
        } else if (age <= 35) {
            return "19-35";
        } else if (age <= 50) {
            return "36-50";
        } else if (age <= 65) {
            return "51-65";
        } else {
            return "65+";
        }
    }

    /**
     * Normalizes sex/gender values for consistent categorization.
     * 
     * @param sex The sex value
     * @return Normalized sex category or "Unknown"
     */
    public static String normalizeSex(String sex) {
        if (sex == null || sex.trim().isEmpty()) {
            return "Unknown";
        }

        String normalized = sex.trim().toLowerCase();
        if (normalized.matches("^(m|male)$")) {
            return "Male";
        } else if (normalized.matches("^(f|female)$")) {
            return "Female";
        } else {
            return "Unknown";
        }
    }

    /**
     * Categorizes geographic regions based on postal codes.
     * 
     * @param postalCode The postal code
     * @return Geographic region category
     */
    public static String categorizeGeographicRegion(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return "Unknown";
        }

        String code = postalCode.trim();

        // US ZIP code regions (first digit)
        if (code.matches("^\\d{5}(-\\d{4})?$")) {
            char firstDigit = code.charAt(0);
            switch (firstDigit) {
                case '0':
                    return "US-Northeast";
                case '1':
                    return "US-Northeast";
                case '2':
                    return "US-Mid-Atlantic";
                case '3':
                    return "US-Southeast";
                case '4':
                    return "US-Southeast";
                case '5':
                    return "US-Midwest";
                case '6':
                    return "US-South-Central";
                case '7':
                    return "US-South-Central";
                case '8':
                    return "US-Mountain";
                case '9':
                    return "US-West";
                default:
                    return "US-Other";
            }
        }

        // Canadian postal code regions (first letter)
        if (code.matches("^[A-Z]\\d[A-Z]\\s?\\d[A-Z]\\d$")) {
            char firstLetter = code.charAt(0);
            switch (firstLetter) {
                case 'A':
                    return "CA-Atlantic";
                case 'B':
                    return "CA-Atlantic";
                case 'C':
                    return "CA-Atlantic";
                case 'E':
                    return "CA-Atlantic";
                case 'G':
                    return "CA-Quebec";
                case 'H':
                    return "CA-Quebec";
                case 'J':
                    return "CA-Quebec";
                case 'K':
                    return "CA-Ontario";
                case 'L':
                    return "CA-Ontario";
                case 'M':
                    return "CA-Ontario";
                case 'N':
                    return "CA-Ontario";
                case 'P':
                    return "CA-Ontario";
                case 'R':
                    return "CA-Manitoba";
                case 'S':
                    return "CA-Saskatchewan";
                case 'T':
                    return "CA-Alberta";
                case 'V':
                    return "CA-British-Columbia";
                case 'X':
                    return "CA-Territories";
                case 'Y':
                    return "CA-Territories";
                default:
                    return "CA-Other";
            }
        }

        return "Other";
    }

    /**
     * Creates a demographic key combining multiple attributes.
     * 
     * @param sex      Normalized sex
     * @param ageGroup Age group category
     * @param region   Geographic region
     * @return Combined demographic key
     */
    public static String createDemographicKey(String sex, String ageGroup, String region) {
        return String.format("%s_%s_%s", sex, ageGroup, region);
    }

    private static LocalDate parseBirthDate(String dateStr) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }
        return null;
    }
}