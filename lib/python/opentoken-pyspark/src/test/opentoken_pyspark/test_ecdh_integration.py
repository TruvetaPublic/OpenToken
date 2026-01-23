"""
Tests for ECDH key exchange integration in OpenTokenProcessor.
"""

import unittest
import tempfile
import os
import shutil
from unittest.mock import patch, MagicMock

from opentoken_pyspark.token_processor import OpenTokenProcessor
from opentoken.keyexchange.key_pair_manager import KeyPairManager


class TestECDHIntegration(unittest.TestCase):
    """Test ECDH key exchange functionality."""

    def setUp(self):
        """Set up temporary key directories."""
        self.receiver_key_dir = tempfile.mkdtemp(prefix="test_receiver_")
        self.sender_key_dir = tempfile.mkdtemp(prefix="test_sender_")

    def tearDown(self):
        """Clean up temporary directories."""
        shutil.rmtree(self.receiver_key_dir, ignore_errors=True)
        shutil.rmtree(self.sender_key_dir, ignore_errors=True)

    def test_from_ecdh_generates_processor(self):
        """Test that from_ecdh creates a processor with valid keys."""
        # Generate keypairs
        receiver_mgr = KeyPairManager(key_directory=self.receiver_key_dir, curve_name="P-384")
        receiver_mgr.generate_and_save_key_pair()

        sender_mgr = KeyPairManager(key_directory=self.sender_key_dir, curve_name="P-384")
        sender_mgr.generate_and_save_key_pair()

        # Create processor using ECDH
        processor = OpenTokenProcessor.from_ecdh(
            receiver_public_key_path=os.path.join(self.receiver_key_dir, "public_key.pem"),
            sender_keypair_path=os.path.join(self.sender_key_dir, "keypair.pem"),
            ecdh_curve="P-384"
        )

        # Verify processor has secrets
        self.assertIsNotNone(processor.hashing_secret)
        self.assertIsNotNone(processor.encryption_key)
        self.assertEqual(len(processor.hashing_secret), 32)  # 32 bytes for Latin-1 encoded
        self.assertEqual(len(processor.encryption_key), 32)

    def test_from_ecdh_with_different_curves(self):
        """Test ECDH with different elliptic curves."""
        for curve in ["P-256", "P-384", "P-521"]:
            with self.subTest(curve=curve):
                # Generate keypairs with specific curve
                receiver_mgr = KeyPairManager(key_directory=self.receiver_key_dir, curve_name=curve)
                receiver_mgr.generate_and_save_key_pair()

                sender_mgr = KeyPairManager(key_directory=self.sender_key_dir, curve_name=curve)
                sender_mgr.generate_and_save_key_pair()

                # Create processor
                processor = OpenTokenProcessor.from_ecdh(
                    receiver_public_key_path=os.path.join(self.receiver_key_dir, "public_key.pem"),
                    sender_keypair_path=os.path.join(self.sender_key_dir, "keypair.pem"),
                    ecdh_curve=curve
                )

                self.assertIsNotNone(processor.hashing_secret)
                self.assertIsNotNone(processor.encryption_key)

                # Clean up for next iteration
                shutil.rmtree(self.receiver_key_dir, ignore_errors=True)
                shutil.rmtree(self.sender_key_dir, ignore_errors=True)
                self.receiver_key_dir = tempfile.mkdtemp(prefix="test_receiver_")
                self.sender_key_dir = tempfile.mkdtemp(prefix="test_sender_")

    def test_from_ecdh_missing_receiver_key(self):
        """Test that missing receiver public key raises FileNotFoundError."""
        # Only generate sender keypair
        sender_mgr = KeyPairManager(key_directory=self.sender_key_dir, curve_name="P-384")
        sender_mgr.generate_and_save_key_pair()

        with self.assertRaises(FileNotFoundError):
            OpenTokenProcessor.from_ecdh(
                receiver_public_key_path=os.path.join(self.receiver_key_dir, "public_key.pem"),
                sender_keypair_path=os.path.join(self.sender_key_dir, "keypair.pem"),
                ecdh_curve="P-384"
            )

    def test_from_ecdh_missing_sender_keypair(self):
        """Test that missing sender keypair raises FileNotFoundError."""
        # Only generate receiver keypair
        receiver_mgr = KeyPairManager(key_directory=self.receiver_key_dir, curve_name="P-384")
        receiver_mgr.generate_and_save_key_pair()

        with self.assertRaises(FileNotFoundError):
            OpenTokenProcessor.from_ecdh(
                receiver_public_key_path=os.path.join(self.receiver_key_dir, "public_key.pem"),
                sender_keypair_path=os.path.join(self.sender_key_dir, "keypair.pem"),
                ecdh_curve="P-384"
            )

    def test_from_ecdh_with_custom_token_definition(self):
        """Test from_ecdh with custom token definition."""
        # Generate keypairs
        receiver_mgr = KeyPairManager(key_directory=self.receiver_key_dir, curve_name="P-384")
        receiver_mgr.generate_and_save_key_pair()

        sender_mgr = KeyPairManager(key_directory=self.sender_key_dir, curve_name="P-384")
        sender_mgr.generate_and_save_key_pair()

        # Create mock token definition
        mock_token_def = MagicMock()

        # Create processor with custom token definition
        processor = OpenTokenProcessor.from_ecdh(
            receiver_public_key_path=os.path.join(self.receiver_key_dir, "public_key.pem"),
            sender_keypair_path=os.path.join(self.sender_key_dir, "keypair.pem"),
            ecdh_curve="P-384",
            token_definition=mock_token_def
        )

        # Verify custom definition was passed through
        self.assertEqual(processor.token_definition, mock_token_def)


if __name__ == "__main__":
    unittest.main()
