/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LastNameAttributeTest {

    private LastNameAttribute lastNameAttribute;

    @BeforeEach
    void setUp() {
        lastNameAttribute = new LastNameAttribute();
    }

    @Test
    void getName_ShouldReturnLastName() {
        assertEquals("LastName", lastNameAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnLastNameAndSurname() {
        String[] expectedAliases = { "LastName", "Surname" };
        String[] actualAliases = lastNameAttribute.getAliases();
        assertArrayEquals(expectedAliases, actualAliases);
    }

    @Test
    void normalize_ShouldReturnUnchangedValue() {
        String input = "Doe";
        assertEquals(input, lastNameAttribute.normalize(input));
    }

    @Test
    void normalize_Accent() {
        String name1 = "Gómez";
        String name2 = "Gutiérrez";
        String name3 = "Hernández";
        String name4 = "Mäder";
        assertEquals("Gomez", lastNameAttribute.normalize(name1));
        assertEquals("Gutierrez", lastNameAttribute.normalize(name2));
        assertEquals("Hernandez", lastNameAttribute.normalize(name3));
        assertEquals("Mader", lastNameAttribute.normalize(name4));
    }

    @Test
    void validate_ShouldReturnTrueForAnyNonEmptyString() {
        assertTrue(lastNameAttribute.validate("Doe"));
        assertTrue(lastNameAttribute.validate("Smith-Jones"));
        assertTrue(lastNameAttribute.validate("D"));
        assertTrue(lastNameAttribute.validate("test123"));
    }

    @Test
    void validate_ShouldReturnFalseForNullOrEmptyString() {
        assertFalse(lastNameAttribute.validate(null), "Null value should not be allowed");
        assertFalse(lastNameAttribute.validate(""), "Empty value should not be allowed");
        assertTrue(lastNameAttribute.validate("test123"), "Non-empty value should be allowed");
    }

    @Test
    void normalize_ThreadSafety() throws InterruptedException {
        final int threadCount = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final List<String> results = Collections.synchronizedList(new ArrayList<>());
        final String testName = "Hernández";

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    barrier.await(); // Synchronize to increase contention

                    // Perform normalization
                    String result = lastNameAttribute.normalize(testName);
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
            assertEquals("Hernandez", result);
        }
    }

    @Test
    void normalize_ShouldRemoveGenerationalSuffixes() {
        // Test various generational suffix formats
        assertEquals("Smith", lastNameAttribute.normalize("Smith Jr."));
        assertEquals("Johnson", lastNameAttribute.normalize("Johnson Junior"));
        assertEquals("Williams", lastNameAttribute.normalize("Williams Sr."));
        assertEquals("Brown", lastNameAttribute.normalize("Brown Senior"));
        assertEquals("Davis", lastNameAttribute.normalize("Davis II"));
        assertEquals("Miller", lastNameAttribute.normalize("Miller III"));
        assertEquals("Wilson", lastNameAttribute.normalize("Wilson IV"));
        assertEquals("Moore", lastNameAttribute.normalize("Moore V"));
        assertEquals("Taylor", lastNameAttribute.normalize("Taylor VI"));
        assertEquals("Anderson", lastNameAttribute.normalize("Anderson VII"));
        assertEquals("Thomas", lastNameAttribute.normalize("Thomas VIII"));
        assertEquals("Jackson", lastNameAttribute.normalize("Jackson IX"));
        assertEquals("White", lastNameAttribute.normalize("White X"));

        // Test numeric suffixes
        assertEquals("Garcia", lastNameAttribute.normalize("Garcia 1st"));
        assertEquals("Martinez", lastNameAttribute.normalize("Martinez 2nd"));
        assertEquals("Robinson", lastNameAttribute.normalize("Robinson 3rd"));
        assertEquals("Clark", lastNameAttribute.normalize("Clark 4th"));
        assertEquals("Rodriguez", lastNameAttribute.normalize("Rodriguez 5th"));

        // Test case insensitive matching
        assertEquals("Lewis", lastNameAttribute.normalize("Lewis jr."));
        assertEquals("Lee", lastNameAttribute.normalize("Lee SENIOR"));
        assertEquals("Walker", lastNameAttribute.normalize("Walker ii"));
        assertEquals("Hall", lastNameAttribute.normalize("Hall JR"));

        // Test suffixes without periods
        assertEquals("Allen", lastNameAttribute.normalize("Allen Jr"));
        assertEquals("Young", lastNameAttribute.normalize("Young Sr"));
    }

    @Test
    void normalize_ShouldRemoveSpecialCharacters() {
        // Test removal of dashes, spaces, and other non-alphanumeric characters
        assertEquals("OConnor", lastNameAttribute.normalize("O'Connor"));
        assertEquals("SmithJones", lastNameAttribute.normalize("Smith-Jones"));
        assertEquals("VanDerBerg", lastNameAttribute.normalize("Van Der Berg"));
        assertEquals("McDonald", lastNameAttribute.normalize("Mc Donald"));
        assertEquals("DelaRosa", lastNameAttribute.normalize("De la Rosa"));

        // Test various special characters
        assertEquals("Smith", lastNameAttribute.normalize("Smith@#$"));
        assertEquals("Johnson", lastNameAttribute.normalize("Johnson_123"));
        assertEquals("WilliamsCo", lastNameAttribute.normalize("Williams&Co"));
        assertEquals("Brown", lastNameAttribute.normalize("Brown*"));
        assertEquals("DavisTest", lastNameAttribute.normalize("Davis(Test)"));
        assertEquals("MillerWilson", lastNameAttribute.normalize("Miller+Wilson"));
        assertEquals("GarciaLopez", lastNameAttribute.normalize("García-López"));

        // Test numbers mixed with letters
        assertEquals("Smith", lastNameAttribute.normalize("Smith123"));
        assertEquals("Johnson", lastNameAttribute.normalize("John5son"));
        assertEquals("Tet", lastNameAttribute.normalize("Te$t123"));
    }

    @Test
    void normalize_ShouldHandleGenerationalSuffixesAndSpecialCharacters() {
        // Test combination of generational suffixes and special characters
        assertEquals("OConnor", lastNameAttribute.normalize("O'Connor Jr."));
        assertEquals("SmithJones", lastNameAttribute.normalize("Smith-Jones Senior"));
        assertEquals("McDonald", lastNameAttribute.normalize("Mc Donald III"));
        assertEquals("VanDerBerg", lastNameAttribute.normalize("Van Der Berg II"));

        // Test with accents, suffixes, and special characters
        assertEquals("GarciaLopez", lastNameAttribute.normalize("García-López Jr."));
        assertEquals("HernandezMartinez", lastNameAttribute.normalize("Hernández-Martinez Sr."));
        assertEquals("RodriguezOBrien", lastNameAttribute.normalize("Rodríguez O'Brien III"));
    }

    @Test
    void normalize_ShouldHandleComplexCombinations() {
        // Test names with accents, special characters, and suffixes all together
        assertEquals("GomezRodriguez", lastNameAttribute.normalize("Gómez-Rodríguez Jr."));
        assertEquals("DelaCroix", lastNameAttribute.normalize("De-la-Croix Sr."));
        assertEquals("OBrienMcCarthy", lastNameAttribute.normalize("O'Brien-McCarthy III"));
        assertEquals("VanderWaal", lastNameAttribute.normalize("Vander-Waal IV"));

        // Test edge cases
        assertEquals("TetNme", lastNameAttribute.normalize("Te$t-N@me Jr."));
        assertEquals("ComplexName", lastNameAttribute.normalize("Cömplex-Nâme123 Senior"));
    }
}
