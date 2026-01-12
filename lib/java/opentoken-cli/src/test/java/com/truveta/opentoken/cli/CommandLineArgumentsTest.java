/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.truveta.opentoken.cli.commands.DecryptCommand;
import com.truveta.opentoken.cli.commands.GenerateKeypairCommand;
import com.truveta.opentoken.cli.commands.TokenizeCommand;

/**
 * Verifies argument parsing for the CLI subcommands.
 */
class CommandLineArgumentsTest {

    private JCommander buildCommander(String name, Object command) {
        return JCommander.newBuilder().addCommand(name, command).build();
    }

    @Test
    void testTokenizeRequiredParameters() {
        TokenizeCommand command = new TokenizeCommand();
        JCommander jc = buildCommander("tokenize", command);

        jc.parse("tokenize",
                "-i", "input.csv",
                "-t", "csv",
                "-o", "output.csv",
                "--receiver-public-key", "receiver.pem");

        assertEquals("input.csv", command.getInputPath());
        assertEquals("csv", command.getInputType());
        assertEquals("output.csv", command.getOutputPath());
        assertNull(command.getOutputType());
        assertFalse(command.isHashOnly());
        assertEquals("P-256", command.getEcdhCurve());
    }

    @Test
    void testTokenizeAllParameters() {
        TokenizeCommand command = new TokenizeCommand();
        JCommander jc = buildCommander("tokenize", command);

        jc.parse("tokenize",
                "-i", "input.parquet",
                "-t", "parquet",
                "-o", "output.parquet",
                "-ot", "csv",
                "--receiver-public-key", "receiver.pem",
                "--sender-keypair-path", "sender/keypair.pem",
                "--hash-only",
                "--ecdh-curve", "P-384");

        assertEquals("input.parquet", command.getInputPath());
        assertEquals("parquet", command.getInputType());
        assertEquals("output.parquet", command.getOutputPath());
        assertEquals("csv", command.getOutputType());
        assertTrue(command.isHashOnly());
        assertEquals("P-384", command.getEcdhCurve());
        assertEquals("receiver.pem", command.getReceiverPublicKey());
        assertEquals("sender/keypair.pem", command.getSenderKeypairPath());
    }

    @Test
    void testTokenizeMissingRequiredInput() {
        TokenizeCommand command = new TokenizeCommand();
        JCommander jc = buildCommander("tokenize", command);

        assertThrows(ParameterException.class, () -> jc.parse("tokenize",
                "-t", "csv",
                "-o", "output.csv",
                "--receiver-public-key", "receiver.pem"));
    }

    @Test
    void testTokenizeMissingRequiredType() {
        TokenizeCommand command = new TokenizeCommand();
        JCommander jc = buildCommander("tokenize", command);

        assertThrows(ParameterException.class, () -> jc.parse("tokenize",
                "-i", "input.csv",
                "-o", "output.csv",
                "--receiver-public-key", "receiver.pem"));
    }

    @Test
    void testTokenizeMissingRequiredOutput() {
        TokenizeCommand command = new TokenizeCommand();
        JCommander jc = buildCommander("tokenize", command);

        assertThrows(ParameterException.class, () -> jc.parse("tokenize",
                "-i", "input.csv",
                "-t", "csv",
                "--receiver-public-key", "receiver.pem"));
    }

    @Test
    void testTokenizeMissingReceiverPublicKey() {
        TokenizeCommand command = new TokenizeCommand();
        JCommander jc = buildCommander("tokenize", command);

        assertThrows(ParameterException.class, () -> jc.parse("tokenize",
                "-i", "input.csv",
                "-t", "csv",
                "-o", "output.csv"));
    }

    @Test
    void testDecryptRequiredParameters() {
        DecryptCommand command = new DecryptCommand();
        JCommander jc = buildCommander("decrypt", command);

        jc.parse("decrypt",
                "-i", "tokens.csv",
                "-t", "csv",
                "-o", "output.csv",
                "--sender-public-key", "sender.pem",
                "--receiver-keypair-path", "receiver/keypair.pem");

        assertEquals("tokens.csv", command.getInputPath());
        assertEquals("csv", command.getInputType());
        assertEquals("output.csv", command.getOutputPath());
        assertNull(command.getOutputType());
        assertEquals("sender.pem", command.getSenderPublicKey());
        assertEquals("receiver/keypair.pem", command.getReceiverKeypairPath());
        assertEquals("P-256", command.getEcdhCurve());
    }

    @Test
    void testDecryptMissingRequiredInput() {
        DecryptCommand command = new DecryptCommand();
        JCommander jc = buildCommander("decrypt", command);

        assertThrows(ParameterException.class, () -> jc.parse("decrypt",
                "-t", "csv",
                "-o", "output.csv"));
    }

    @Test
    void testDecryptMissingRequiredType() {
        DecryptCommand command = new DecryptCommand();
        JCommander jc = buildCommander("decrypt", command);

        assertThrows(ParameterException.class, () -> jc.parse("decrypt",
                "-i", "tokens.csv",
                "-o", "output.csv"));
    }

    @Test
    void testDecryptMissingRequiredOutput() {
        DecryptCommand command = new DecryptCommand();
        JCommander jc = buildCommander("decrypt", command);

        assertThrows(ParameterException.class, () -> jc.parse("decrypt",
                "-i", "tokens.csv",
                "-t", "csv"));
    }

    @Test
    void testGenerateKeypairDefaults() {
        GenerateKeypairCommand command = new GenerateKeypairCommand();
        JCommander jc = buildCommander("generate-keypair", command);

        jc.parse("generate-keypair");

        assertEquals("P-256", command.getEcdhCurve());
        assertNull(command.getOutputDir());
    }

    @Test
    void testGenerateKeypairCustomValues() {
        GenerateKeypairCommand command = new GenerateKeypairCommand();
        JCommander jc = buildCommander("generate-keypair", command);

        jc.parse("generate-keypair",
                "--ecdh-curve", "P-384",
                "--output-dir", "/tmp/keys");

        assertEquals("P-384", command.getEcdhCurve());
        assertEquals("/tmp/keys", command.getOutputDir());
    }
}
