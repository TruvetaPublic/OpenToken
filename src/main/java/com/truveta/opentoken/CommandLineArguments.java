// Copyright (c) Truveta. All rights reserved.

package com.truveta.opentoken;

import com.beust.jcommander.Parameter;
import lombok.Getter;

/**
 * Processes the application's command line arguments.
 */
public class CommandLineArguments {

    @Getter
    @Parameter(
        names = { "-h", "--hashingsecret" },
        description = "Get Hashing Secret for the tokengen workflow instance.",
        required = false
    )
    private String hashingSecret = null;

    @Getter
    @Parameter(
        names = { "-e", "--encryptionkey" },
        description = "Get encryption key for tokengen workflow instance.",
        required = false
    )
    private String encryptionKey = null;
    
    @Getter
    @Parameter(
        names = { "-i", "--input" },
        description = "Input file path.",
        required = true
    )
    private String inputPath = "csv";

    @Getter
    @Parameter(
        names = { "-t", "--type" },
        description = "Input file type.",
        required = true
    )
    private String inputType = "";

    @Getter
    @Parameter(
        names = { "-o", "--output" },
        description = "Output file path.",
        required = true
    )
    private String outputPath = "";

}
