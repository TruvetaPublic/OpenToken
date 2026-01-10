/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Unit tests for {@link CommandLineArguments}.
 */
class CommandLineArgumentsTest {

    @Test
    void testRequiredParameters() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse("-i", "input.csv", "-t", "csv", "-o", "output.csv");

        assertEquals("input.csv", args.getInputPath());
        assertEquals("csv", args.getInputType());
        assertEquals("output.csv", args.getOutputPath());
    }

    @Test
    void testAllParameters() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(
                        "-i", "input.csv",
                        "-t", "csv",
                        "-o", "output.csv",
                        "-ot", "parquet",
                        "-d",
                        "-h",
                        "--ecdh-curve", "P-384");

        assertEquals("input.csv", args.getInputPath());
        assertEquals("csv", args.getInputType());
        assertEquals("output.csv", args.getOutputPath());
        assertEquals("parquet", args.getOutputType());
        assertTrue(args.isDecryptWithEcdh());
        assertTrue(args.isHashOnly());
        assertEquals("P-384", args.getEcdhCurve());
    }

    @Test
    void testLongParameterNames() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(
                        "--input", "input.parquet",
                        "--type", "parquet",
                        "--output", "output.parquet",
                        "--output-type", "csv",
                        "--decrypt",
                        "--ecdh-curve", "P-521");

        assertEquals("input.parquet", args.getInputPath());
        assertEquals("parquet", args.getInputType());
        assertEquals("output.parquet", args.getOutputPath());
        assertEquals("csv", args.getOutputType());
        assertTrue(args.isDecryptWithEcdh());
        assertEquals("P-521", args.getEcdhCurve());
    }

    @Test
    void testDefaultValues() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse("-i", "input.csv", "-t", "csv", "-o", "output.csv");

        assertEquals("", args.getOutputType());
        assertFalse(args.isDecryptWithEcdh());
        assertFalse(args.isHashOnly());
        assertEquals("P-256", args.getEcdhCurve());
    }

    @Test
    void testMissingRequiredInput() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .build();

        assertThrows(ParameterException.class, () -> jc.parse("-t", "csv", "-o", "output.csv"));
    }

    @Test
    void testMissingRequiredType() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .build();

        assertThrows(ParameterException.class, () -> jc.parse("-i", "input.csv", "-o", "output.csv"));
    }

    @Test
    void testMissingRequiredOutput() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .build();

        assertThrows(ParameterException.class, () -> jc.parse("-i", "input.csv", "-t", "csv"));
    }

    @Test
    void testTypeConstants() {
        assertEquals("csv", CommandLineArguments.TYPE_CSV);
        assertEquals("parquet", CommandLineArguments.TYPE_PARQUET);
    }

    @Test
    void testHashOnlyWithoutDecrypt() {
        CommandLineArguments args = new CommandLineArguments();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(
                        "-i", "input.csv",
                        "-t", "csv",
                        "-o", "output.csv",
                        "--hash-only");

        assertTrue(args.isHashOnly());
        assertFalse(args.isDecryptWithEcdh());
        assertEquals("P-256", args.getEcdhCurve());
    }
}
