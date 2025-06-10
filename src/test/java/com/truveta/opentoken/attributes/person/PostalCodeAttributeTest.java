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
        assertEquals("12345", postalCodeAttribute.normalize("12345-6789"));
        assertEquals("12345", postalCodeAttribute.normalize("12345"));
    }

    @Test
    void validate_ShouldReturnTrueForValidPostalCodes() {
        assertTrue(postalCodeAttribute.validate("12345"));
        assertTrue(postalCodeAttribute.validate(" 12345"));
        assertTrue(postalCodeAttribute.validate("12345 "));
        assertTrue(postalCodeAttribute.validate("12345-6789"));
        assertTrue(postalCodeAttribute.validate("01234-6789"));
    }

    @Test
    void normalize_ShouldHandleWhitespace() {
        PostalCodeAttribute attribute = new PostalCodeAttribute();

        // Test different types of whitespace
        assertEquals("12345", attribute.normalize("12345"), "No whitespace");
        assertEquals("12345", attribute.normalize(" 12345"), "Leading space");
        assertEquals("12345", attribute.normalize("12345 "), "Trailing space");
        assertEquals("12345", attribute.normalize(" 12345 "), "Leading and trailing spaces");
        assertEquals("12345", attribute.normalize("1 2 3 4 5"), "Spaces between digits");
        assertEquals("12345", attribute.normalize("12\t345"), "Tab character");
        assertEquals("12345", attribute.normalize("12\n345"), "Newline character");
        assertEquals("12345", attribute.normalize("12\r\n345"), "Carriage return and newline");
        assertEquals("12345", attribute.normalize("  12   345  "), "Multiple spaces");
    }

    @Test
    void validate_ShouldReturnFalseForInvalidPostalCodes() {
        assertFalse(postalCodeAttribute.validate(null), "Null value should not be allowed");
        assertFalse(postalCodeAttribute.validate(""), "Empty value should not be allowed");
        assertFalse(postalCodeAttribute.validate("1234"), "Short postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("123456"), "Long postal code should not be allowed");
        assertFalse(postalCodeAttribute.validate("1234-5678"), "Invalid format should not be allowed");
        assertFalse(postalCodeAttribute.validate("abcde"), "Non-numeric should not be allowed");
    }

    @Test
    void normalize_ThreadSafety() throws InterruptedException {
        final int threadCount = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final List<String> results = Collections.synchronizedList(new ArrayList<>());
        final String testPostalCode = "12345-6789";

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
            assertEquals("12345", result);
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
        assertEquals("12345", postalCodeAttribute.normalize("12345"));

        assertEquals("12345", postalCodeAttribute.normalize(" 12345"));

        // Test more than 5 characters
        assertEquals("12345", postalCodeAttribute.normalize("123456"));
        assertEquals("12345", postalCodeAttribute.normalize("12345-6789"));
        assertEquals("12345", postalCodeAttribute.normalize("123456789"));
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
                "12345",
                "12345-6789",
                "01234-6789",
                "98765",
                "00000-0000",
                "99999",
                "54321-9876"
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
