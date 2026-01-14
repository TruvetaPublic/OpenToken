"""
Copyright (c) Truveta. All rights reserved.
"""

import base64
import hashlib
import hmac
import logging
import threading
from typing import Optional, Union
from opentoken.tokens.token import Token
from opentoken.tokentransformer.token_transformer import TokenTransformer


logger = logging.getLogger(__name__)


class HashTokenTransformer(TokenTransformer):
    """
    Transforms the token using a cryptographic hash function with
    a secret key.

    See: https://datatracker.ietf.org/doc/html/rfc4868 (HMACSHA256)
    """

    def __init__(self, hashing_secret: Union[str, bytes, bytearray, memoryview, None]):
        """
        Initializes the underlying MAC with the secret key.

        Args:
            hashing_secret: The cryptographic secret key, as a string, bytes, or None.
                When bytes/bytearray/memoryview are provided, they are used directly
                without charset conversion (matching Java's byte[] constructor).

        Raises:
            ValueError: If the hashing secret is None or empty.
        """
        self.hashing_secret = hashing_secret
        self.hashing_secret_bytes: Optional[bytes] = None
        self._lock = threading.Lock()

        if hashing_secret is None:
            self._mac_available = False
            return

        if isinstance(hashing_secret, (bytes, bytearray, memoryview)):
            self.hashing_secret_bytes = bytes(hashing_secret)
            self._mac_available = len(self.hashing_secret_bytes) > 0
            return

        if str(hashing_secret).strip() == "":
            self._mac_available = False
            return

        self._mac_available = True
        # Preserve exact byte values for ECDH-derived secrets represented as latin-1 strings.
        self.hashing_secret_bytes = str(hashing_secret).encode('latin-1')

    def transform(self, token: str) -> str:
        """
        Hash token transformer.

        The token is transformed using HMAC SHA256 algorithm.

        Args:
            token: The token to be transformed.

        Returns:
            Hashed token in base64 format.

        Raises:
            ValueError: If token is None or blank.
            RuntimeError: If the HMAC is not initialized properly.
        """
        if token is None or token.strip() == "":
            logger.error("Invalid Argument. Token can't be Null.")
            raise ValueError("Invalid Argument. Token can't be Null.")

        if Token.BLANK == token:
            return Token.BLANK  # Return blank token as is

        if not self._mac_available or not self.hashing_secret_bytes:
            raise RuntimeError("HMAC is not properly initialized due to empty hashing secret.")

        with self._lock:
            # Create HMAC with SHA256
            mac = hmac.new(self.hashing_secret_bytes, token.encode('utf-8'), hashlib.sha256)

            # Get the digest and encode to base64
            digest = mac.digest()
            return base64.b64encode(digest).decode('utf-8')

    def __getstate__(self):
        """Custom serialization support."""
        state = self.__dict__.copy()
        # Remove the lock as it can't be pickled
        del state['_lock']
        return state

    def __setstate__(self, state):
        """Custom deserialization support."""
        self.__dict__.update(state)
        # Recreate the lock
        self._lock = threading.Lock()
