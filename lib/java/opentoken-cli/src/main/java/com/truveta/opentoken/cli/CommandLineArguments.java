/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli;

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

        @Getter
        @Parameter(names = { "-d",
                        "--decrypt" }, description = "Decrypt mode. Decrypts tokens from input file.", required = false)
        private boolean decrypt = false;

        @Getter
        @Parameter(names = {
                        "--hash-only" }, description = "Hash-only mode. Generates tokens with hashing only, skipping encryption.", required = false)
        private boolean hashOnly = false;

        @Getter
        @Parameter(names = {
                        "--receiver-public-key" }, description = "Path to receiver's public key file for ECDH key exchange.", required = false)
        private String receiverPublicKey = null;

        @Getter
        @Parameter(names = {
                        "--sender-public-key" }, description = "Path to sender's public key file (for decryption with ECDH).", required = false)
        private String senderPublicKey = null;

        @Getter
        @Parameter(names = {
                        "--sender-keypair-path" }, description = "Path to sender's private key file (default: ~/.opentoken/keypair.pem).", required = false)
        private String senderKeypairPath = null;

        @Getter
        @Parameter(names = {
                        "--receiver-keypair-path" }, description = "Path to receiver's private key file (default: ~/.opentoken/keypair.pem).", required = false)
        private String receiverKeypairPath = null;

        @Getter
        @Parameter(names = {
                        "--generate-keypair" }, description = "Generate a new ECDH P-256 key pair and exit.", required = false)
        private boolean generateKeypair = false;

        @Getter
        @Parameter(names = {
                        "--decrypt-with-ecdh" }, description = "Decrypt mode using ECDH key exchange.", required = false)
        private boolean decryptWithEcdh = false;
}
