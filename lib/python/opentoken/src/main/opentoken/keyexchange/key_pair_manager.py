"""
Copyright (c) Truveta. All rights reserved.
"""

import os
import logging
from pathlib import Path
from typing import Tuple

from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

from opentoken.keyexchange.key_exchange_exception import KeyExchangeException


logger = logging.getLogger(__name__)


class KeyPairManager:
    """
    Manages ECDH key pair lifecycle for OpenToken.

    Handles generation, loading, and saving of elliptic curve key pairs
    used for Diffie-Hellman key exchange. Uses the P-256 (secp256r1) curve
    for broad compatibility and security.
    """

    DEFAULT_KEY_DIR = os.path.join(os.path.expanduser("~"), ".opentoken")
    DEFAULT_PRIVATE_KEY_FILENAME = "keypair.pem"
    DEFAULT_PUBLIC_KEY_FILENAME = "public_key.pem"
    EC_CURVE_DEFAULT_NAME = "p-256"

    CURVE_ALIASES = {
        "p-256": ec.SECP256R1,
        "p256": ec.SECP256R1,
        "secp256r1": ec.SECP256R1,
        "prime256v1": ec.SECP256R1,
        "p-384": ec.SECP384R1,
        "p384": ec.SECP384R1,
        "secp384r1": ec.SECP384R1,
        "p-521": ec.SECP521R1,
        "p521": ec.SECP521R1,
        "secp521r1": ec.SECP521R1,
    }

    def __init__(self, key_directory: str = None, curve_name: str = None):
        """
        Creates a KeyPairManager.

        Args:
            key_directory: The directory to store keys. Defaults to ~/.opentoken.
            curve_name: Optional elliptic curve name (aliases supported, default P-256).
        """
        self.key_directory = key_directory or self.DEFAULT_KEY_DIR
        self.curve_name = curve_name or self.EC_CURVE_DEFAULT_NAME
        self.ec_curve = self._resolve_curve(self.curve_name)

    def get_or_create_key_pair(self) -> Tuple[ec.EllipticCurvePrivateKey, ec.EllipticCurvePublicKey]:
        """
        Loads or generates a key pair.

        If a key pair exists at the default location, it is loaded.
        Otherwise, a new key pair is generated and saved.

        Returns:
            A tuple of (private_key, public_key).

        Raises:
            KeyExchangeException: If key loading or generation fails.
        """
        private_key_path = os.path.join(self.key_directory, self.DEFAULT_PRIVATE_KEY_FILENAME)

        if os.path.exists(private_key_path):
            logger.info(f"Loading existing key pair from {private_key_path}")
            return self.load_key_pair(private_key_path)
        else:
            logger.info("No existing key pair found. Generating new key pair.")
            return self.generate_and_save_key_pair()

    def generate_key_pair(self) -> Tuple[ec.EllipticCurvePrivateKey, ec.EllipticCurvePublicKey]:
        """
        Generates a new ECDH key pair using the P-256 curve.

        Returns:
            A tuple of (private_key, public_key).

        Raises:
            KeyExchangeException: If key generation fails.
        """
        try:
            private_key = ec.generate_private_key(self.ec_curve, default_backend())
            public_key = private_key.public_key()

            logger.debug(f"Generated new ECDH key pair using curve {self.curve_name}")
            return private_key, public_key
        except Exception as e:
            raise KeyExchangeException("Failed to generate ECDH key pair", e)

    def generate_and_save_key_pair(self) -> Tuple[ec.EllipticCurvePrivateKey, ec.EllipticCurvePublicKey]:
        """
        Generates a new key pair and saves it to the default location.

        Returns:
            A tuple of (private_key, public_key).

        Raises:
            KeyExchangeException: If generation or saving fails.
        """
        private_key, public_key = self.generate_key_pair()
        self.save_key_pair(private_key, public_key)
        return private_key, public_key

    def save_key_pair(self, private_key: ec.EllipticCurvePrivateKey, 
                     public_key: ec.EllipticCurvePublicKey) -> None:
        """
        Saves a key pair to the configured directory.

        The private key is saved with restrictive permissions (0o600 on Unix-like systems).

        Args:
            private_key: The private key to save.
            public_key: The public key to save.

        Raises:
            KeyExchangeException: If saving fails.
        """
        try:
            # Create directory if it doesn't exist
            os.makedirs(self.key_directory, exist_ok=True)
            logger.debug(f"Ensured key directory exists: {self.key_directory}")

            # Save private key
            private_key_path = os.path.join(self.key_directory, self.DEFAULT_PRIVATE_KEY_FILENAME)
            self.save_private_key(private_key, private_key_path)

            # Save public key
            public_key_path = os.path.join(self.key_directory, self.DEFAULT_PUBLIC_KEY_FILENAME)
            self.save_public_key(public_key, public_key_path)

            logger.info(f"Saved key pair to {self.key_directory}")
        except Exception as e:
            raise KeyExchangeException("Failed to save key pair", e)

    def save_private_key(self, private_key: ec.EllipticCurvePrivateKey, file_path: str) -> None:
        """
        Saves a private key to a file in PEM format with restrictive permissions.

        Args:
            private_key: The private key to save.
            file_path: The path to save the key.

        Raises:
            KeyExchangeException: If saving fails.
        """
        try:
            pem = private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption()
            )

            with open(file_path, 'wb') as f:
                f.write(pem)

            # Set restrictive permissions (Unix-like systems only)
            try:
                os.chmod(file_path, 0o600)
                logger.debug(f"Set restrictive permissions (0600) on {file_path}")
            except Exception as e:
                logger.warning(f"Failed to set restrictive permissions on {file_path}: {e}")

            logger.debug(f"Saved private key to {file_path}")
        except Exception as e:
            raise KeyExchangeException(f"Failed to save private key to {file_path}", e)

    def save_public_key(self, public_key: ec.EllipticCurvePublicKey, file_path: str) -> None:
        """
        Saves a public key to a file in PEM format.

        Args:
            public_key: The public key to save.
            file_path: The path to save the key.

        Raises:
            KeyExchangeException: If saving fails.
        """
        try:
            pem = public_key.public_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PublicFormat.SubjectPublicKeyInfo
            )

            with open(file_path, 'wb') as f:
                f.write(pem)

            logger.debug(f"Saved public key to {file_path}")
        except Exception as e:
            raise KeyExchangeException(f"Failed to save public key to {file_path}", e)

    def load_key_pair(self, private_key_path: str) -> Tuple[ec.EllipticCurvePrivateKey, ec.EllipticCurvePublicKey]:
        """
        Loads a key pair from the specified private key file path.

        This method loads the private key from the specified path and attempts to load
        the corresponding public key from the same directory (using the standard public key filename).

        Args:
            private_key_path: The path to the private key file.

        Returns:
            A tuple of (private_key, public_key).

        Raises:
            KeyExchangeException: If loading fails.
        """
        try:
            # Load private key
            with open(private_key_path, 'rb') as f:
                private_key = serialization.load_pem_private_key(
                    f.read(),
                    password=None,
                    backend=default_backend()
                )

            # Determine public key path (same directory, standard filename)
            private_path = Path(private_key_path)
            public_key_path = private_path.parent / self.DEFAULT_PUBLIC_KEY_FILENAME

            # Load public key
            if public_key_path.exists():
                with open(public_key_path, 'rb') as f:
                    public_key = serialization.load_pem_public_key(
                        f.read(),
                        backend=default_backend()
                    )
            else:
                raise KeyExchangeException(
                    f"Public key file not found at expected location: {public_key_path}"
                )

            return private_key, public_key
        except Exception as e:
            if isinstance(e, KeyExchangeException):
                raise
            raise KeyExchangeException(f"Failed to load key pair from {private_key_path}", e)

    def get_key_directory(self) -> str:
        """
        Returns the key directory path.

        Returns:
            The key directory.
        """
        return self.key_directory

    def _resolve_curve(self, curve_name: str):
        normalized = curve_name.lower().replace("_", "").replace("-", "").replace(" ", "")
        if normalized in self.CURVE_ALIASES:
            return self.CURVE_ALIASES[normalized]()
        raise KeyExchangeException(f"Unsupported elliptic curve: {curve_name}")
