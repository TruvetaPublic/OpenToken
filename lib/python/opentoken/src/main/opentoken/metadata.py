"""
Copyright (c) Truveta. All rights reserved.
"""

import hashlib
import sys
from typing import Dict, Any


class Metadata:
    """Handles metadata generation and management for OpenToken."""

    # Metadata keys
    PLATFORM = "Platform"
    PYTHON_VERSION = "PythonVersion"
    OPENTOKEN_VERSION = "OpenTokenVersion"
    OUTPUT_FORMAT = "OutputFormat"
    ENCRYPTION_SECRET_HASH = "EncryptionSecretHash"
    HASHING_SECRET_HASH = "HashingSecretHash"
    BLANK_TOKENS_BY_RULE = "BlankTokensByRule"
    
    # Key exchange metadata keys
    KEY_EXCHANGE_METHOD = "KeyExchangeMethod"
    SENDER_PUBLIC_KEY_HASH = "SenderPublicKeyHash"
    RECEIVER_PUBLIC_KEY_HASH = "ReceiverPublicKeyHash"
    CURVE = "Curve"
    
    # Key exchange values
    KEY_EXCHANGE_METHOD_ECDH = "ECDH-P256"
    KEY_EXCHANGE_METHOD_SECRET = "SharedSecret"
    CURVE_P256 = "P-256"

    # Metadata values
    PLATFORM_PYTHON = "Python"
    METADATA_FILE_EXTENSION = ".metadata.json"
    SYSTEM_PYTHON_VERSION = f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}"

    DEFAULT_VERSION = "1.12.1"

    # Output format values
    OUTPUT_FORMAT_JSON = "JSON"
    OUTPUT_FORMAT_CSV = "CSV"
    OUTPUT_FORMAT_PARQUET = "Parquet"

    def __init__(self):
        """Initialize the metadata object."""
        self.metadata_map: Dict[str, Any] = {}

    def initialize(self) -> Dict[str, Any]:
        """
        Initialize metadata with system information only.
        Secret hashes must be set separately using add_hashed_secret().

        Returns:
            The initialized metadata map
        """
        self.metadata_map.clear()
        self.metadata_map[self.PYTHON_VERSION] = self.SYSTEM_PYTHON_VERSION
        self.metadata_map[self.PLATFORM] = self.PLATFORM_PYTHON
        self.metadata_map[self.OPENTOKEN_VERSION] = self.DEFAULT_VERSION

        return self.metadata_map

    def add_hashed_secret(self, secret_key: str, secret_to_hash: str) -> Dict[str, Any]:
        """
        Set the secret and add its hash to the metadata.

        Args:
            secret_key: The key to use for the hashed secret in metadata
            secret_to_hash: The secret to hash

        Returns:
            The metadata map for method chaining
        """
        if secret_to_hash and secret_to_hash.strip():
            self.metadata_map[secret_key] = self.calculate_secure_hash(secret_to_hash)
        return self.metadata_map
    
    def add_key_exchange_metadata(self, sender_public_key_bytes: bytes, 
                                  receiver_public_key_bytes: bytes) -> Dict[str, Any]:
        """
        Add key exchange metadata for ECDH-based encryption.
        
        Args:
            sender_public_key_bytes: The sender's public key bytes
            receiver_public_key_bytes: The receiver's public key bytes
            
        Returns:
            The metadata map for method chaining
        """
        self.metadata_map[self.KEY_EXCHANGE_METHOD] = self.KEY_EXCHANGE_METHOD_ECDH
        self.metadata_map[self.CURVE] = self.CURVE_P256
        
        if sender_public_key_bytes:
            self.metadata_map[self.SENDER_PUBLIC_KEY_HASH] = self.calculate_secure_hash_bytes(sender_public_key_bytes)
        
        if receiver_public_key_bytes:
            self.metadata_map[self.RECEIVER_PUBLIC_KEY_HASH] = self.calculate_secure_hash_bytes(receiver_public_key_bytes)
        
        return self.metadata_map

    @staticmethod
    def calculate_secure_hash(input_str: str) -> str:
        """
        Calculate a secure SHA-256 hash of the given input.
        The hash is returned as a hexadecimal string.

        Args:
            input_str: The input string to hash

        Returns:
            The SHA-256 hash as a hexadecimal string

        Raises:
            HashCalculationException: If there's an error calculating the hash
        """
        if not input_str:
            return None

        try:
            # Create SHA-256 hash
            hash_object = hashlib.sha256(input_str.encode('utf-8'))
            hex_string = hash_object.hexdigest()
            return hex_string

        except Exception as e:
            raise HashCalculationException("Error calculating SHA-256 hash", e)
    
    @staticmethod
    def calculate_secure_hash_bytes(input_bytes: bytes) -> str:
        """
        Calculate a secure SHA-256 hash of the given byte array.
        The hash is returned as a hexadecimal string.

        Args:
            input_bytes: The input bytes to hash

        Returns:
            The SHA-256 hash as a hexadecimal string

        Raises:
            HashCalculationException: If there's an error calculating the hash
        """
        if not input_bytes:
            return None

        try:
            # Create SHA-256 hash
            hash_object = hashlib.sha256(input_bytes)
            hex_string = hash_object.hexdigest()
            return hex_string

        except Exception as e:
            raise HashCalculationException("Error calculating SHA-256 hash", e)


class HashCalculationException(Exception):
    """Custom exception for hash calculation errors."""

    def __init__(self, message: str, cause: Exception = None):
        super().__init__(message)
        self.cause = cause
