"""
Copyright (c) Truveta. All rights reserved.
"""


class KeyExchangeException(Exception):
    """
    Exception thrown when key exchange operations fail.

    This exception is used to wrap various cryptographic and I/O exceptions
    that may occur during ECDH key exchange, key derivation, or key management operations.
    """

    def __init__(self, message: str, cause: Exception = None):
        """
        Creates a new KeyExchangeException.

        Args:
            message: The error message.
            cause: The underlying exception that caused this error (optional).
        """
        super().__init__(message)
        self.cause = cause
