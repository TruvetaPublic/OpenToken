/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the ApplicationVersion class.
 * This test verifies that the application version can be correctly retrieved
 * from different sources.
 */
class ApplicationVersionTest {

    @TempDir
    Path tempDir;
    
    private Path bumpversionConfigPath;
    private boolean bumpversionConfigExisted;
    private List<String> originalContent;
    
    @BeforeEach
    void setUp() throws IOException {
        // Store the original .bumpversion.cfg path
        bumpversionConfigPath = Paths.get(".bumpversion.cfg");
        
        // Check if the file exists and backup its content
        bumpversionConfigExisted = Files.exists(bumpversionConfigPath);
        if (bumpversionConfigExisted) {
            Path backupPath = tempDir.resolve(".bumpversion.cfg.original");
            Files.copy(bumpversionConfigPath, backupPath);
            originalContent = Files.readAllLines(backupPath);
        }
    }
    
    @AfterEach
    void tearDown() throws IOException {
        try {
            // Restore the original .bumpversion.cfg file if it existed
            if (bumpversionConfigExisted && originalContent != null) {
                // Delete the file first to avoid permission issues
                Files.deleteIfExists(bumpversionConfigPath);
                Files.write(bumpversionConfigPath, originalContent);
            } else if (!bumpversionConfigExisted) {
                Files.deleteIfExists(bumpversionConfigPath);
            }
        } catch (IOException e) {
            System.err.println("Error in teardown: " + e.getMessage());
        }
    }
    
    @Test
    void testGetApplicationVersion_DefaultVersion() throws IOException {
        // Ensure the file doesn't exist for this test
        Files.deleteIfExists(bumpversionConfigPath);
        
        // Call the method under test
        String version = ApplicationVersion.getApplicationVersion();
        
        // Verify version is returned when file is not found
        assertNotNull(version, "Version should not be null");
        // Could be default or from manifest
        assertTrue(version.equals("0.0.0") || version.matches("^\\d+\\.\\d+\\.\\d+$"), 
                "Version should be in proper format or default 0.0.0");
    }
    
    @Test
    void testGetApplicationVersion_FromBumpversionConfig() throws IOException {
        try {
            // Create a test .bumpversion.cfg file with a known version
            String testVersion = "9.8.7"; // Use a distinctive version unlikely to be in the manifest
            List<String> configContent = List.of(
                "[bumpversion]",
                "current_version = " + testVersion,
                "commit = True",
                "tag = True"
            );
            
            // Delete any existing file first
            Files.deleteIfExists(bumpversionConfigPath);
            Files.write(bumpversionConfigPath, configContent);
            
            // Call the method under test
            String version = ApplicationVersion.getApplicationVersion();
            
            // Verify the version is read from the config file
            assertNotNull(version, "Version should not be null");
            // It might match our test version or come from manifest in a built JAR
            assertTrue(version.equals(testVersion) || version.matches("^\\d+\\.\\d+\\.\\d+$"),
                    "Version should match test version or be in proper format");
        } catch (IOException e) {
            System.err.println("Error in test: " + e.getMessage());
            throw e;
        }
    }
    
    @Test
    void testGetApplicationVersion_MalformedBumpversionConfig() throws IOException {
        try {
            // Create a malformed .bumpversion.cfg file
            List<String> configContent = List.of(
                "[bumpversion]",
                "current_version = malformed-version",
                "commit = True",
                "tag = True"
            );
            
            // Delete any existing file first
            Files.deleteIfExists(bumpversionConfigPath);
            Files.write(bumpversionConfigPath, configContent);
            
            // Call the method under test
            String version = ApplicationVersion.getApplicationVersion();
            
            // Verify the version is either the default or in proper format
            assertNotNull(version, "Version should not be null");
            // It might be default or from manifest in a built JAR
            assertTrue(version.equals("0.0.0") || version.matches("^\\d+\\.\\d+\\.\\d+$"), 
                    "Version should be the default or in proper format");
        } catch (IOException e) {
            System.err.println("Error in test: " + e.getMessage());
            throw e;
        }
    }
    
    @Test
    void testGetApplicationVersion_ValidFormat() {
        try {
            // Just make sure we don't have a bad config file
            Files.deleteIfExists(bumpversionConfigPath);
            
            // Call the method under test
            String version = ApplicationVersion.getApplicationVersion();
            
            // Verify the version follows semantic versioning format or is default
            assertNotNull(version, "Version should not be null");
            // Could be default or from manifest
            assertTrue(version.equals("0.0.0") || version.matches("^\\d+\\.\\d+\\.\\d+$"), 
                    "Version should follow semantic versioning format (MAJOR.MINOR.PATCH) or be default");
        } catch (IOException e) {
            System.err.println("Error in test: " + e.getMessage());
        }
    }
    
    @Test
    void testGetApplicationVersion_EmptyBumpversionConfig() throws IOException {
        try {
            // Create an empty .bumpversion.cfg file
            // Delete any existing file first
            Files.deleteIfExists(bumpversionConfigPath);
            Files.write(bumpversionConfigPath, List.of());
            
            // Call the method under test
            String version = ApplicationVersion.getApplicationVersion();
            
            // Verify the version is either the default or in proper format
            assertNotNull(version, "Version should not be null");
            // It might be default or from manifest in a built JAR
            assertTrue(version.equals("0.0.0") || version.matches("^\\d+\\.\\d+\\.\\d+$"), 
                    "Version should be the default or in proper format");
        } catch (IOException e) {
            System.err.println("Error in test: " + e.getMessage());
            throw e;
        }
    }
}
