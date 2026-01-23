"""
Copyright (c) Truveta. All rights reserved.
"""

import os
import logging
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend

from opentoken.keyexchange.key_exchange_exception import KeyExchangeException


logger = logging.getLogger(__name__)


class PublicKeyLoader:
    """
    Loads and validates public keys for OpenToken key exchange.

    Supports loading EC public keys in PEM format from files or strings,
    and validates that they use a supported curve (P-256/P-384/P-521).
    """

    def load_public_key(self, file_path: str) -> ec.EllipticCurvePublicKey:
        """
        Loads a public key from a PEM file.

        Args:
            file_path: The path to the PEM file containing the public key.

        Returns:
            The loaded public key.

        Raises:
            KeyExchangeException: If loading or validation fails.
        """
        try:
            logger.debug(f"Loading public key from {file_path}")

            # Check if file exists
            if not os.path.exists(file_path):
                raise KeyExchangeException(f"Public key file not found: {file_path}")

            with open(file_path, 'rb') as f:
                public_key = serialization.load_pem_public_key(
                    f.read(),
                    backend=default_backend()
                )

            self.validate_public_key(public_key)

            logger.info(f"Successfully loaded and validated public key from {file_path}")
            return public_key
        except Exception as e:
            if isinstance(e, KeyExchangeException):
                raise
            raise KeyExchangeException(f"Failed to load public key from {file_path}", e)

    def load_public_key_from_string(self, pem_content: str) -> ec.EllipticCurvePublicKey:
        """
        Loads a public key from a PEM-encoded string.

        Args:
            pem_content: The PEM-encoded public key content.

        Returns:
            The loaded public key.

        Raises:
            KeyExchangeException: If loading or validation fails.
        """
        try:
            logger.debug("Loading public key from PEM string")

            # Convert string to bytes if needed
            if isinstance(pem_content, str):
                pem_content = pem_content.encode('utf-8')

            public_key = serialization.load_pem_public_key(
                pem_content,
                backend=default_backend()
            )

            self.validate_public_key(public_key)

            logger.info("Successfully loaded and validated public key from PEM string")
            return public_key
        except Exception as e:
            if isinstance(e, KeyExchangeException):
                raise
            raise KeyExchangeException("Failed to load public key from PEM string", e)

    def validate_public_key(self, public_key) -> None:
        """
        Validates that a public key is an EC key using the expected curve.

        Args:
            public_key: The public key to validate.

        Raises:
            KeyExchangeException: If validation fails.
        """
        if public_key is None:
            raise KeyExchangeException("Public key is None")

        # Check if it's an EC key
        if not isinstance(public_key, ec.EllipticCurvePublicKey):
            raise KeyExchangeException(
                f"Invalid key type: expected EC public key, got {type(public_key).__name__}"
            )

        # Get the curve
        curve = public_key.curve
        curve_name = self._get_curve_name(curve)
        logger.debug(f"Public key uses curve: {curve_name}")

        # Validate it's a supported curve
        if not isinstance(curve, (ec.SECP256R1, ec.SECP384R1, ec.SECP521R1)):
            logger.warning(f"Public key uses unsupported curve {curve_name}")

        logger.debug("Public key validation passed")

    def public_key_file_exists(self, file_path: str) -> bool:
        """
        Checks if a public key file exists.

        Args:
            file_path: The path to check.

        Returns:
            True if the file exists, False otherwise.
        """
        return os.path.exists(file_path)

    def _get_curve_name(self, curve: ec.EllipticCurve) -> str:
        """
        Gets the curve name from an EC curve object.

        Args:
            curve: The EC curve.

        Returns:
            A string representing the curve name.
        """
        if isinstance(curve, ec.SECP256R1):
            return "P-256/secp256r1"
        elif isinstance(curve, ec.SECP384R1):
            return "P-384/secp384r1"
        elif isinstance(curve, ec.SECP521R1):
            return "P-521/secp521r1"
        else:
            return f"Unknown ({curve.name})"
