"""
Copyright (c) Truveta. All rights reserved.
"""
import base64
import logging
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
from opentoken.tokentransformer.token_transformer import TokenTransformer
from opentoken.tokentransformer.encryption_constants import EncryptionConstants


logger = logging.getLogger(__name__)


class DecryptTokenTransformer(TokenTransformer):
    """
    Transforms the token using AES-256 symmetric decryption.

    See: https://datatracker.ietf.org/doc/html/rfc3826 (AES)
    """

    def __init__(self, encryption_key: str):
        """
        Initializes the underlying cipher (AES) with the decryption secret.

        Args:
            encryption_key: The encryption key. The key must be 32 characters long.

        Raises:
            ValueError: If the encryption key is not 32 characters long.
        """
        if len(encryption_key) != EncryptionConstants.KEY_BYTE_LENGTH:
            logger.error(f"Invalid Argument. Key must be {EncryptionConstants.KEY_BYTE_LENGTH} characters long")
            raise ValueError(f"Key must be {EncryptionConstants.KEY_BYTE_LENGTH} characters long")

        self.encryption_key = encryption_key.encode('utf-8')

    def transform(self, token: str) -> str:
        """
        Decryption token transformer.

        Decrypts the token using AES-256 symmetric decryption algorithm.

        Args:
            token: The encrypted token in base64 format.

        Returns:
            The decrypted token.

        Raises:
            Exception: If decryption fails due to various cryptographic errors.
        """
        try:
            # Decode the base64-encoded token
            message_bytes = base64.b64decode(token)

            # Extract IV, encrypted data, and tag
            iv_bytes = message_bytes[:EncryptionConstants.IV_SIZE]
            ciphertext_and_tag = message_bytes[EncryptionConstants.IV_SIZE:]
            ciphertext = ciphertext_and_tag[:-EncryptionConstants.TAG_LENGTH_BYTES]
            tag = ciphertext_and_tag[-EncryptionConstants.TAG_LENGTH_BYTES:]

            # Create cipher for decryption
            cipher = Cipher(
                algorithms.AES(self.encryption_key),
                modes.GCM(iv_bytes, tag),
                backend=default_backend()
            )

            # Decrypt the token
            decryptor = cipher.decryptor()
            decrypted_bytes = decryptor.update(ciphertext) + decryptor.finalize()

            return decrypted_bytes.decode('utf-8')

        except Exception as e:
            logger.error(f"Error during token decryption: {e}")
            raise
