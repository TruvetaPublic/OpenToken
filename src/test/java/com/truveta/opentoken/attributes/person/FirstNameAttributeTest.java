/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class FirstNameAttributeTest {

    private FirstNameAttribute firstNameAttribute;

    @BeforeEach
    void setUp() {
        firstNameAttribute = new FirstNameAttribute();
    }

    @Test
    void getName_ShouldReturnFirstName() {
        assertEquals("FirstName", firstNameAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnFirstNameAndGivenName() {
        String[] expectedAliases = { "FirstName", "GivenName" };
        String[] actualAliases = firstNameAttribute.getAliases();
        assertArrayEquals(expectedAliases, actualAliases);
    }

    @Test
    void normalize_ShouldReturnUnchangedValue() {
        String input = "John";
        assertEquals(input, firstNameAttribute.normalize(input));
    }

    @Test
    void normalize_Accent() {
        String name1 = "José";
        String name2 = "Vũ";
        String name3 = "François";
        String name4 = "Renée";
        assertEquals("Jose", firstNameAttribute.normalize(name1));
        assertEquals("Vu", firstNameAttribute.normalize(name2));
        assertEquals("Francois", firstNameAttribute.normalize(name3));
        assertEquals("Renee", firstNameAttribute.normalize(name4));
    }

    @Test
    void validate_ShouldReturnTrueForAnyNonEmptyString() {
        assertTrue(firstNameAttribute.validate("John"));
        assertTrue(firstNameAttribute.validate("Jane Doe"));
        assertTrue(firstNameAttribute.validate("J"));
    }

    @Test
    void validate_ShouldReturnFalseForNullOrEmptyString() {
        assertFalse(firstNameAttribute.validate(null), "Null value should not be allowed");
        assertFalse(firstNameAttribute.validate(""), "Empty value should not be allowed");
        assertTrue(firstNameAttribute.validate("test123"), "Non-empty value should be allowed");
    }

    @Test
    void normalize_ThreadSafety() throws InterruptedException {
        final int threadCount = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final List<String> results = Collections.synchronizedList(new ArrayList<>());
        final String testName = "François";

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    barrier.await(); // Synchronize to increase contention

                    // Perform normalization
                    String result = firstNameAttribute.normalize(testName);
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
            assertEquals("Francois", result);
        }
    }

    @Test
    void normalize_ShouldRemoveTitles() {
        // Test various title formats
        assertEquals("John", firstNameAttribute.normalize("Mr. John"));
        assertEquals("Jane", firstNameAttribute.normalize("Mrs. Jane"));
        assertEquals("Sarah", firstNameAttribute.normalize("Ms. Sarah"));
        assertEquals("Emily", firstNameAttribute.normalize("Miss Emily"));
        assertEquals("Robert", firstNameAttribute.normalize("Dr. Robert"));
        assertEquals("Alice", firstNameAttribute.normalize("Prof. Alice"));
        assertEquals("James", firstNameAttribute.normalize("Capt. James"));
        assertEquals("William", firstNameAttribute.normalize("Sir William"));
        assertEquals("David", firstNameAttribute.normalize("Col. David"));
        assertEquals("Michael", firstNameAttribute.normalize("Gen. Michael"));
        assertEquals("Thomas", firstNameAttribute.normalize("Cmdr. Thomas"));
        assertEquals("Daniel", firstNameAttribute.normalize("Lt. Daniel"));
        assertEquals("Samuel", firstNameAttribute.normalize("Rabbi Samuel"));
        assertEquals("Joseph", firstNameAttribute.normalize("Father Joseph"));
        assertEquals("Francis", firstNameAttribute.normalize("Brother Francis"));
        assertEquals("Mary", firstNameAttribute.normalize("Sister Mary"));
        assertEquals("Charles", firstNameAttribute.normalize("Hon. Charles"));
        assertEquals("George", firstNameAttribute.normalize("Honorable George"));
        assertEquals("Matthew", firstNameAttribute.normalize("Reverend Matthew"));
        assertEquals("Andrew", firstNameAttribute.normalize("Doctor Andrew"));

        // Test titles without periods
        assertEquals("John", firstNameAttribute.normalize("Mr John"));
        assertEquals("Jane", firstNameAttribute.normalize("Dr Jane"));

        // Test case insensitive titles
        assertEquals("John", firstNameAttribute.normalize("MR. John"));
        assertEquals("Jane", firstNameAttribute.normalize("dr. Jane"));
        assertEquals("Sarah", firstNameAttribute.normalize("MS Sarah"));
    }

    @Test
    void normalize_ShouldRemoveMiddleInitials() {
        // Test middle initial removal (detected by second to last character being a
        // space)
        assertEquals("John", firstNameAttribute.normalize("John A"));
        assertEquals("Jane", firstNameAttribute.normalize("Jane B"));
        assertEquals("Robert", firstNameAttribute.normalize("Robert C"));
        assertEquals("Mary", firstNameAttribute.normalize("Mary X"));

        // Test with periods in middle initials
        assertEquals("John", firstNameAttribute.normalize("John A."));
        assertEquals("Jane", firstNameAttribute.normalize("Jane B."));

        // Test names that shouldn't have initials removed (no space before last
        // character)
        assertEquals("Jo", firstNameAttribute.normalize("Jo")); // Short name, no middle initial
        assertEquals("A", firstNameAttribute.normalize("A")); // Single character name
    }

    @Test
    void normalize_ShouldHandleTitlesAndInitialsTogether() {
        // Test combination of title and middle initial
        assertEquals("John", firstNameAttribute.normalize("Dr. John A"));
        assertEquals("Jane", firstNameAttribute.normalize("Mrs. Jane B."));
        assertEquals("Robert", firstNameAttribute.normalize("Prof. Robert C"));
        assertEquals("Mary", firstNameAttribute.normalize("Miss Mary X"));

        // Test with accents, titles, and initials
        assertEquals("Jose", firstNameAttribute.normalize("Mr. José A"));
        assertEquals("Francois", firstNameAttribute.normalize("Dr. François B."));
    }

    @Test
    void normalize_ShouldRemoveNonAlphabeticCharacters() {
        // Test removal of dashes, spaces, and other non-alphanumeric characters
        assertEquals("JohnDoe", firstNameAttribute.normalize("John-Doe"));
        assertEquals("MaryJane", firstNameAttribute.normalize("Mary Jane"));
        assertEquals("AnnMarie", firstNameAttribute.normalize("Ann-Marie"));
        assertEquals("JeanLuc", firstNameAttribute.normalize("Jean-Luc"));

        // Test with numbers and special characters
        assertEquals("John", firstNameAttribute.normalize("John123"));
        assertEquals("Jane", firstNameAttribute.normalize("Jane@#$"));
        assertEquals("RobertSmith", firstNameAttribute.normalize("Robert_Smith"));

    void serialization_ShouldPreserveState() throws Exception {
        FirstNameAttribute originalAttribute = new FirstNameAttribute();

        // Serialize the attribute
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(originalAttribute);
        out.close();

        // Deserialize the attribute
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        FirstNameAttribute deserializedAttribute = (FirstNameAttribute) in.readObject();
        in.close();

        // Test that both attributes behave identically
        String[] testValues = {
                "John",
                "Mr. John A",
                "Dr. Jane B.",
                "José",
                "François",
                "John-Paul",
                "Mary Jane",
                "Prof. Robert C"
        };

        for (String value : testValues) {
            assertEquals(
                    originalAttribute.getName(),
                    deserializedAttribute.getName(),
                    "Attribute names should match");

            assertArrayEquals(
                    originalAttribute.getAliases(),
                    deserializedAttribute.getAliases(),
                    "Attribute aliases should match");

            assertEquals(
                    originalAttribute.normalize(value),
                    deserializedAttribute.normalize(value),
                    "Normalization should be identical for value: " + value);

            assertEquals(
                    originalAttribute.validate(value),
                    deserializedAttribute.validate(value),
                    "Validation should be identical for value: " + value);
        }
    }
}
