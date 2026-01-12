"""Tests for PemUtils."""


import pytest
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

from opentoken.keyexchange.key_exchange_exception import KeyExchangeException
from opentoken.keyexchange.pem_utils import PemUtils


class TestPemUtils:
    """Unit tests for PEM helper utilities."""

    def test_encode_and_decode_round_trip(self):
        """Binary blobs can be wrapped and restored via PEM helpers."""
        raw = b"binary-data-with-null-\x00-bytes"
        label = "TEST DATA"

        pem = PemUtils.encode_to_pem(raw, label)
        assert f"-----BEGIN {label}-----" in pem
        assert f"-----END {label}-----" in pem

        decoded = PemUtils.decode_pem(pem)
        assert decoded == raw

    def test_decode_invalid_pem_raises_exception(self):
        """Invalid PEM bodies bubble up as KeyExchangeException."""
        with pytest.raises(KeyExchangeException):
            PemUtils.decode_pem("-----BEGIN INVALID-----\n%%%%\n-----END INVALID-----")

    def test_load_private_key_from_file(self, tmp_path):
        """Private keys written to disk load successfully."""
        private_key = ec.generate_private_key(ec.SECP256R1(), default_backend())
        pem_bytes = private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        )

        path = tmp_path / "keypair.pem"
        path.write_bytes(pem_bytes)

        loaded_key = PemUtils.load_private_key(str(path))
        assert loaded_key.private_numbers() == private_key.private_numbers()

    def test_load_public_key_from_file_and_string(self, tmp_path):
        """Public keys can be loaded from both files and strings."""
        private_key = ec.generate_private_key(ec.SECP256R1(), default_backend())
        public_key = private_key.public_key()

        pem_bytes = public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )

        path = tmp_path / "public.pem"
        path.write_bytes(pem_bytes)

        from_file = PemUtils.load_public_key(str(path))
        assert from_file.public_numbers() == public_key.public_numbers()

        from_string = PemUtils.load_public_key_from_string(pem_bytes.decode("ascii"))
        assert from_string.public_numbers() == public_key.public_numbers()
