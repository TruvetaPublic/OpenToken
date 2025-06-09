/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BirthDateAttributeTest {

    private BirthDateAttribute birthDateAttribute;

    @BeforeEach
    void setUp() {
        birthDateAttribute = new BirthDateAttribute();
    }

    @Test
    void getName_ShouldReturnBirthDate() {
        assertEquals("BirthDate", birthDateAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnBirthDateAlias() {
        assertArrayEquals(new String[] { "BirthDate" }, birthDateAttribute.getAliases());
    }

    @Test
    void normalize_ValidDateFormats_ShouldNormalizeToYYYYMMDD() {
        assertEquals("2023-10-26", birthDateAttribute.normalize("2023-10-26"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("2023/10/26"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("10/26/2023"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("10-26-2023"));
        assertEquals("2023-10-26", birthDateAttribute.normalize("26.10.2023"));
    }

    @Test
    void normalize_InvalidDateFormat_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            birthDateAttribute.normalize("20231026");
        });
        assertEquals("Invalid date format: 20231026", exception.getMessage());
    }

    @Test
    void validate_ValidDate_ShouldReturnTrue() {
        assertTrue(birthDateAttribute.validate("2023-10-26"));
    }

    @Test
    void normalize_ThreadSafety() throws InterruptedException {
        final int threadCount = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final List<String> results = Collections.synchronizedList(new ArrayList<>());
        final String testDate = "10/26/2023";

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    barrier.await(); // Synchronize to increase contention

                    // Perform normalization
                    String result = birthDateAttribute.normalize(testDate);
                    results.add(result);

                    finishLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }

        startLatch.countDown(); // Start all threads
        finishLatch.await(15, TimeUnit.SECONDS); // Wait for all threads to complete

        // Verify all threads got the same result
        assertEquals(threadCount, results.size());
        for (String result : results) {
            assertEquals("2023-10-26", result);
        }
    }

    @Test
    void normalize_DateOutsideAcceptableRange_ShouldReturnNull() {
        // Test dates before 1910-01-01
        assertNull(birthDateAttribute.normalize("1909-12-31"));
        assertNull(birthDateAttribute.normalize("12/31/1909"));
        assertNull(birthDateAttribute.normalize("1900-01-01"));
        assertNull(birthDateAttribute.normalize("01/01/1900"));
        assertNull(birthDateAttribute.normalize("31.12.1909"));

        // Test dates after today
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate nextYear = LocalDate.now().plusYears(1);

        String tomorrowFormatted = tomorrow.toString(); // yyyy-MM-dd format
        String nextYearFormatted = nextYear.toString(); // yyyy-MM-dd format

        assertNull(birthDateAttribute.normalize(tomorrowFormatted));
        assertNull(birthDateAttribute.normalize(nextYearFormatted));
        assertNull(birthDateAttribute.normalize("2030-01-01"));
        assertNull(birthDateAttribute.normalize("01/01/2030"));
    }

    @Test
    void normalize_DateWithinAcceptableRange_ShouldReturnNormalizedDate() {
        // Test boundary dates that should be accepted
        assertEquals("1910-01-01", birthDateAttribute.normalize("1910-01-01"));
        assertEquals("1910-01-01", birthDateAttribute.normalize("01/01/1910"));
        assertEquals("1910-01-01", birthDateAttribute.normalize("01.01.1910"));

        // Test today's date (should be accepted)
        LocalDate today = LocalDate.now();
        String todayFormatted = today.toString();
        assertEquals(todayFormatted, birthDateAttribute.normalize(todayFormatted));

        // Test some dates within the acceptable range
        assertEquals("1950-06-15", birthDateAttribute.normalize("1950-06-15"));
        assertEquals("1990-12-25", birthDateAttribute.normalize("12/25/1990"));
        assertEquals("2000-02-29", birthDateAttribute.normalize("29.02.2000")); // Leap year
    }

    void testSerialization() throws Exception {
        // Serialize the attribute
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(birthDateAttribute);
        oos.close();

        // Deserialize the attribute
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        BirthDateAttribute deserializedAttribute = (BirthDateAttribute) ois.readObject();
        ois.close();

        // Test various date formats with both original and deserialized attributes
        String[] testValues = {
                "2023-10-26",
                "2023/10/26",
                "10/26/2023",
                "10-26-2023",
                "26.10.2023",
                "1990-01-01",
                "12/31/1999",
                "01-01-2000"
        };

        for (String value : testValues) {
            assertEquals(
                    birthDateAttribute.getName(),
                    deserializedAttribute.getName(),
                    "Attribute names should match");

            assertArrayEquals(
                    birthDateAttribute.getAliases(),
                    deserializedAttribute.getAliases(),
                    "Attribute aliases should match");

            assertEquals(
                    birthDateAttribute.normalize(value),
                    deserializedAttribute.normalize(value),
                    "Normalization should be identical for value: " + value);

            assertEquals(
                    birthDateAttribute.validate(value),
                    deserializedAttribute.validate(value),
                    "Validation should be identical for value: " + value);
        }
    }
}