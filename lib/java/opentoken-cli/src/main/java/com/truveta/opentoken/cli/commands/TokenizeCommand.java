/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.Getter;

/**
 * Command for tokenizing person attributes (with or without encryption).
 */
@Parameters(commandDescription = "Tokenize person attributes using ECDH key exchange")
public class TokenizeCommand {

    @Getter
    @Parameter(names = { "-i", "--input" }, description = "Input file path", required = true)
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
            "--receiver-public-key" }, description = "Path to receiver's public key file for ECDH key exchange", required = true)
    private String receiverPublicKey;

    @Getter
    @Parameter(names = {
            "--sender-keypair-path" }, description = "Path to sender's private key file (default: ~/.opentoken/keypair.pem)", required = false)
    private String senderKeypairPath;

    @Getter
    @Parameter(names = {
            "--hash-only" }, description = "Hash-only mode. Generates hashed tokens without encryption", required = false)
    private boolean hashOnly = false;

    @Getter
    @Parameter(names = {
            "--ecdh-curve" }, description = "Elliptic curve name for ECDH (default: P-256 / secp256r1)", required = false)
    private String ecdhCurve = "P-256";
}
