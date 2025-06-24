/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken;

import com.beust.jcommander.Parameter;
import lombok.Getter;

/**
 * Processes the application's command line arguments.
 */
public class CommandLineArguments {

    public static final String TYPE_CSV = "csv";
    public static final String TYPE_PARQUET = "parquet";

    @Getter
    @Parameter(names = { "-h",
            "--hashingsecret" }, description = "Hashing Secret to hash token signatures.", required = false)
    private String hashingSecret = null;

    @Getter
    @Parameter(names = { "-e",
            "--encryptionkey" }, description = "Encryption key to encrypt tokens with.", required = false)
    private String encryptionKey = null;

    @Getter
    @Parameter(names = { "-i", "--input" }, description = "Input file path.", required = true)
    private String inputPath = "csv";

    @Getter
    @Parameter(names = { "-t", "--type" }, description = "Input file type.", required = true)
    private String inputType = "";

    @Getter
    @Parameter(names = { "-o", "--output" }, description = "Output file path.", required = true)
    private String outputPath = "";

    @Getter
    @Parameter(names = { "-ot",
            "--output-type" }, description = "Output file type if different from input.", required = false)
    private String outputType = "";
}
