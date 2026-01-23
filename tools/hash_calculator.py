#!/usr/bin/env python3
"""Hash calculator for OpenToken metadata fields.

This script calculates SHA-256 hashes that appear in OpenToken `.metadata.json` files.

It supports:
- Public-key hashes (`SenderPublicKeyHash`, `ReceiverPublicKeyHash`) computed from PEM public key files.
- Legacy secret hashes (`HashingSecretHash`, `EncryptionSecretHash`) for backwards compatibility.

Public-key hashing matches the runtime behavior:
- Hash the DER-encoded SubjectPublicKeyInfo bytes of the EC public key.
"""

import argparse
import hashlib
import sys
from typing import Optional


def calculate_public_key_hash_from_pem(pem_path: str) -> str:
    """Calculate the SHA-256 hash (hex) of a PEM public key's DER SubjectPublicKeyInfo bytes."""
    try:
        from cryptography.hazmat.primitives import serialization
    except Exception as e:
        raise RuntimeError(
            "cryptography is required to hash public keys; install OpenToken Python requirements"
        ) from e

    with open(pem_path, 'rb') as f:
        pk = serialization.load_pem_public_key(f.read())

    der = pk.public_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    return hashlib.sha256(der).hexdigest()


def add_legacy_secret_hashes(results: dict, hashing_secret: Optional[str], encryption_key: Optional[str]) -> None:
    """Add legacy secret-hash fields to the results map."""
    if hashing_secret:
        results['HashingSecretHash'] = calculate_secure_hash(hashing_secret)
    if encryption_key:
        results['EncryptionSecretHash'] = calculate_secure_hash(encryption_key)


def add_public_key_hashes(results: dict, sender_public_key: Optional[str], receiver_public_key: Optional[str]) -> None:
    """Add public-key hash fields to the results map."""
    if sender_public_key:
        results['SenderPublicKeyHash'] = calculate_public_key_hash_from_pem(sender_public_key)
    if receiver_public_key:
        results['ReceiverPublicKeyHash'] = calculate_public_key_hash_from_pem(receiver_public_key)


def calculate_secure_hash(input_string: str) -> str:
    """
    Calculates a secure SHA-256 hash of the given input.
    
    This method produces the same hash as the Java implementation:
    MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))
    
    Args:
        input_string: The input string to hash
        
    Returns:
        The SHA-256 hash as a hexadecimal string
        
    Raises:
        ValueError: If input_string is None or empty
    """
    if not input_string:
        raise ValueError("Input string cannot be None or empty")
    
    # Calculate SHA-256 hash using UTF-8 encoding (same as Java StandardCharsets.UTF_8)
    hash_bytes = hashlib.sha256(input_string.encode('utf-8')).digest()
    
    # Convert to hexadecimal string (same format as Java implementation)
    return hash_bytes.hex()


def main():
    """Main function to handle command line arguments and calculate hashes."""
    parser = argparse.ArgumentParser(
        prog='hash_calculator.py',
        description='Calculate secure SHA-256 hashes for OpenToken secrets'
    )
    
    parser.add_argument(
        '--hashing-secret',
        help='(Legacy) hashing secret to calculate HashingSecretHash for',
        dest='hashing_secret'
    )
    
    parser.add_argument(
        '--encryption-key',
        help='(Legacy) encryption key to calculate EncryptionSecretHash for',
        dest='encryption_key'
    )

    parser.add_argument(
        '--sender-public-key',
        help='Path to sender public key PEM to calculate SenderPublicKeyHash for',
        dest='sender_public_key'
    )

    parser.add_argument(
        '--receiver-public-key',
        help='Path to receiver public key PEM to calculate ReceiverPublicKeyHash for',
        dest='receiver_public_key'
    )
    
    parser.add_argument(
        '--output-format',
        choices=['json', 'table', 'simple'],
        default='table',
        help='Output format (default: table)'
    )
    
    # Parse arguments
    args = parser.parse_args()
    
    # Validate that at least one input is provided
    if (not args.hashing_secret and not args.encryption_key
            and not args.sender_public_key and not args.receiver_public_key):
        print("Error: Provide at least one of --sender-public-key, --receiver-public-key, --hashing-secret, or --encryption-key")
        parser.print_help()
        sys.exit(1)
    
    results = {}

    try:
        add_legacy_secret_hashes(results, args.hashing_secret, args.encryption_key)
        add_public_key_hashes(results, args.sender_public_key, args.receiver_public_key)
    
    except ValueError as e:
        print(f"Error: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Unexpected error: {e}")
        sys.exit(1)
    
    # Output results
    if args.output_format == 'json':
        import json
        print(json.dumps(results, indent=2))
    elif args.output_format == 'simple':
        for key, value in results.items():
            print(f"{value}")
    else:  # table format (default)
        print("OpenToken Metadata Hashes")
        print("=" * 50)
        for key, value in results.items():
            print(f"{key:20}: {value}")
        print("=" * 50)
        print("These hashes match those generated by OpenToken")


if __name__ == "__main__":
    main()
