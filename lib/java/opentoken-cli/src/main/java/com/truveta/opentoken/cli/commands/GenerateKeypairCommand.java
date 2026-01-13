/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Getter;

/**
 * Command for generating ECDH key pairs.
 */
@Parameters(commandDescription = "Generate a new ECDH key pair")
public class GenerateKeypairCommand {

        @Getter
        @Parameter(names = {
                        "--ecdh-curve" }, description = "Elliptic curve name for ECDH (default: P-384 / secp384r1)", required = false)
        private String ecdhCurve = "P-384";

        @Getter
        @Parameter(names = {
                        "--output-dir" }, description = "Directory to save the key pair (default: ~/.opentoken)", required = false)
        private String outputDir = null;
}
