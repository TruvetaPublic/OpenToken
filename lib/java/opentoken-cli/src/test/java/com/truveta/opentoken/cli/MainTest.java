/**
 * Copyright (c) Truveta. All rights reserved.
 */
package com.truveta.opentoken.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.truveta.opentoken.keyexchange.KeyPairManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for {@link Main} class.
 * Tests the end-to-end workflows for token generation and decryption.
 */
class MainTest {

    @TempDir
    Path tempDir;

    private Path inputCsv;
    private Path outputCsv;
    private Path outputParquet;
    private PrintStream originalErr;

    // Helper method to generate a keypair directory and return path to files
    private Path createAndSaveKeyPair(String dirName) throws Exception {
        Path keyDir = tempDir.resolve(dirName);
        KeyPairManager kpm = new KeyPairManager(keyDir.toString());
        kpm.generateAndSaveKeyPair();
        return keyDir;
    }

    @BeforeEach
    void setUp() throws IOException {
        // Create test input CSV
        inputCsv = tempDir.resolve("input.csv");
        outputCsv = tempDir.resolve("output.csv");
        outputParquet = tempDir.resolve("output.parquet");

        String csvContent = "RecordId,FirstName,LastName,PostalCode,Sex,BirthDate,SocialSecurityNumber\n"
                + "test-001,John,Doe,98004,Male,2000-01-01,123-45-6789\n"
                + "test-002,Jane,Smith,12345,Female,1990-05-15,234-56-7890\n";
        Files.writeString(inputCsv, csvContent);

        // Capture stderr for error logging verification
        originalErr = System.err;
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }

    @Test
    void testTokenGenerationCsvToCsv() throws Exception {
        Path receiverDir = createAndSaveKeyPair("receiver");
        Path senderDir = createAndSaveKeyPair("sender");

        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", senderDir.resolve("keypair.pem").toString()
        };

        assertDoesNotThrow(() -> Main.main(args));

        assertTrue(Files.exists(outputCsv), "Output CSV should be created");
        assertTrue(Files.size(outputCsv) > 0, "Output CSV should not be empty");

