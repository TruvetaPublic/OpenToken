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
    }

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

    @Test
    void normalize_ShouldHandleEdgeCasesInTitleRemoval() {
        // Test edge cases for title removal
        assertEquals("Mr", firstNameAttribute.normalize("Mr."));
        assertEquals("Sir", firstNameAttribute.normalize("Sir"));
        assertEquals("Prof", firstNameAttribute.normalize("Prof."));
        assertEquals("General", firstNameAttribute.normalize("General"));

        // Test titles with only spaces after
        assertEquals("Mr", firstNameAttribute.normalize("Mr. "));
        assertEquals("Dr", firstNameAttribute.normalize("    Dr.   "));

        // Test multiple titles (should only remove the first one)
        assertEquals("Smith", firstNameAttribute.normalize("Mr. Smith"));
        assertEquals("Johnson", firstNameAttribute.normalize("Dr. Johnson"));

        // Test names that start with title-like words but aren't titles
        assertEquals("Drew", firstNameAttribute.normalize("Drew"));
        assertEquals("Profeta", firstNameAttribute.normalize("Profeta"));
        assertEquals("Missy", firstNameAttribute.normalize("Missy"));

        // Test titles with unusual spacing
        assertEquals("MrJohn", firstNameAttribute.normalize("Mr.John"));
        assertEquals("Jane", firstNameAttribute.normalize("Dr.  Jane"));
    }

    @Test
    void normalize_ShouldRemoveGenerationalSuffixes() {
        // Test various generational suffix formats in first names
        assertEquals("John", firstNameAttribute.normalize("John Jr."));
        assertEquals("Jane", firstNameAttribute.normalize("Jane Junior"));
        assertEquals("Robert", firstNameAttribute.normalize("Robert Sr."));
        assertEquals("Mary", firstNameAttribute.normalize("Mary Senior"));
        assertEquals("William", firstNameAttribute.normalize("William II"));
        assertEquals("Elizabeth", firstNameAttribute.normalize("Elizabeth III"));
        assertEquals("Charles", firstNameAttribute.normalize("Charles IV"));
        assertEquals("David", firstNameAttribute.normalize("David V"));
        assertEquals("Michael", firstNameAttribute.normalize("Michael VI"));
        assertEquals("Sarah", firstNameAttribute.normalize("Sarah VII"));
        assertEquals("James", firstNameAttribute.normalize("James VIII"));
        assertEquals("Anna", firstNameAttribute.normalize("Anna IX"));
        assertEquals("Thomas", firstNameAttribute.normalize("Thomas X"));

        // Test numeric suffixes
        assertEquals("Daniel", firstNameAttribute.normalize("Daniel 1st"));
        assertEquals("Emily", firstNameAttribute.normalize("Emily 2nd"));
        assertEquals("Andrew", firstNameAttribute.normalize("Andrew 3rd"));
        assertEquals("Jennifer", firstNameAttribute.normalize("Jennifer 4th"));
        assertEquals("Christopher", firstNameAttribute.normalize("Christopher 5th"));

        // Test case insensitive matching
        assertEquals("Matthew", firstNameAttribute.normalize("Matthew jr."));
        assertEquals("Jessica", firstNameAttribute.normalize("Jessica SENIOR"));
        assertEquals("Nicholas", firstNameAttribute.normalize("Nicholas ii"));
        assertEquals("Amanda", firstNameAttribute.normalize("Amanda JR"));

        // Test suffixes without periods
        assertEquals("Joshua", firstNameAttribute.normalize("Joshua Jr"));
        assertEquals("Michelle", firstNameAttribute.normalize("Michelle Sr"));
    }

    @Test
    void normalize_ShouldHandleTitlesAndGenerationalSuffixesTogether() {
        // Test combination of titles and generational suffixes
        assertEquals("John", firstNameAttribute.normalize("Dr. John Jr."));
        assertEquals("Jane", firstNameAttribute.normalize("Mrs. Jane Sr."));
        assertEquals("Robert", firstNameAttribute.normalize("Prof. Robert III"));
        assertEquals("Mary", firstNameAttribute.normalize("Miss Mary II"));
        assertEquals("William", firstNameAttribute.normalize("Mr. William Senior"));
        assertEquals("Elizabeth", firstNameAttribute.normalize("Dr. Elizabeth Junior"));

        // Test with accents, titles, and generational suffixes
        assertEquals("Jose", firstNameAttribute.normalize("Mr. José Jr."));
        assertEquals("Francois", firstNameAttribute.normalize("Dr. François Sr."));
        assertEquals("Maria", firstNameAttribute.normalize("Mrs. María III"));

        // Test with middle initials, titles, and suffixes
        assertEquals("John", firstNameAttribute.normalize("Dr. John A. Jr."));
        assertEquals("Jane", firstNameAttribute.normalize("Mrs. Jane B Sr."));
        assertEquals("Robert", firstNameAttribute.normalize("Prof. Robert C III"));

        // Test different orders and combinations
        assertEquals("Charles", firstNameAttribute.normalize("Sir Charles IV"));
        assertEquals("David", firstNameAttribute.normalize("Col. David V"));
        assertEquals("Michael", firstNameAttribute.normalize("Gen. Michael VI"));
    }

    @Test
    void normalize_ShouldHandleEdgeCasesWithTitlesAndSuffixes() {
        // Test when title and suffix removal would result in empty string
        assertEquals("Jr", firstNameAttribute.normalize("Jr."));
        assertEquals("Senior", firstNameAttribute.normalize("Senior"));
        assertEquals("III", firstNameAttribute.normalize("III"));
        assertEquals("Dr", firstNameAttribute.normalize("Dr."));

        // Test names that look like titles or suffixes but aren't
        assertEquals("Junior", firstNameAttribute.normalize("Junior")); // As a first name
        assertEquals("King", firstNameAttribute.normalize("King")); // Not in title pattern
        assertEquals("Prince", firstNameAttribute.normalize("Prince")); // Not in title pattern
        assertEquals("Earl", firstNameAttribute.normalize("Earl")); // Not in title pattern

        // Test complex combinations with multiple spaces
        assertEquals("John", firstNameAttribute.normalize("  Mr.   John   Jr.  "));
        assertEquals("Jane", firstNameAttribute.normalize("Dr.    Jane    Senior"));
        assertEquals("Robert", firstNameAttribute.normalize("   Prof. Robert   III   "));

        // Test unusual but valid combinations
        assertEquals("JohnPaul", firstNameAttribute.normalize("Dr. John-Paul Jr."));
        assertEquals("MaryEllen", firstNameAttribute.normalize("Mrs. Mary Ellen Sr."));
        assertEquals("JeanLuc", firstNameAttribute.normalize("Capt. Jean-Luc III"));

        // Test with numbers and special characters mixed in
        assertEquals("John", firstNameAttribute.normalize("Mr. John123 Jr."));
        assertEquals("Jane", firstNameAttribute.normalize("Dr. Jane@#$ Sr."));
        assertEquals("RobertSmith", firstNameAttribute.normalize("Prof. Robert_Smith III"));
    }

    @Test
    void normalize_ShouldHandleMultipleTitlesAndSuffixes() {
        // Test multiple titles (should only remove the first valid one)
        assertEquals("DrJohn", firstNameAttribute.normalize("Mr. Dr. John"));
        assertEquals("MrsJane", firstNameAttribute.normalize("Dr. Mrs. Jane"));

        // Test multiple suffixes (should remove all recognized suffixes)
        assertEquals("JohnJr", firstNameAttribute.normalize("John Jr. Sr.")); // This should remove both
        assertEquals("JaneIII", firstNameAttribute.normalize("Jane III II")); // This should remove both

        // Test edge case: title that looks like a name
        assertEquals("Drew", firstNameAttribute.normalize("Drew")); // Drew is not Dr.
        assertEquals("Missy", firstNameAttribute.normalize("Missy")); // Missy is not Miss
        assertEquals("Gene", firstNameAttribute.normalize("Gene")); // Gene is not Gen.
    }

    @Test
    void normalize_ShouldHandleGenerationalSuffixesWithoutTitles() {
        // Test standalone generational suffixes in various formats
        assertEquals("Alexander", firstNameAttribute.normalize("Alexander Jr."));
        assertEquals("Benjamin", firstNameAttribute.normalize("Benjamin Junior"));
        assertEquals("Catherine", firstNameAttribute.normalize("Catherine Sr."));
        assertEquals("Dominic", firstNameAttribute.normalize("Dominic Senior"));

        // Test Roman numerals
        assertEquals("Edward", firstNameAttribute.normalize("Edward I"));
        assertEquals("Frederick", firstNameAttribute.normalize("Frederick II"));
        assertEquals("George", firstNameAttribute.normalize("George III"));
        assertEquals("Henry", firstNameAttribute.normalize("Henry IV"));
        assertEquals("Isaac", firstNameAttribute.normalize("Isaac V"));

        // Test ordinal numbers
        assertEquals("Jonathan", firstNameAttribute.normalize("Jonathan 1st"));
        assertEquals("Katherine", firstNameAttribute.normalize("Katherine 2nd"));
        assertEquals("Lawrence", firstNameAttribute.normalize("Lawrence 3rd"));
        assertEquals("Margaret", firstNameAttribute.normalize("Margaret 4th"));

        // Test with accents and special characters
        assertEquals("Jose", firstNameAttribute.normalize("José Jr."));
        assertEquals("Francois", firstNameAttribute.normalize("François Sr."));
        assertEquals("JeanMarc", firstNameAttribute.normalize("Jean-Marc III"));
    }
}
