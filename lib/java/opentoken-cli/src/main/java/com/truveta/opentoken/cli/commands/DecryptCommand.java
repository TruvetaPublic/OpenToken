/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Getter;

/**
 * Command for decrypting tokens using ECDH key exchange.
 */
@Parameters(commandDescription = "Decrypt tokens using ECDH key exchange")
public class DecryptCommand {

        @Getter
        @Parameter(names = { "-i", "--input" }, description = "Input file path (or ZIP package)", required = true)
        private String inputPath;

        @Getter
        @Parameter(names = { "-t", "--type" }, description = "Input file type (csv or parquet)", required = true)
        private String inputType;

        @Getter
        @Parameter(names = { "-o", "--output" }, description = "Output file path", required = true)
        private String outputPath;

        @Getter
        @Parameter(names = { "-ot",
                        "--output-type" }, description = "Output file type if different from input (csv or parquet)", required = false)
        private String outputType;

        @Getter
        @Parameter(names = {
                        "--sender-public-key" }, description = "Path to sender's public key file (extracted from ZIP if not provided)", required = false)
        private String senderPublicKey;

        @Getter
        @Parameter(names = {
                        "--receiver-keypair-path" }, description = "Path to receiver's private key file (default: ~/.opentoken/keypair.pem)", required = false)
        private String receiverKeypairPath;

        @Getter
        @Parameter(names = {
                        "--ecdh-curve" }, description = "Elliptic curve name for ECDH (default: P-384 / secp384r1)", required = false)
        private String ecdhCurve = "P-384";
}
