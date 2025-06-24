/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import java.util.jar.Manifest;

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
        return Const.DEFAULT_VERSION;
    }   
}