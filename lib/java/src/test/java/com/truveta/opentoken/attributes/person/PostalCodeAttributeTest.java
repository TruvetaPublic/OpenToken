/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.attributes.person;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class PostalCodeAttributeTest {
    private PostalCodeAttribute postalCodeAttribute;

    @BeforeEach
    void setUp() {
        postalCodeAttribute = new PostalCodeAttribute();
    }

    @Test
    void getName_ShouldReturnPostalCode() {
        assertEquals("PostalCode", postalCodeAttribute.getName());
    }

    @Test
    void getAliases_ShouldReturnPostalCodeAndZipCode() {
        String[] expectedAliases = { "PostalCode", "ZipCode" };
        assertArrayEquals(expectedAliases, postalCodeAttribute.getAliases());
    }

    @Test
    void normalize_ShouldReturnFirst5Digits() {
        assertEquals("10001", postalCodeAttribute.normalize("10001-6789"));
        assertEquals("10001", postalCodeAttribute.normalize("10001"));
    }

    @Test
    void normalize_ShouldHandleCanadianPostalCodes() {
        assertEquals("K1A 0A6", postalCodeAttribute.normalize("K1A0A6"));
        assertEquals("K1A 0A6", postalCodeAttribute.normalize("k1a0a6"));
        assertEquals("K1A 0A6", postalCodeAttribute.normalize("K1A 0A6"));
        assertEquals("M5V 3L9", postalCodeAttribute.normalize("m5v3l9"));
        assertEquals("H3Z 2Y7", postalCodeAttribute.normalize("H3Z2Y7"));
        assertEquals("T2X 1V4", postalCodeAttribute.normalize("t2x1v4"));
    }

    @Test
    void validate_ShouldReturnTrueForValidPostalCodes() {
        assertTrue(postalCodeAttribute.validate("95123 "));
        assertTrue(postalCodeAttribute.validate(" 95123"));
        assertTrue(postalCodeAttribute.validate("95123"));
        assertTrue(postalCodeAttribute.validate("95123-6789"));
        assertTrue(postalCodeAttribute.validate("65201-6789"));
    }

    @Test
    void validate_ShouldReturnTrueForValidCanadianPostalCodes() {
        assertTrue(postalCodeAttribute.validate("K1A 0A7"));
        assertTrue(postalCodeAttribute.validate("K1A0A7"));
        assertTrue(postalCodeAttribute.validate("k1a 0a7"));
        assertTrue(postalCodeAttribute.validate("k1a0a7"));
        assertTrue(postalCodeAttribute.validate("M5V 3L9"));
        assertTrue(postalCodeAttribute.validate("H3Z 2Y7"));
        assertTrue(postalCodeAttribute.validate("T2X 1V4"));
        assertTrue(postalCodeAttribute.validate(" K1A 0A7 "));
        assertTrue(postalCodeAttribute.validate("  K1A0A7  "));
    }

    @Test
    void normalize_ShouldHandleWhitespace() {
        PostalCodeAttribute attribute = new PostalCodeAttribute();

        // Test different types of whitespace
        assertEquals("10001", attribute.normalize("10001"), "No whitespace");
        assertEquals("10001", attribute.normalize(" 10001"), "Leading space");
        assertEquals("10001", attribute.normalize("10001 "), "Trailing space");
        assertEquals("10001", attribute.normalize(" 10001 "), "Leading and trailing spaces");
        assertEquals("10   001", attribute.normalize("  10   001  "), "Multiple spaces");
    }

    @Test
    void normalize_ShouldNotHandleInnerWhitespace() {
        PostalCodeAttribute attribute = new PostalCodeAttribute();

        // Test inner whitespace
        assertEquals("1 0 0 0 1", attribute.normalize("1 0 0 0 1"), "Spaces between digits");
        assertEquals("10\t001", attribute.normalize("10\t001"), "Tab character");
        assertEquals("10\n001", attribute.normalize("10\n001"), "Newline character");
        assertEquals("10\r\n001", attribute.normalize("10\r\n001"), "Carriage return and newline");
        assertEquals("K1A  0A7", attribute.normalize("K1A  0A7"),
                "Inner space should not be normalized for Canadian postal code");
    }

    @Test
    void validate_ShouldReturnFalseForInvalidPostalCodes() {
        assertFalse(postalCodeAttribute.validate(null), "Null value should not be allowed");
        assertFalse(postalCodeAttribute.validate(""), "Empty value should not be allowed");
        assertFalse(postalCodeAttribute.validate("1234"), "Short postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("12345"), "Invalid postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("54321"), "Invalid postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("123456"), "Long postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("1234-5678"), "Invalid format should not be allowed");
        assertFalse(postalCodeAttribute.validate("abcde"), "Non-numeric should not be allowed");

        // Invalid Canadian postal code formats
        assertFalse(postalCodeAttribute.validate("K1A"), "Incomplete Canadian postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("K1A 0A"), "Incomplete Canadian postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("K1A 0A67"), "Too long Canadian postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("K11 0A6"),
                "Invalid Canadian postal code format should not be allowed");
        assertFalse(postalCodeAttribute.validate("KAA 0A6"),
                "Invalid Canadian postal code format should not be allowed");
        assertFalse(postalCodeAttribute.validate("K1A 0AA"),
                "Invalid Canadian postal code format should not be allowed");
    }

    @Test
    void normalize_ThreadSafety() throws InterruptedException {
        final int threadCount = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final List<String> results = Collections.synchronizedList(new ArrayList<>());
        final String testPostalCode = "10001-6789";

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    barrier.await(); // Synchronize to increase contention

                    // Perform normalization
                    String result = postalCodeAttribute.normalize(testPostalCode);
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
            assertEquals("10001", result);
        }
    }

    @Test
    void normalize_ShouldHandleEdgeCases() {
        // Test short postal codes (less than 5 characters)
        assertEquals("1234", postalCodeAttribute.normalize("1234 "));
        assertEquals("123", postalCodeAttribute.normalize("123"));
        assertEquals("12", postalCodeAttribute.normalize("12"));
        assertEquals("1", postalCodeAttribute.normalize("1"));

        // Test null and empty values
        assertNull(postalCodeAttribute.normalize(null));
        assertEquals("", postalCodeAttribute.normalize(""));

        // Test exactly 5 characters
        assertEquals("10001", postalCodeAttribute.normalize("10001"));

        assertEquals("10001", postalCodeAttribute.normalize(" 10001"));

        assertEquals("10001", postalCodeAttribute.normalize("10001-6789"));
        assertEquals("10001", postalCodeAttribute.normalize("100016789"));
    }

    @Test
    void testSerialization() throws Exception {
        // Serialize the attribute
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(postalCodeAttribute);
        oos.close();

        // Deserialize the attribute
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        PostalCodeAttribute deserializedAttribute = (PostalCodeAttribute) ois.readObject();
        ois.close();

        // Test various postal code values with both original and deserialized
        // attributes
        String[] testValues = {
                "10001",
                "10001-6789",
                "90210-1234",
                "30301",
                "60601-2345",
                "K1A 0A7",
                "k1a0a7",
                "M5V 3L9",
                "H3Z2Y7"
        };

        for (String value : testValues) {
            assertEquals(
                    postalCodeAttribute.getName(),
                    deserializedAttribute.getName(),
                    "Attribute names should match");

            assertArrayEquals(
                    postalCodeAttribute.getAliases(),
                    deserializedAttribute.getAliases(),
                    "Attribute aliases should match");

            assertEquals(
                    postalCodeAttribute.normalize(value),
                    deserializedAttribute.normalize(value),
                    "Normalization should be identical for value: " + value);

            assertEquals(
                    postalCodeAttribute.validate(value),
                    deserializedAttribute.validate(value),
                    "Validation should be identical for value: " + value);
        }
    }
}
