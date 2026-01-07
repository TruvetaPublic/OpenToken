"""
Copyright (c) Truveta. All rights reserved.
"""

import os
import tempfile
import shutil
import pytest
from pathlib import Path

from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import serialization

from opentoken.keyexchange.key_pair_manager import KeyPairManager
from opentoken.keyexchange.key_exchange_exception import KeyExchangeException


class TestKeyPairManager:
    """Unit tests for KeyPairManager."""

    @pytest.fixture
    def temp_dir(self):
        """Create a temporary directory for tests."""
        temp_dir = tempfile.mkdtemp()
        yield temp_dir
        shutil.rmtree(temp_dir, ignore_errors=True)

    @pytest.fixture
    def key_pair_manager(self, temp_dir):
        """Create a KeyPairManager with a temporary directory."""
        return KeyPairManager(temp_dir)

    def test_generate_key_pair(self, key_pair_manager):
        """Test generating a key pair."""
        private_key, public_key = key_pair_manager.generate_key_pair()

        assert private_key is not None
        assert public_key is not None
        assert isinstance(private_key, ec.EllipticCurvePrivateKey)
        assert isinstance(public_key, ec.EllipticCurvePublicKey)

        # Verify it's using P-256 curve
        assert isinstance(public_key.curve, ec.SECP256R1)

    def test_generate_and_save_key_pair(self, key_pair_manager, temp_dir):
        """Test generating and saving a key pair."""
        private_key, public_key = key_pair_manager.generate_and_save_key_pair()

        assert private_key is not None
        assert public_key is not None

        # Verify files were created
        private_key_path = os.path.join(temp_dir, KeyPairManager.DEFAULT_PRIVATE_KEY_FILENAME)
        public_key_path = os.path.join(temp_dir, KeyPairManager.DEFAULT_PUBLIC_KEY_FILENAME)

        assert os.path.exists(private_key_path)
        assert os.path.exists(public_key_path)

        # Verify PEM format
        with open(private_key_path, 'rb') as f:
            private_pem = f.read()
            assert b"-----BEGIN PRIVATE KEY-----" in private_pem
            assert b"-----END PRIVATE KEY-----" in private_pem

        with open(public_key_path, 'rb') as f:
            public_pem = f.read()
            assert b"-----BEGIN PUBLIC KEY-----" in public_pem
            assert b"-----END PUBLIC KEY-----" in public_pem

    def test_save_and_load_key_pair(self, key_pair_manager, temp_dir):
        """Test saving and loading a key pair."""
        # Generate and save
        original_private, original_public = key_pair_manager.generate_and_save_key_pair()

        # Load
        private_key_path = os.path.join(temp_dir, KeyPairManager.DEFAULT_PRIVATE_KEY_FILENAME)
        loaded_private, loaded_public = key_pair_manager.load_key_pair(private_key_path)

        assert loaded_private is not None
        assert loaded_public is not None

        # Verify the keys are equivalent
        original_private_bytes = original_private.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        )
        loaded_private_bytes = loaded_private.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        )
        assert original_private_bytes == loaded_private_bytes

        original_public_bytes = original_public.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )
        loaded_public_bytes = loaded_public.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )
        assert original_public_bytes == loaded_public_bytes

    def test_get_or_create_key_pair_creates_new(self, key_pair_manager, temp_dir):
        """Test get_or_create_key_pair creates a new key pair when none exists."""
        private_key, public_key = key_pair_manager.get_or_create_key_pair()

        assert private_key is not None
        assert public_key is not None

        # Verify files were created
        private_key_path = os.path.join(temp_dir, KeyPairManager.DEFAULT_PRIVATE_KEY_FILENAME)
        assert os.path.exists(private_key_path)

    def test_get_or_create_key_pair_loads_existing(self, key_pair_manager):
        """Test get_or_create_key_pair loads existing key pair."""
        # First call creates
        first_private, first_public = key_pair_manager.get_or_create_key_pair()

        # Second call loads
        second_private, second_public = key_pair_manager.get_or_create_key_pair()

        # Should be the same key pair
        first_private_bytes = first_private.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        )
        second_private_bytes = second_private.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        )
        assert first_private_bytes == second_private_bytes

    def test_load_key_pair_file_not_found(self, key_pair_manager, temp_dir):
        """Test loading a non-existent key pair raises an exception."""
        nonexistent_path = os.path.join(temp_dir, "nonexistent.pem")

        with pytest.raises(KeyExchangeException):
            key_pair_manager.load_key_pair(nonexistent_path)

    def test_save_public_key(self, key_pair_manager, temp_dir):
        """Test saving a public key."""
        private_key, public_key = key_pair_manager.generate_key_pair()
        public_key_path = os.path.join(temp_dir, "test_public.pem")

        key_pair_manager.save_public_key(public_key, public_key_path)

        assert os.path.exists(public_key_path)

        # Verify content
        with open(public_key_path, 'rb') as f:
            content = f.read()
            assert b"-----BEGIN PUBLIC KEY-----" in content
            assert b"-----END PUBLIC KEY-----" in content

    def test_save_private_key(self, key_pair_manager, temp_dir):
        """Test saving a private key."""
        private_key, _ = key_pair_manager.generate_key_pair()
        private_key_path = os.path.join(temp_dir, "test_private.pem")

        key_pair_manager.save_private_key(private_key, private_key_path)

        assert os.path.exists(private_key_path)

        # Verify content
        with open(private_key_path, 'rb') as f:
            content = f.read()
            assert b"-----BEGIN PRIVATE KEY-----" in content
            assert b"-----END PRIVATE KEY-----" in content

    def test_get_key_directory(self, key_pair_manager, temp_dir):
        """Test getting the key directory."""
        assert key_pair_manager.get_key_directory() == temp_dir

    def test_default_constructor(self):
        """Test KeyPairManager with default constructor."""
        manager = KeyPairManager()
        assert manager.get_key_directory() == KeyPairManager.DEFAULT_KEY_DIR
