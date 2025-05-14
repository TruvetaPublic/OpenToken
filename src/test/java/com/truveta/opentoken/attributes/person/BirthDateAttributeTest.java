/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}