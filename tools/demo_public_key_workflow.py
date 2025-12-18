#!/usr/bin/env python3
"""
End-to-End Public Key Exchange Workflow Demo

This script demonstrates the complete workflow for using OpenToken with
ECDH-based public key exchange, as described in docs/public-key-workflow.md

Usage:
    python3 demo_public_key_workflow.py

This script will:
1. Generate receiver's key pair
2. Generate sender's key pair
3. Perform ECDH key exchange (sender side)
4. Perform ECDH key exchange (receiver side)
5. Verify both parties derived identical keys
"""

import os
import sys
import tempfile
import shutil
import hashlib
from pathlib import Path

# Add the opentoken library to the path
repo_root = Path(__file__).parent.parent
sys.path.insert(0, str(repo_root / "lib" / "python" / "opentoken" / "src" / "main"))

try:
    from opentoken.keyexchange import KeyPairManager, KeyExchange, PublicKeyLoader
except ImportError as e:
    print("Error: Could not import opentoken library.")
    print("Please install it first:")
    print("  cd lib/python/opentoken")
    print("  pip install -e .")
    sys.exit(1)


def print_header(title):
    """Print a formatted section header."""
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)


def print_step(step_num, description):
    """Print a formatted step."""
    print(f"\n[Step {step_num}] {description}")
    print("-" * 70)


def print_success(message):
    """Print a success message."""
    print(f"✓ {message}")


def print_info(message):
    """Print an info message."""
    print(f"  {message}")


def print_key_fingerprint(key_bytes, label):
    """Print the SHA-256 fingerprint of a key."""
    fingerprint = hashlib.sha256(key_bytes).hexdigest()
    print(f"  {label}: {fingerprint[:32]}...")


