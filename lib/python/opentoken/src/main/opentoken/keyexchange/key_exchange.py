"""
Copyright (c) Truveta. All rights reserved.
"""

import logging

from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.backends import default_backend

from opentoken.keyexchange.key_exchange_exception import KeyExchangeException


logger = logging.getLogger(__name__)


class DerivedKeys:
    """Container for derived keys."""

    def __init__(self, hashing_key: bytes, encryption_key: bytes):
        """
        Creates a DerivedKeys instance.

        Args:
            hashing_key: The derived hashing key.
            encryption_key: The derived encryption key.
        """
        self._hashing_key = bytes(hashing_key)
        self._encryption_key = bytes(encryption_key)

    def get_hashing_key(self) -> bytes:
        """
        Gets the hashing key.

        Returns:
            A copy of the hashing key.
        """
        return bytes(self._hashing_key)

    def get_encryption_key(self) -> bytes:
        """
        Gets the encryption key.

        Returns:
            A copy of the encryption key.
        """
        return bytes(self._encryption_key)

    def get_hashing_key_as_string(self) -> str:
        """
        Gets the hashing key as a string (for use with HashTokenTransformer).

        Returns:
            The hashing key as a Latin-1 encoded string.
        """
        return self._hashing_key.decode('latin-1')

    def get_encryption_key_as_string(self) -> str:
        """
        Gets the encryption key as a string (for use with EncryptTokenTransformer).

        Returns:
            The encryption key as a Latin-1 encoded string.
        """
        return self._encryption_key.decode('latin-1')


class KeyExchange:
    """
    Handles Elliptic Curve Diffie-Hellman (ECDH) key exchange and key derivation.

    This class performs ECDH key agreement between two parties and derives
    separate hashing and encryption keys using HKDF (HMAC-based Key Derivation Function).
    The derived keys are used for HMAC-SHA256 hashing and AES-256-GCM encryption.
    """

    # ECDH algorithm name.
    ECDH_ALGORITHM = "ECDH"

    # HMAC algorithm for key derivation.
    HMAC_ALGORITHM = "HmacSHA256"

    # Salt for hashing key derivation (must be consistent across implementations)
    HASHING_KEY_SALT = b"HASHING"

    # Info string for hashing key derivation (must be consistent across implementations)
    HASHING_KEY_INFO = b"OpenToken-Hash"

    # Salt for encryption key derivation (must be consistent across implementations)
    ENCRYPTION_KEY_SALT = b"ENCRYPTION"

    # Info string for encryption key derivation (must be consistent across implementations)
    ENCRYPTION_KEY_INFO = b"OpenToken-Encrypt"

    # Length of derived keys in bytes (32 bytes = 256 bits)
    KEY_LENGTH = 32

    def perform_key_exchange(self, sender_private_key: ec.EllipticCurvePrivateKey,
                           receiver_public_key: ec.EllipticCurvePublicKey) -> bytes:
        """
        Performs ECDH key agreement between sender and receiver.

        Args:
            sender_private_key: The sender's private key.
            receiver_public_key: The receiver's public key.

        Returns:
            The shared secret bytes.

        Raises:
            KeyExchangeException: If key agreement fails.
        """
        try:
            shared_key = sender_private_key.exchange(ec.ECDH(), receiver_public_key)
            logger.debug(f"Performed ECDH key exchange, shared secret length: {len(shared_key)} bytes")
            return shared_key
        except Exception as e:
            raise KeyExchangeException("Failed to perform ECDH key exchange", e)

    def derive_hashing_key(self, shared_secret: bytes) -> bytes:
        """
        Derives a hashing key from the shared secret using HKDF.

        Args:
            shared_secret: The shared secret from ECDH.

        Returns:
            The derived hashing key (32 bytes).

        Raises:
            KeyExchangeException: If key derivation fails.
        """
        return self._hkdf(shared_secret, self.HASHING_KEY_SALT, self.HASHING_KEY_INFO, self.KEY_LENGTH)

    def derive_encryption_key(self, shared_secret: bytes) -> bytes:
        """
        Derives an encryption key from the shared secret using HKDF.

        Args:
            shared_secret: The shared secret from ECDH.

        Returns:
            The derived encryption key (32 bytes).

        Raises:
            KeyExchangeException: If key derivation fails.
        """
        return self._hkdf(shared_secret, self.ENCRYPTION_KEY_SALT, self.ENCRYPTION_KEY_INFO, self.KEY_LENGTH)

    def derive_keys(self, shared_secret: bytes) -> DerivedKeys:
        """
        Derives both hashing and encryption keys from the shared secret.

        Args:
            shared_secret: The shared secret from ECDH.

        Returns:
            A DerivedKeys object containing both keys.

        Raises:
            KeyExchangeException: If key derivation fails.
        """
        hashing_key = self.derive_hashing_key(shared_secret)
        encryption_key = self.derive_encryption_key(shared_secret)

        logger.debug(f"Derived hashing key ({len(hashing_key)} bytes) and "
                    f"encryption key ({len(encryption_key)} bytes)")

        return DerivedKeys(hashing_key, encryption_key)

    def exchange_and_derive_keys(self, sender_private_key: ec.EllipticCurvePrivateKey,
                                receiver_public_key: ec.EllipticCurvePublicKey) -> DerivedKeys:
        """
        Performs a complete key exchange and derivation.

        Args:
            sender_private_key: The sender's private key.
            receiver_public_key: The receiver's public key.

        Returns:
            A DerivedKeys object containing both derived keys.

        Raises:
            KeyExchangeException: If key exchange or derivation fails.
        """
        shared_secret = self.perform_key_exchange(sender_private_key, receiver_public_key)
        return self.derive_keys(shared_secret)

    def _hkdf(self, ikm: bytes, salt: bytes, info: bytes, length: int) -> bytes:
        """
        HKDF (HMAC-based Key Derivation Function) implementation.

        This performs the Extract-and-Expand paradigm as defined in RFC 5869.

        Args:
            ikm: The input keying material (shared secret).
            salt: The salt value.
            info: The application-specific info string.
            length: The desired output length in bytes.

        Returns:
            The derived key material.

        Raises:
            KeyExchangeException: If key derivation fails.
        """
        try:
            hkdf = HKDF(
                algorithm=hashes.SHA256(),
                length=length,
                salt=salt,
                info=info,
                backend=default_backend()
            )
            return hkdf.derive(ikm)
        except Exception as e:
            raise KeyExchangeException("HKDF key derivation failed", e)
