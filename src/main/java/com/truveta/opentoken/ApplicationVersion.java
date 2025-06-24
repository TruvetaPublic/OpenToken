/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationVersion {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationVersion.class);
    /**
     * Default version if not found in manifest.
     */
    private ApplicationVersion() {}

    /**
     * Get the application version.
     * @return The application version from manifest or default
     */
    public static String getApplicationVersion() {
        try {
            Manifest manifest = new Manifest(
                Const.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
            String version = manifest.getMainAttributes().getValue("Implementation-Version");
            if (version != null && !version.isEmpty()) {
                return version;
            }
        } catch (Exception e) {
            logger.error("Failed to read version from MANIFEST.MF", e);
        }
        return readVersionFromBumpversionConfig();
    }
    
    /**
     * Reads the current version from .bumpversion.cfg file
     * @return The version string from the config file, or "0.0.0" if not found
     */
    private static String readVersionFromBumpversionConfig() {
        String configPath = ".bumpversion.cfg";
        try {
            if (Files.exists(Paths.get(configPath))) {
                try (Stream<String> lines = Files.lines(Paths.get(configPath))) {
                    return lines
                        .filter(line -> line.contains("current_version"))
                        .findFirst()
                        .map(line -> line.replaceAll(".*current_version\\s*=\\s*([0-9]+\\.[0-9]+\\.[0-9]+).*", "$1"))
                        .orElse("0.0.0");
                }
            }
            
            logger.warn("Could not find .bumpversion.cfg file");
        } catch (IOException e) {
            logger.error("Error reading version from .bumpversion.cfg", e);
        }
        return "0.0.0";
    }
}