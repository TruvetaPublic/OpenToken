"""Utilities for working with PEM-encoded cryptographic material."""

import base64
import logging
from pathlib import Path
from typing import Union

from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

from opentoken.keyexchange.key_exchange_exception import KeyExchangeException


logger = logging.getLogger(__name__)


class PemUtils:
    """Helpers for encoding, decoding, and loading PEM formatted keys."""

    _LINE_LENGTH = 64

    @staticmethod
    def encode_to_pem(key_data: Union[bytes, bytearray, memoryview], label: str) -> str:
        """Encode binary key material into a PEM string."""
        if not isinstance(label, str) or not label.strip():
            raise ValueError("PEM label must be a non-empty string")

        data = PemUtils._to_bytes(key_data)
        base64_data = base64.b64encode(data).decode("ascii")
        chunks = [base64_data[i:i + PemUtils._LINE_LENGTH]
                  for i in range(0, len(base64_data), PemUtils._LINE_LENGTH)]

        header = f"-----BEGIN {label}-----"
        footer = f"-----END {label}-----"
        return "\n".join([header, *chunks, footer, ""])  # Trailing newline matches OpenSSL output

    @staticmethod
    def decode_pem(pem_content: Union[str, bytes]) -> bytes:
        """Decode the body of a PEM string back into raw bytes."""
        try:
            if isinstance(pem_content, bytes):
                pem_str = pem_content.decode("utf-8")
            else:
                pem_str = pem_content

            body_lines = [
                line.strip() for line in pem_str.strip().splitlines()
                if line and not line.startswith("-----BEGIN") and not line.startswith("-----END")
            ]
            base64_body = "".join(body_lines)
            return base64.b64decode(base64_body, validate=True)
        except Exception as exc:  # pragma: no cover - wrapped for consistent exception type
            logger.error("Failed to decode PEM content", exc_info=exc)
            raise KeyExchangeException("Failed to decode PEM content") from exc

    @staticmethod
    def load_private_key(file_path: str) -> ec.EllipticCurvePrivateKey:
        """Load a private key from a PEM file on disk."""
        try:
            pem_bytes = Path(file_path).read_bytes()
            private_key = serialization.load_pem_private_key(
                pem_bytes,
                password=None,
                backend=default_backend()
            )
            return private_key
        except Exception as exc:
            logger.error("Failed to load private key from %s", file_path, exc_info=exc)
            raise KeyExchangeException(f"Failed to load private key from {file_path}") from exc

    @staticmethod
    def load_public_key(file_path: str) -> ec.EllipticCurvePublicKey:
        """Load a public key from a PEM file on disk."""
        try:
            pem_bytes = Path(file_path).read_bytes()
            public_key = serialization.load_pem_public_key(
                pem_bytes,
                backend=default_backend()
            )
            return public_key
        except Exception as exc:
            logger.error("Failed to load public key from %s", file_path, exc_info=exc)
            raise KeyExchangeException(f"Failed to load public key from {file_path}") from exc

    @staticmethod
    def load_public_key_from_string(pem_content: Union[str, bytes]) -> ec.EllipticCurvePublicKey:
        """Load a PEM-encoded public key from a string."""
        try:
            if isinstance(pem_content, str):
                pem_bytes = pem_content.encode("utf-8")
            else:
                pem_bytes = bytes(pem_content)

            public_key = serialization.load_pem_public_key(
                pem_bytes,
                backend=default_backend()
            )
            return public_key
        except Exception as exc:
            logger.error("Failed to load public key from provided PEM string", exc_info=exc)
            raise KeyExchangeException("Failed to load public key from string") from exc

    @staticmethod
    def _to_bytes(key_data: Union[bytes, bytearray, memoryview]) -> bytes:
        if isinstance(key_data, (bytes, bytearray, memoryview)):
            return bytes(key_data)
        raise TypeError("Key data must be bytes-like")