def main():
    print_header("OpenToken Public Key Exchange Workflow Demo")
    print("This demo simulates the complete workflow between sender and receiver")
    print("using ECDH key exchange as described in docs/public-key-workflow.md")
    
    # Create temporary directories for this demo
    temp_dir = tempfile.mkdtemp(prefix="opentoken_demo_")
    receiver_dir = os.path.join(temp_dir, "receiver_keys")
    sender_dir = os.path.join(temp_dir, "sender_keys")
    exchange_dir = os.path.join(temp_dir, "exchange")
    
    os.makedirs(receiver_dir)
    os.makedirs(sender_dir)
    os.makedirs(exchange_dir)
    
    try:
        # ===================================================================
        # RECEIVER SIDE
        # ===================================================================
        
        print_step(1, "Receiver Generates Key Pair")
        print_info("Generating P-256 ECDH key pair for receiver...")
        
        receiver_km = KeyPairManager(receiver_dir)
        receiver_private_key, receiver_public_key = receiver_km.get_or_create_key_pair()
        
        print_success(f"Receiver key pair generated")
        print_info(f"Private key: {receiver_dir}/keypair.pem (0600 permissions)")
        print_info(f"Public key:  {receiver_dir}/public_key.pem")
        
        # Get key fingerprints
        from cryptography.hazmat.primitives import serialization
        receiver_pub_pem = receiver_public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )
        print_key_fingerprint(receiver_pub_pem, "Public key fingerprint")
        
        # ===================================================================
        
        print_step(2, "Receiver Shares Public Key")
        print_info("Copying receiver's public key to shared location...")
        
        receiver_public_key_file = os.path.join(exchange_dir, "receiver_public_key.pem")
        shutil.copy(
            os.path.join(receiver_dir, "public_key.pem"),
            receiver_public_key_file
        )
        
        print_success(f"Public key copied to: {receiver_public_key_file}")
        print_info("This file can be safely transmitted to the sender")
        print_info("(No private key information is shared)")
        
        # ===================================================================
        # SENDER SIDE
        # ===================================================================
        
        print_step(3, "Sender Receives Public Key")
        print_info(f"Sender loads receiver's public key from: {receiver_public_key_file}")
        
        public_key_loader = PublicKeyLoader()
        loaded_receiver_public_key = public_key_loader.load_public_key(receiver_public_key_file)
        
        print_success("Receiver's public key loaded successfully")
        print_success("Public key validated (P-256 curve)")
        
        # ===================================================================
        
        print_step(4, "Sender Generates Tokens (ECDH Key Exchange)")
        print_info("Generating sender's key pair (if not exists)...")
        
        sender_km = KeyPairManager(sender_dir)
        sender_private_key, sender_public_key = sender_km.get_or_create_key_pair()
        
        print_success(f"Sender key pair ready")
        print_info(f"Private key: {sender_dir}/keypair.pem")
        print_info(f"Public key:  {sender_dir}/public_key.pem")
        
        print_info("\nPerforming ECDH key exchange...")
        key_exchange = KeyExchange()
        sender_keys = key_exchange.exchange_and_derive_keys(
            sender_private_key,
            loaded_receiver_public_key
        )
        
        sender_hashing_key = sender_keys.get_hashing_key()
        sender_encryption_key = sender_keys.get_encryption_key()
        
        print_success("ECDH key exchange complete")
        print_success("Derived keys using HKDF-SHA256:")
        print_key_fingerprint(sender_hashing_key, "  Hashing key   ")
        print_key_fingerprint(sender_encryption_key, "  Encryption key")
        
        print_info("\n[Note] These derived keys would be used to:")
        print_info("  1. Hash tokens with HMAC-SHA256 (using hashing key)")
        print_info("  2. Encrypt tokens with AES-256-GCM (using encryption key)")
        
        # Save sender's public key (to be sent to receiver)
        sender_public_key_file = os.path.join(exchange_dir, "sender_public_key.pem")
        sender_km.save_public_key(sender_public_key, sender_public_key_file)
        
        print_success(f"\nSender's public key saved to: {sender_public_key_file}")
        print_info("This will be included in the output package for the receiver")
        
        # ===================================================================
        
        print_step(5, "Sender Sends Output to Receiver")
        print_info("In production, sender would package:")
        print_info("  ├─ tokens.csv (or tokens.parquet)")
        print_info("  ├─ tokens.metadata.json")
        print_info("  └─ sender_public_key.pem")
        print_info("\ninto a ZIP file and send to receiver")
        print_success("Package would be sent via secure channel")
        
        # ===================================================================
        # RECEIVER SIDE (DECRYPTION)
        # ===================================================================
        
        print_step(6, "Receiver Decrypts Tokens (ECDH Key Exchange)")
        print_info(f"Receiver loads sender's public key from: {sender_public_key_file}")
        
        loaded_sender_public_key = public_key_loader.load_public_key(sender_public_key_file)
        
        print_success("Sender's public key loaded successfully")
        
        print_info("\nPerforming ECDH key exchange (receiver side)...")
        receiver_keys = key_exchange.exchange_and_derive_keys(
            receiver_private_key,
            loaded_sender_public_key
        )
        
        receiver_hashing_key = receiver_keys.get_hashing_key()
        receiver_encryption_key = receiver_keys.get_encryption_key()
        
        print_success("ECDH key exchange complete")
        print_success("Derived keys using HKDF-SHA256:")
        print_key_fingerprint(receiver_hashing_key, "  Hashing key   ")
        print_key_fingerprint(receiver_encryption_key, "  Encryption key")
        
        # ===================================================================
        
        print_step(7, "Verify Key Derivation")
        print_info("Comparing keys derived by sender and receiver...")
        
        hashing_match = sender_hashing_key == receiver_hashing_key
        encryption_match = sender_encryption_key == receiver_encryption_key
        
        print(f"\n  Hashing keys match:    {hashing_match} {'✓' if hashing_match else '✗'}")
        print(f"  Encryption keys match: {encryption_match} {'✓' if encryption_match else '✗'}")
        
        if hashing_match and encryption_match:
            print_header("SUCCESS: Key Exchange Verified!")
            print("\n✓ Both parties derived IDENTICAL keys from ECDH")
            print("✓ Sender can encrypt tokens, receiver can decrypt them")
            print("✓ No shared secrets were transmitted!")
            print("\nKey Properties:")
            print(f"  • Hashing key length:    {len(sender_hashing_key)} bytes")
            print(f"  • Encryption key length: {len(sender_encryption_key)} bytes")
            print(f"  • Curve used: P-256 (secp256r1)")
            print(f"  • Key derivation: HKDF-SHA256")
            
            print("\nNext Steps in Production:")
            print("  1. Sender encrypts tokens using derived encryption key")
            print("  2. Sender packages tokens + metadata + sender public key")
            print("  3. Receiver uses derived encryption key to decrypt tokens")
            print("  4. Both parties can verify key hashes in metadata.json")
        else:
            print_header("ERROR: Key Derivation Mismatch!")
            print("\n✗ Keys do not match - this should not happen")
            return 1
        
    finally:
        # Cleanup
        print("\n" + "=" * 70)
        print("Cleaning up temporary files...")
        shutil.rmtree(temp_dir, ignore_errors=True)
        print_success(f"Removed temporary directory: {temp_dir}")
    
    print("\n" + "=" * 70)
    print("Demo complete! For full documentation, see:")
    print("  docs/public-key-workflow.md")
    print("=" * 70 + "\n")
    
    return 0


if __name__ == "__main__":
    sys.exit(main())
