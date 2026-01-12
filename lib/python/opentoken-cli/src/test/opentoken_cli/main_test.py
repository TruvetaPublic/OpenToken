"""
Copyright (c) Truveta. All rights reserved.

Tests for the Python CLI entry point.
"""

import zipfile
from pathlib import Path

import pytest

from opentoken_cli.main import (
    main,
    _create_person_attributes_reader,
    _create_person_attributes_writer,
    _create_token_reader,
    _create_token_writer,
)
from opentoken.keyexchange import KeyPairManager


def _create_keypair_dir(tmp_path: Path, name: str) -> Path:
    """Create an ECDH key pair in a temporary directory."""
    key_dir = tmp_path / name
    manager = KeyPairManager(str(key_dir), curve_name="P-256")
    manager.generate_and_save_key_pair()
    return key_dir


@pytest.fixture
def temp_input_csv(tmp_path):
    """Create a temporary input CSV file for CLI tests."""
    csv_file = tmp_path / "input.csv"
    csv_content = (
        "RecordId,FirstName,LastName,PostalCode,Sex,BirthDate,SocialSecurityNumber\n"
        "test-001,John,Doe,98004,Male,2000-01-01,142-32-9123\n"
        "test-002,Jane,Smith,12345,Female,1990-05-15,221-57-0098\n"
    )
    csv_file.write_text(csv_content)
    return csv_file


class TestReaderWriterFactories:
    """Tests for reader/writer factory functions."""

    def test_create_person_attributes_reader_csv(self, tmp_path):
        """Test creating CSV person attributes reader."""
        csv_file = tmp_path / "test.csv"
        csv_file.write_text("RecordId,FirstName,LastName\nrec-1,John,Doe\n")

        reader = _create_person_attributes_reader(str(csv_file), "csv")
        assert reader is not None
        reader.close()

    def test_create_person_attributes_reader_invalid_type(self):
        """Test that invalid type raises ValueError."""
        with pytest.raises(ValueError, match="Unsupported input type"):
            _create_person_attributes_reader("/path/to/file", "invalid")

    def test_create_person_attributes_writer_csv(self, tmp_path):
        """Test creating CSV person attributes writer."""
        output_file = tmp_path / "output.csv"

        writer = _create_person_attributes_writer(str(output_file), "csv")
        assert writer is not None
        writer.close()

    def test_create_person_attributes_writer_invalid_type(self):
        """Test that invalid output type raises ValueError."""
        with pytest.raises(ValueError, match="Unsupported output type"):
            _create_person_attributes_writer("/path/to/file", "invalid")

    def test_create_token_reader_csv(self, tmp_path):
        """Test creating CSV token reader."""
        csv_file = tmp_path / "tokens.csv"
        csv_file.write_text("RuleId,Token,RecordId\nT1,token123,rec-1\n")

        reader = _create_token_reader(str(csv_file), "csv")
        assert reader is not None
        reader.close()

    def test_create_token_reader_invalid_type(self):
        """Test that invalid input type raises ValueError."""
        with pytest.raises(ValueError, match="Unsupported input type"):
            _create_token_reader("/path/to/file", "invalid")

    def test_create_token_writer_csv(self, tmp_path):
        """Test creating CSV token writer."""
        output_file = tmp_path / "output_tokens.csv"

        writer = _create_token_writer(str(output_file), "csv")
        assert writer is not None
        writer.close()

    def test_create_token_writer_invalid_type(self):
        """Test that invalid output type raises ValueError."""
        with pytest.raises(ValueError, match="Unsupported output type"):
            _create_token_writer("/path/to/file", "invalid")


class TestCliSubcommands:
    """Integration tests for subcommand-based CLI workflows."""

    def test_generate_keypair_subcommand_creates_files(self, tmp_path):
        """Ensure the generate-keypair subcommand writes key material."""
        output_dir = tmp_path / "keys"

        main([
            "generate-keypair",
            "--output-dir", str(output_dir),
            "--ecdh-curve", "P-256",
        ])

        private_key = output_dir / "keypair.pem"
        public_key = output_dir / "public_key.pem"
        assert private_key.exists()
        assert public_key.exists()

    def test_tokenize_creates_encrypted_package(self, temp_input_csv, tmp_path):
        """Tokenize command should emit a ZIP with tokens, metadata, and sender key."""
        receiver_dir = _create_keypair_dir(tmp_path, "receiver")
        sender_dir = _create_keypair_dir(tmp_path, "sender")
        output_zip = tmp_path / "tokens.zip"

        main([
            "tokenize",
            "-i", str(temp_input_csv),
            "-t", "csv",
            "-o", str(output_zip),
            "--receiver-public-key", str(receiver_dir / "public_key.pem"),
            "--sender-keypair-path", str(sender_dir / "keypair.pem"),
        ])

        assert output_zip.exists()
        with zipfile.ZipFile(output_zip) as zf:
            names = set(zf.namelist())
            assert any(name.endswith(".csv") for name in names)
            assert any(name.endswith(".metadata.json") for name in names)
            assert "sender_public_key.pem" in names

    def test_hash_only_mode_creates_package_without_encryption(self, temp_input_csv, tmp_path):
        """Hash-only mode should still package outputs without encryption key usage."""
        receiver_dir = _create_keypair_dir(tmp_path, "receiver_hash")
        sender_dir = _create_keypair_dir(tmp_path, "sender_hash")
        output_zip = tmp_path / "hashed.zip"

        main([
            "tokenize",
            "-i", str(temp_input_csv),
            "-t", "csv",
            "-o", str(output_zip),
            "--receiver-public-key", str(receiver_dir / "public_key.pem"),
            "--sender-keypair-path", str(sender_dir / "keypair.pem"),
            "--hash-only",
        ])

        assert output_zip.exists()
        with zipfile.ZipFile(output_zip) as zf:
            assert any(name.endswith(".csv") for name in zf.namelist())

    def test_decrypt_round_trip_from_zip(self, temp_input_csv, tmp_path):
        """End-to-end encrypt then decrypt workflow using subcommands."""
        receiver_dir = _create_keypair_dir(tmp_path, "receiver_decrypt")
        sender_dir = _create_keypair_dir(tmp_path, "sender_decrypt")
        encrypted_zip = tmp_path / "encrypted.zip"

        main([
            "tokenize",
            "-i", str(temp_input_csv),
            "-t", "csv",
            "-o", str(encrypted_zip),
            "--receiver-public-key", str(receiver_dir / "public_key.pem"),
            "--sender-keypair-path", str(sender_dir / "keypair.pem"),
        ])

        decrypted_csv = tmp_path / "decrypted.csv"
        main([
            "decrypt",
            "-i", str(encrypted_zip),
            "-t", "csv",
            "-o", str(decrypted_csv),
            "--receiver-keypair-path", str(receiver_dir / "keypair.pem"),
        ])

        assert decrypted_csv.exists()
        contents = decrypted_csv.read_text()
        assert "RecordId" in contents

    def test_invalid_input_type_exits(self, temp_input_csv, tmp_path):
        """Invalid input types should raise SystemExit."""
        receiver_dir = _create_keypair_dir(tmp_path, "receiver_invalid")
        sender_dir = _create_keypair_dir(tmp_path, "sender_invalid")
        output_zip = tmp_path / "invalid.zip"

        with pytest.raises(SystemExit):
            main([
                "tokenize",
                "-i", str(temp_input_csv),
                "-t", "invalid",
                "-o", str(output_zip),
                "--receiver-public-key", str(receiver_dir / "public_key.pem"),
                "--sender-keypair-path", str(sender_dir / "keypair.pem"),
            ])
