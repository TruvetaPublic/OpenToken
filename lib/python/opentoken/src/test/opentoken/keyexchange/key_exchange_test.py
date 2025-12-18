"""
Copyright (c) Truveta. All rights reserved.
"""

import pytest

from opentoken.keyexchange.key_pair_manager import KeyPairManager
from opentoken.keyexchange.key_exchange import KeyExchange, DerivedKeys
from opentoken.keyexchange.key_exchange_exception import KeyExchangeException


class TestKeyExchange:
    """Unit tests for KeyExchange."""

    @pytest.fixture
    def key_exchange(self):
        """Create a KeyExchange instance."""
        return KeyExchange()

    @pytest.fixture
    def key_pair_manager(self):
        """Create a KeyPairManager instance."""
        return KeyPairManager()

    def test_perform_key_exchange(self, key_exchange, key_pair_manager):
        """Test performing ECDH key exchange."""
        # Generate two key pairs (simulating sender and receiver)
        sender_private, sender_public = key_pair_manager.generate_key_pair()
        receiver_private, receiver_public = key_pair_manager.generate_key_pair()

        # Perform key exchange
        shared_secret = key_exchange.perform_key_exchange(sender_private, receiver_public)

        assert shared_secret is not None
        assert len(shared_secret) > 0

        # Verify symmetric property: both parties should derive the same shared secret
        shared_secret2 = key_exchange.perform_key_exchange(receiver_private, sender_public)

        assert shared_secret == shared_secret2

    def test_derive_hashing_key(self, key_exchange, key_pair_manager):
        """Test deriving a hashing key."""
        sender_private, _ = key_pair_manager.generate_key_pair()
        _, receiver_public = key_pair_manager.generate_key_pair()

        shared_secret = key_exchange.perform_key_exchange(sender_private, receiver_public)
        hashing_key = key_exchange.derive_hashing_key(shared_secret)

        assert hashing_key is not None
        assert len(hashing_key) == KeyExchange.KEY_LENGTH

    def test_derive_encryption_key(self, key_exchange, key_pair_manager):
        """Test deriving an encryption key."""
        sender_private, _ = key_pair_manager.generate_key_pair()
        _, receiver_public = key_pair_manager.generate_key_pair()

        shared_secret = key_exchange.perform_key_exchange(sender_private, receiver_public)
        encryption_key = key_exchange.derive_encryption_key(shared_secret)

        assert encryption_key is not None
        assert len(encryption_key) == KeyExchange.KEY_LENGTH

    def test_derive_keys(self, key_exchange, key_pair_manager):
        """Test deriving both keys."""
        sender_private, _ = key_pair_manager.generate_key_pair()
        _, receiver_public = key_pair_manager.generate_key_pair()

        shared_secret = key_exchange.perform_key_exchange(sender_private, receiver_public)
        keys = key_exchange.derive_keys(shared_secret)

        assert keys is not None
        assert keys.get_hashing_key() is not None
        assert keys.get_encryption_key() is not None
        assert len(keys.get_hashing_key()) == KeyExchange.KEY_LENGTH
        assert len(keys.get_encryption_key()) == KeyExchange.KEY_LENGTH

        # Verify that hashing and encryption keys are different
        assert keys.get_hashing_key() != keys.get_encryption_key()

    def test_exchange_and_derive_keys(self, key_exchange, key_pair_manager):
        """Test complete key exchange and derivation."""
        sender_private, _ = key_pair_manager.generate_key_pair()
        _, receiver_public = key_pair_manager.generate_key_pair()

        keys = key_exchange.exchange_and_derive_keys(sender_private, receiver_public)

        assert keys is not None
        assert keys.get_hashing_key() is not None
        assert keys.get_encryption_key() is not None

    def test_symmetric_key_derivation(self, key_exchange, key_pair_manager):
        """Test that both parties derive the same keys."""
        # Generate two key pairs
        sender_private, sender_public = key_pair_manager.generate_key_pair()
        receiver_private, receiver_public = key_pair_manager.generate_key_pair()

        # Sender derives keys
        sender_keys = key_exchange.exchange_and_derive_keys(sender_private, receiver_public)

        # Receiver derives keys
        receiver_keys = key_exchange.exchange_and_derive_keys(receiver_private, sender_public)

        # Both should derive the same keys
        assert sender_keys.get_hashing_key() == receiver_keys.get_hashing_key()
        assert sender_keys.get_encryption_key() == receiver_keys.get_encryption_key()

    def test_derived_keys_as_string(self, key_exchange, key_pair_manager):
        """Test getting keys as strings."""
        sender_private, _ = key_pair_manager.generate_key_pair()
        _, receiver_public = key_pair_manager.generate_key_pair()

        keys = key_exchange.exchange_and_derive_keys(sender_private, receiver_public)

        hashing_key_string = keys.get_hashing_key_as_string()
        encryption_key_string = keys.get_encryption_key_as_string()

        assert hashing_key_string is not None
        assert encryption_key_string is not None
        assert len(hashing_key_string) == KeyExchange.KEY_LENGTH
        assert len(encryption_key_string) == KeyExchange.KEY_LENGTH

    def test_derived_keys_getters_return_copies(self, key_exchange, key_pair_manager):
        """Test that key getters return consistent, immutable values."""
        sender_private, _ = key_pair_manager.generate_key_pair()
        _, receiver_public = key_pair_manager.generate_key_pair()

        keys = key_exchange.exchange_and_derive_keys(sender_private, receiver_public)

        # Get keys twice
        hashing_key1 = keys.get_hashing_key()
        hashing_key2 = keys.get_hashing_key()

        # They should be equal
        assert hashing_key1 == hashing_key2

        # Keys are bytes, which are immutable in Python, so internal state cannot be modified
        # Verify keys remain consistent
        hashing_key3 = keys.get_hashing_key()
        assert hashing_key3 == hashing_key1

    def test_consistent_derivation_with_same_shared_secret(self, key_exchange, key_pair_manager):
        """Test that deriving keys from the same shared secret produces consistent results."""
        sender_private, _ = key_pair_manager.generate_key_pair()
        _, receiver_public = key_pair_manager.generate_key_pair()

        shared_secret = key_exchange.perform_key_exchange(sender_private, receiver_public)

        # Derive keys multiple times from the same shared secret
        keys1 = key_exchange.derive_keys(shared_secret)
        keys2 = key_exchange.derive_keys(shared_secret)

        # Should be identical
        assert keys1.get_hashing_key() == keys2.get_hashing_key()
        assert keys1.get_encryption_key() == keys2.get_encryption_key()