        // Check metadata file
        Path metadataPath = tempDir.resolve("output.metadata.json");
        assertTrue(Files.exists(metadataPath), "Metadata file should be created");
    }

    @Test
    void testTokenGenerationCsvToParquet() throws Exception {
        Path receiverDir = createAndSaveKeyPair("receiver_parquet");
        Path senderDir = createAndSaveKeyPair("sender_parquet");

        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputParquet.toString(),
                "-ot", "parquet",
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", senderDir.resolve("keypair.pem").toString()
        };

        assertDoesNotThrow(() -> Main.main(args));

        assertTrue(Files.exists(outputParquet), "Output Parquet should be created");
        assertTrue(Files.size(outputParquet) > 0, "Output Parquet should not be empty");
    }

    @Test
    void testHashOnlyMode() throws Exception {
        Path receiverDir = createAndSaveKeyPair("receiver_hashonly");

        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--hash-only"
        };

        assertDoesNotThrow(() -> Main.main(args));

        assertTrue(Files.exists(outputCsv), "Output CSV should be created");
        assertTrue(Files.size(outputCsv) > 0, "Output CSV should not be empty");
    }

    @Test
    void testDecryptMode() throws Exception {
        // Generate keypairs
        Path receiverDir = createAndSaveKeyPair("receiver_decrypt");
        Path senderDir = createAndSaveKeyPair("sender_decrypt");

        // First, generate encrypted tokens
        String[] encryptArgs = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", senderDir.resolve("keypair.pem").toString()
        };
        Main.main(encryptArgs);

        // Now decrypt them
        Path decryptedCsv = tempDir.resolve("decrypted.csv");
        String[] decryptArgs = {
                "-d",
                "-i", outputCsv.toString(),
                "-t", "csv",
                "-o", decryptedCsv.toString(),
                "--sender-public-key", senderDir.resolve("public_key.pem").toString(),
                "--receiver-keypair-path", receiverDir.resolve("keypair.pem").toString()
        };

        assertDoesNotThrow(() -> Main.main(decryptArgs));

        assertTrue(Files.exists(decryptedCsv), "Decrypted CSV should be created");
        assertTrue(Files.size(decryptedCsv) > 0, "Decrypted CSV should not be empty");
    }

    @Test
    void testInvalidInputType() throws Exception {
        Path receiverDir = createAndSaveKeyPair("receiver_invalidinput");
        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "invalid",
                "-o", outputCsv.toString(),
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", receiverDir.resolve("keypair.pem").toString()
        };

        // Should not throw, but should log error and return early
        assertDoesNotThrow(() -> Main.main(args));
        // Output should not exist due to early exit
        assertTrue(!Files.exists(outputCsv) || Files.size(outputCsv) == 0);
    }

    @Test
    void testInvalidOutputType() throws Exception {
        Path receiverDir = createAndSaveKeyPair("receiver_invalidoutput");
        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "-ot", "invalid",
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", receiverDir.resolve("keypair.pem").toString()
        };

        // Should not throw, but should log error and return early
        assertDoesNotThrow(() -> Main.main(args));
    }

    @Test
    void testMissingReceiverPublicKey() {
        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "--sender-keypair-path", tempDir.resolve("sender").resolve("keypair.pem").toString()
        };

        // Should not throw, but should log error and return early due to missing receiver public key
        assertDoesNotThrow(() -> Main.main(args));
    }

    @Test
    void testMissingSenderKeypairWithEncryption() throws Exception {
        Path receiverDir = createAndSaveKeyPair("receiver_missing_sender");
        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString()
        };

        // Should not throw, but should still proceed (sender keypair will be created in default location or current dir)
        assertDoesNotThrow(() -> Main.main(args));
    }

    @Test
    void testDecryptModeWithMissingEncryptionKey() throws IOException {
        // Create a dummy encrypted tokens file
        Path dummyTokens = tempDir.resolve("tokens.csv");
        Files.writeString(dummyTokens, "RecordId,RuleId,Token\ntest-001,T1,encrypted_token\n");

        String[] args = {
                "-d",
                "-i", dummyTokens.toString(),
                "-t", "csv",
                "-o", outputCsv.toString()
        };

        // Should not throw, but should log error and return early
        assertDoesNotThrow(() -> Main.main(args));
    }

    @Test
    void testOutputTypeDefaultsToInputType() throws Exception {
        Path receiverDir = createAndSaveKeyPair("receiver_outputdefault");
        Path senderDir = createAndSaveKeyPair("sender_outputdefault");

        String[] args = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", senderDir.resolve("keypair.pem").toString()
        };

        assertDoesNotThrow(() -> Main.main(args));

        // Verify CSV output was created (same as input type)
        assertTrue(Files.exists(outputCsv));
        String content = Files.readString(outputCsv);
        assertTrue(content.contains("RecordId"));
    }

    @Test
    void testParquetInputToParquetOutput() throws Exception {
        // First create a parquet file from CSV
        Path tempParquet = tempDir.resolve("temp.parquet");
        Path receiverDir = createAndSaveKeyPair("receiver_parquet_input");
        Path senderDir = createAndSaveKeyPair("sender_parquet_input");

        String[] createArgs = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", tempParquet.toString(),
                "-ot", "parquet",
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", senderDir.resolve("keypair.pem").toString()
        };
        Main.main(createArgs);

        // Now use parquet as input
        Path outputParquet2 = tempDir.resolve("output2.parquet");
        String[] args = {
                "-d",
                "-i", tempParquet.toString(),
                "-t", "parquet",
                "-o", outputParquet2.toString(),
                "--sender-public-key", senderDir.resolve("public_key.pem").toString(),
                "--receiver-keypair-path", receiverDir.resolve("keypair.pem").toString()
        };

        assertDoesNotThrow(() -> Main.main(args));
    }

    @Test
    void testDecryptCsvToParquet() throws Exception {
        // Generate keypairs
        Path receiverDir = createAndSaveKeyPair("receiver_decrypt_parquet");
        Path senderDir = createAndSaveKeyPair("sender_decrypt_parquet");

        // First generate encrypted tokens
        String[] encryptArgs = {
                "-i", inputCsv.toString(),
                "-t", "csv",
                "-o", outputCsv.toString(),
                "--receiver-public-key", receiverDir.resolve("public_key.pem").toString(),
                "--sender-keypair-path", senderDir.resolve("keypair.pem").toString()
        };
        Main.main(encryptArgs);

        // Decrypt CSV to Parquet
        Path decryptedParquet = tempDir.resolve("decrypted.parquet");
        String[] decryptArgs = {
                "-d",
                "-i", outputCsv.toString(),
                "-t", "csv",
                "-o", decryptedParquet.toString(),
                "-ot", "parquet",
                "--sender-public-key", senderDir.resolve("public_key.pem").toString(),
                "--receiver-keypair-path", receiverDir.resolve("keypair.pem").toString()
        };

        assertDoesNotThrow(() -> Main.main(decryptArgs));
        assertTrue(Files.exists(decryptedParquet));
    }
}
