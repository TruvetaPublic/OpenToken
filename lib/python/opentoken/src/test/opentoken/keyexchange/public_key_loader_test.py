"""
Copyright (c) Truveta. All rights reserved.
"""

import os
import tempfile
import shutil
import pytest

from cryptography.hazmat.primitives import serialization

from opentoken.keyexchange.key_pair_manager import KeyPairManager
from opentoken.keyexchange.public_key_loader import PublicKeyLoader
from opentoken.keyexchange.key_exchange_exception import KeyExchangeException


class TestPublicKeyLoader:
    """Unit tests for PublicKeyLoader."""

    @pytest.fixture
    def temp_dir(self):
        """Create a temporary directory for tests."""
        temp_dir = tempfile.mkdtemp()
        yield temp_dir
        shutil.rmtree(temp_dir, ignore_errors=True)

    @pytest.fixture
    def public_key_loader(self):
        """Create a PublicKeyLoader instance."""
        return PublicKeyLoader()

    @pytest.fixture
    def key_pair_manager(self, temp_dir):
        """Create a KeyPairManager with a temporary directory."""
        return KeyPairManager(temp_dir)

    def test_load_public_key(self, public_key_loader, key_pair_manager, temp_dir):
        """Test loading a public key from a file."""
        # Generate and save a key pair
        key_pair_manager.generate_and_save_key_pair()
        public_key_path = os.path.join(temp_dir, KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME)

        # Load the public key
        loaded_key = public_key_loader.load_public_key(public_key_path)

        assert loaded_key is not None
        assert loaded_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        ) is not None

    def test_load_public_key_from_string(self, public_key_loader, key_pair_manager):
        """Test loading a public key from a PEM string."""
        # Generate a key pair
        _, public_key = key_pair_manager.generate_key_pair()

        # Convert to PEM string
        pem_content = public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )

        # Load from string
        loaded_key = public_key_loader.load_public_key_from_string(pem_content)

        assert loaded_key is not None
        assert loaded_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        ) is not None

    def test_load_public_key_file_not_found(self, public_key_loader, temp_dir):
        """Test loading a non-existent public key file raises an exception."""
        nonexistent_path = os.path.join(temp_dir, "nonexistent.pem")

        with pytest.raises(KeyExchangeException):
            public_key_loader.load_public_key(nonexistent_path)

    def test_validate_public_key(self, public_key_loader, key_pair_manager):
        """Test validating a public key."""
        _, public_key = key_pair_manager.generate_key_pair()

        # Should not raise exception for valid EC public key
        public_key_loader.validate_public_key(public_key)

    def test_validate_public_key_none(self, public_key_loader):
        """Test validating a None public key raises an exception."""
        with pytest.raises(KeyExchangeException):
            public_key_loader.validate_public_key(None)

    def test_public_key_file_exists(self, public_key_loader, key_pair_manager, temp_dir):
        """Test checking if a public key file exists."""
        # Create a key pair file
        key_pair_manager.generate_and_save_key_pair()
        public_key_path = os.path.join(temp_dir, KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME)

        assert public_key_loader.public_key_file_exists(public_key_path)

        # Test non-existent file
        nonexistent_path = os.path.join(temp_dir, "nonexistent.pem")
        assert not public_key_loader.public_key_file_exists(nonexistent_path)

    def test_load_and_validate_public_key(self, public_key_loader, key_pair_manager, temp_dir):
        """Test loading and validating a public key."""
        # Generate and save a key pair
        key_pair_manager.generate_and_save_key_pair()
        public_key_path = os.path.join(temp_dir, KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME)

        # Load the public key (validation happens automatically)
        loaded_key = public_key_loader.load_public_key(public_key_path)

        # Additional explicit validation
        public_key_loader.validate_public_key(loaded_key)

        assert loaded_key is not None
