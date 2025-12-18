# Public Key Exchange Workflow - End-to-End Guide

This guide demonstrates the complete workflow for using OpenToken with public-key cryptography and ECDH key exchange. This replaces the traditional secret-based approach with a more secure key exchange mechanism.

## Table of Contents

- [Overview](#overview)
- [CLI Quick Reference](#cli-quick-reference)
- [Prerequisites](#prerequisites)
- [Complete Workflow](#complete-workflow)
  - [Step 1: Receiver Generates Key Pair](#step-1-receiver-generates-key-pair)
  - [Step 2: Receiver Shares Public Key](#step-2-receiver-shares-public-key)
  - [Step 3: Sender Receives Public Key](#step-3-sender-receives-public-key)
  - [Step 4: Sender Generates Tokens](#step-4-sender-generates-tokens)
  - [Step 5: Sender Sends Output to Receiver](#step-5-sender-sends-output-to-receiver)
  - [Step 6: Receiver Decrypts Tokens](#step-6-receiver-decrypts-tokens)
  - [Step 7: Verify Key Derivation](#step-7-verify-key-derivation)
- [Key Concepts](#key-concepts)
- [Security Considerations](#security-considerations)
- [Troubleshooting](#troubleshooting)

---

## Overview

The public-key workflow uses **Elliptic Curve Diffie-Hellman (ECDH)** for secure key exchange:

1. **Receiver** generates an ECDH key pair and shares the public key
2. **Sender** uses the receiver's public key to derive encryption keys
3. Both parties can independently derive the **same** hashing and encryption keys
4. Tokens are encrypted with these derived keys
5. Output includes the sender's public key for receiver to decrypt

```
┌────────────┐                           ┌─────────────┐
│  Receiver  │                           │   Sender    │
└─────┬──────┘                           └──────┬──────┘
      │                                         │
      │ 1. Generate key pair                    │
      │    (P-256 curve)                        │
      │                                         │
      │ 2. Share public key ────────────────────>
      │                                         │
      │                                         │ 3. Generate key pair
      │                                         │    (if not exists)
      │                                         │
      │                                         │ 4. Perform ECDH
      │                                         │    Derive keys via HKDF
      │                                         │
      │                                         │ 5. Generate & encrypt tokens
      │                                         │
      │ 6. Receive output.zip <─────────────────┤
      │    ├─ tokens.csv/parquet                │
      │    ├─ metadata.json                     │
      │    └─ sender_public_key.pem             │
      │                                         │
      │ 7. Perform ECDH                         │
      │    Derive same keys                     │
      │                                         │
      │ 8. Decrypt tokens                       │
      └─────────────────────────────────────────┘
```

---

## CLI Quick Reference

This section provides a quick overview of the CLI commands for the public key workflow. **Note:** CLI integration is currently in progress (Phases 3-5). The commands below show the planned interface.

### Receiver: Generate Key Pair

```bash
# Java
java -jar opentoken-cli/target/opentoken-cli-*.jar --generate-keypair

# Python
python -m opentoken_cli.main --generate-keypair
```

**Output:** Keys saved to `~/.opentoken/keypair.pem` (private) and `~/.opentoken/public_key.pem` (public)

### Sender: Generate Encrypted Tokens

```bash
# Java
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  --receiver-public-key ~/opentoken_exchange/receiver_public_key.pem \
  -i ../../resources/sample.csv \
  -t csv \
  -o ../../resources/output.zip

# Python
python -m opentoken_cli.main \
  --receiver-public-key ~/opentoken_exchange/receiver_public_key.pem \
  -i ../../../resources/sample.csv \
  -t csv \
  -o ../../../resources/output.zip
```

**What happens:**
1. CLI loads receiver's public key
2. CLI generates sender's key pair (if not exists) in `~/.opentoken/`
3. CLI performs ECDH key exchange to derive keys
4. CLI generates and encrypts tokens
5. CLI creates ZIP package: `output.zip` containing:
   - `tokens.csv` (encrypted)
   - `tokens.metadata.json`
   - `sender_public_key.pem`

### Receiver: Decrypt Tokens

```bash
# Java
java -jar opentoken-cli/target/opentoken-cli-*.jar --decrypt-with-ecdh \
  -i ../../received_tokens/output.zip \
  -o ../../received_tokens/decrypted_tokens.csv

# Python
python -m opentoken_cli.main --decrypt-with-ecdh \
  -i ../../../received_tokens/output.zip \
  -o ../../../received_tokens/decrypted_tokens.csv
```

**What happens:**
1. CLI extracts sender's public key from ZIP
2. CLI loads receiver's private key from `~/.opentoken/`
3. CLI performs ECDH key exchange (derives same keys as sender)
4. CLI decrypts tokens
5. CLI writes decrypted output

### Key CLI Arguments

| Argument | Description |
|----------|-------------|
| `--generate-keypair` | Generate a new ECDH P-256 key pair |
| `--receiver-public-key <path>` | Path to receiver's public key (for sender) |
| `--sender-public-key <path>` | Path to sender's public key (for receiver, optional if in ZIP) |
| `--sender-keypair-path <path>` | Custom location for sender's key pair (default: `~/.opentoken/`) |
| `--receiver-keypair-path <path>` | Custom location for receiver's key pair (default: `~/.opentoken/`) |
| `--decrypt-with-ecdh` | Decrypt mode using ECDH key exchange |
| `-i, --input <path>` | Input file path |
| `-o, --output <path>` | Output file path (use `.zip` extension for packaged output) |
| `-t, --type <format>` | Input/output format (`csv` or `parquet`) |

---

## Prerequisites

### Java Implementation
- Java 11 or higher
- Maven 3.6+
- OpenToken library (Java)

### Python Implementation
- Python 3.8 or higher
- OpenToken library (Python)
- `cryptography>=42.0.2`

### Installation

**Java:**
```bash
cd lib/java
mvn clean install
```

**Python:**
```bash
cd lib/python/opentoken
pip install -e .
```

---

## Complete Workflow

### Step 1: Receiver Generates Key Pair

The receiver must first generate an ECDH P-256 key pair. This only needs to be done once and can be reused for multiple data exchanges.

#### Option A: Using CLI (Recommended)

**Java CLI:**

```bash
# Generate receiver key pair
cd lib/java
java -jar opentoken-cli/target/opentoken-cli-*.jar --generate-keypair

# Output:
# ✓ Key pair generated successfully
# ✓ Private key saved to: /home/user/.opentoken/keypair.pem (0600 permissions)
# ✓ Public key saved to: /home/user/.opentoken/public_key.pem
```

**Python CLI:**

```bash
# Generate receiver key pair
cd lib/python/opentoken-cli
python -m opentoken_cli.main --generate-keypair

# Output:
# ✓ Key pair generated successfully
# ✓ Private key saved to: /home/user/.opentoken/keypair.pem (0600 permissions)
# ✓ Public key saved to: /home/user/.opentoken/public_key.pem
```

**Note:** The CLI commands above show the planned interface. Current implementation status: **In Progress (Phase 3-4)**

#### Option B: Using Programmatic API (Currently Available)

**Using Java:**

```java
import com.truveta.opentoken.keyexchange.KeyPairManager;
import java.security.KeyPair;

KeyPairManager keyPairManager = new KeyPairManager();
KeyPair receiverKeyPair = keyPairManager.getOrCreateKeyPair();

// Keys are automatically saved to ~/.opentoken/
// - Private key: ~/.opentoken/keypair.pem
// - Public key: ~/.opentoken/public_key.pem

System.out.println("Receiver key pair generated and saved to: " + 
                   keyPairManager.getKeyDirectory());
```

**Using Python:**

```python
from opentoken.keyexchange import KeyPairManager

key_pair_manager = KeyPairManager()
receiver_private_key, receiver_public_key = key_pair_manager.get_or_create_key_pair()

# Keys are automatically saved to ~/.opentoken/
# - Private key: ~/.opentoken/keypair.pem  
# - Public key: ~/.opentoken/public_key.pem

print(f"Receiver key pair generated and saved to: {key_pair_manager.get_key_directory()}")
```

**Quick Script (Python):**

```bash
# Create a simple script to generate keys
cat > generate_receiver_keys.py << 'EOF'
#!/usr/bin/env python3
from opentoken.keyexchange import KeyPairManager

key_pair_manager = KeyPairManager()
private_key, public_key = key_pair_manager.get_or_create_key_pair()
print(f"✓ Receiver key pair generated")
print(f"✓ Private key: ~/.opentoken/keypair.pem")
print(f"✓ Public key: ~/.opentoken/public_key.pem")
EOF

python3 generate_receiver_keys.py
```

**Verify the keys were created:**

```bash
ls -la ~/.opentoken/
# Expected output:
# -rw-------  1 user  group  227 Dec 18 10:00 keypair.pem        # Private key (0600 permissions)
# -rw-r--r--  1 user  group  178 Dec 18 10:00 public_key.pem     # Public key
```

---

### Step 2: Receiver Shares Public Key

The receiver needs to share **only the public key** with the sender. The private key must remain secret.

**Package the public key:**

```bash
# Copy the public key to a shared location
cp ~/.opentoken/public_key.pem /path/to/shared/receiver_public_key.pem

# Or create a shareable archive
tar -czf receiver_public_key.tar.gz -C ~/.opentoken public_key.pem
```

**Example public key content:**

```
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8JI9+xLm5uF7Gk3TpQqR8WzY7Jkl
mNcHfGh9EpVzMxKkYjJhGfDcWvL0pRsT8mNyQwXzUfVgHjKlMnOpQrStBg==
-----END PUBLIC KEY-----
```

**Security Note:** The public key can be safely transmitted over insecure channels (email, cloud storage, etc.). Only the private key needs to be kept secret.

---

### Step 3: Sender Receives Public Key

The sender receives the receiver's public key and saves it locally.

```bash
# Save received public key
mkdir -p ~/opentoken_exchange
cp /path/to/received/receiver_public_key.pem ~/opentoken_exchange/receiver_public_key.pem

# Verify the key format
cat ~/opentoken_exchange/receiver_public_key.pem
# Should show: -----BEGIN PUBLIC KEY----- ... -----END PUBLIC KEY-----
```

**Validate the public key (optional):**

**Using Python:**

```python
from opentoken.keyexchange import PublicKeyLoader

loader = PublicKeyLoader()
try:
    public_key = loader.load_public_key("~/opentoken_exchange/receiver_public_key.pem")
    print("✓ Public key is valid")
except Exception as e:
    print(f"✗ Invalid public key: {e}")
```

---

### Step 4: Sender Generates Tokens

The sender now runs OpenToken with the receiver's public key to generate encrypted tokens. The CLI will automatically:
1. Load or generate the sender's key pair
2. Perform ECDH key exchange with the receiver's public key
3. Derive hashing and encryption keys using HKDF
4. Generate and encrypt tokens
5. Package output as ZIP with sender's public key included

#### Option A: Using CLI (Recommended - Planned Interface)

**Java CLI:**

```bash
cd lib/java
mvn clean install

# Run OpenToken with receiver's public key
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  --receiver-public-key ~/opentoken_exchange/receiver_public_key.pem \
  -i ../../resources/sample.csv \
  -t csv \
  -o ../../resources/output.zip

# Output:
# ✓ Loaded receiver's public key
# ✓ Generated sender key pair (saved to ~/.opentoken/)
# ✓ Performed ECDH key exchange
# ✓ Derived hashing key (32 bytes)
# ✓ Derived encryption key (32 bytes)
# ✓ Processed 105 records, 93 valid tokens generated
# ✓ Output package created: output.zip
#   ├─ tokens.csv (encrypted)
#   ├─ tokens.metadata.json
#   └─ sender_public_key.pem
```

**Python CLI:**

```bash
cd lib/python/opentoken-cli
pip install -e . -e ../opentoken

# Run OpenToken with receiver's public key
python -m opentoken_cli.main \
  --receiver-public-key ~/opentoken_exchange/receiver_public_key.pem \
  -i ../../../resources/sample.csv \
  -t csv \
  -o ../../../resources/output.zip

# Output:
# ✓ Loaded receiver's public key
# ✓ Generated sender key pair (saved to ~/.opentoken/)
# ✓ Performed ECDH key exchange
# ✓ Derived hashing key (32 bytes)
# ✓ Derived encryption key (32 bytes)
# ✓ Processed 105 records, 93 valid tokens generated
# ✓ Output package created: output.zip
#   ├─ tokens.csv (encrypted)
#   ├─ tokens.metadata.json
#   └─ sender_public_key.pem
```

**Alternative: Specify Custom Key Location**

```bash
# Use a specific sender key pair location
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  --receiver-public-key ~/opentoken_exchange/receiver_public_key.pem \
  --sender-keypair-path ~/my_keys/keypair.pem \
  -i ../../resources/sample.csv \
  -t csv \
  -o ../../resources/output.zip
```

**Note:** CLI commands above show the planned interface. Current implementation status: **In Progress (Phase 3-4)**. The programmatic API (Option B below) is currently available.

#### Option B: Using Programmatic API (Currently Available)

**Using Java:**

```java
import com.truveta.opentoken.keyexchange.*;
import com.truveta.opentoken.tokentransformer.*;
import java.security.KeyPair;
import java.security.PublicKey;

// 1. Load or generate sender's key pair
KeyPairManager senderKeyManager = new KeyPairManager("./sender_keys");
KeyPair senderKeyPair = senderKeyManager.getOrCreateKeyPair();

// 2. Load receiver's public key
PublicKeyLoader publicKeyLoader = new PublicKeyLoader();
PublicKey receiverPublicKey = publicKeyLoader.loadPublicKey(
    "~/opentoken_exchange/receiver_public_key.pem"
);

// 3. Perform ECDH key exchange and derive keys
KeyExchange keyExchange = new KeyExchange();
KeyExchange.DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
    senderKeyPair.getPrivate(),
    receiverPublicKey
);

// 4. Use derived keys for token transformers
HashTokenTransformer hashTransformer = new HashTokenTransformer(
    keys.getHashingKeyAsString()
);
EncryptTokenTransformer encryptTransformer = new EncryptTokenTransformer(
    keys.getEncryptionKeyAsString()
);

// 5. Process tokens (existing OpenToken pipeline)
// ... token generation code ...

// 6. Save sender's public key for the receiver
senderKeyManager.savePublicKey(
    senderKeyPair.getPublic(),
    "./output/sender_public_key.pem"
);

System.out.println("✓ Tokens generated with ECDH-derived keys");
System.out.println("✓ Sender public key saved to: ./output/sender_public_key.pem");
```

**Using Python:**

```python
from opentoken.keyexchange import KeyPairManager, PublicKeyLoader, KeyExchange
from opentoken.tokentransformer import HashTokenTransformer, EncryptTokenTransformer

# 1. Load or generate sender's key pair
sender_key_manager = KeyPairManager("./sender_keys")
sender_private_key, sender_public_key = sender_key_manager.get_or_create_key_pair()

# 2. Load receiver's public key
public_key_loader = PublicKeyLoader()
receiver_public_key = public_key_loader.load_public_key(
    "~/opentoken_exchange/receiver_public_key.pem"
)

# 3. Perform ECDH key exchange and derive keys
key_exchange = KeyExchange()
keys = key_exchange.exchange_and_derive_keys(
    sender_private_key,
    receiver_public_key
)

# 4. Use derived keys for token transformers
hash_transformer = HashTokenTransformer(keys.get_hashing_key_as_string())
encrypt_transformer = EncryptTokenTransformer(keys.get_encryption_key_as_string())

# 5. Process tokens (existing OpenToken pipeline)
# ... token generation code ...

# 6. Save sender's public key for the receiver
sender_key_manager.save_public_key(
    sender_public_key,
    "./output/sender_public_key.pem"
)

print("✓ Tokens generated with ECDH-derived keys")
print("✓ Sender public key saved to: ./output/sender_public_key.pem")
```

**Output Structure:**

```
output.zip
├── tokens.csv                  # Encrypted tokens
├── tokens.metadata.json        # Metadata with key exchange info
└── sender_public_key.pem       # Sender's public key for receiver
```

---

### Step 5: Sender Sends Output to Receiver

The sender packages and sends the output to the receiver.

**Create a package:**

```bash
# If not already in ZIP format, create one
cd output
zip -r tokens_output.zip tokens.csv tokens.metadata.json sender_public_key.pem

# Send to receiver (example methods)
# - Secure file transfer
# - Cloud storage with access controls
# - Encrypted email attachment
```

**What's included:**

1. **tokens.csv** (or tokens.parquet): Encrypted tokens
2. **tokens.metadata.json**: Metadata including:
   - Key exchange method (ECDH-P256)
   - SHA-256 hash of sender's public key
   - SHA-256 hash of receiver's public key
   - Processing statistics
3. **sender_public_key.pem**: Sender's public key (needed for decryption)

---

### Step 6: Receiver Decrypts Tokens

The receiver uses their private key and the sender's public key (from the ZIP package) to derive the same keys and decrypt tokens.

**Extract the package:**

```bash
unzip tokens_output.zip -d received_tokens/
cd received_tokens/

# Verify contents
ls -la
# Expected: tokens.csv, tokens.metadata.json, sender_public_key.pem
```

#### Option A: Using CLI (Recommended - Planned Interface)

The CLI automatically extracts the sender's public key from the ZIP, performs ECDH, and decrypts the tokens.

**Java CLI:**

```bash
cd lib/java

# Decrypt tokens using the ZIP package
# The CLI extracts sender's public key from the ZIP automatically
java -jar opentoken-cli/target/opentoken-cli-*.jar --decrypt-with-ecdh \
  -i ../../received_tokens/tokens_output.zip \
  -o ../../received_tokens/decrypted_tokens.csv

# Output:
# ✓ Extracted sender's public key from package
# ✓ Loaded receiver's private key from ~/.opentoken/
# ✓ Performed ECDH key exchange
# ✓ Derived encryption key (matches sender's key)
# ✓ Decrypted 93 tokens successfully
# ✓ Output written to: decrypted_tokens.csv
```

**Python CLI:**

```bash
cd lib/python/opentoken-cli

# Decrypt tokens using the ZIP package
python -m opentoken_cli.main --decrypt-with-ecdh \
  -i ../../../received_tokens/tokens_output.zip \
  -o ../../../received_tokens/decrypted_tokens.csv

# Output:
# ✓ Extracted sender's public key from package
# ✓ Loaded receiver's private key from ~/.opentoken/
# ✓ Performed ECDH key exchange
# ✓ Derived encryption key (matches sender's key)
# ✓ Decrypted 93 tokens successfully
# ✓ Output written to: decrypted_tokens.csv
```

**Alternative: Specify Keys Explicitly**

```bash
# Specify custom receiver keypair location
java -jar opentoken-cli/target/opentoken-cli-*.jar --decrypt-with-ecdh \
  --receiver-keypair-path ~/my_keys/keypair.pem \
  --sender-public-key ./received_tokens/sender_public_key.pem \
  -i ./received_tokens/tokens.csv \
  -o ./decrypted_tokens.csv
```

**Note:** CLI commands above show the planned interface. Current implementation status: **In Progress (Phase 5)**. The programmatic API (Option B below) is currently available.

#### Option B: Using Programmatic API (Currently Available)

**Decrypt using Java:**

```java
import com.truveta.opentoken.keyexchange.*;
import com.truveta.opentoken.tokentransformer.*;

// 1. Load receiver's private key
KeyPairManager receiverKeyManager = new KeyPairManager();
KeyPair receiverKeyPair = receiverKeyManager.loadKeyPair(
    System.getProperty("user.home") + "/.opentoken/keypair.pem"
);

// 2. Load sender's public key (from the received package)
PublicKeyLoader publicKeyLoader = new PublicKeyLoader();
PublicKey senderPublicKey = publicKeyLoader.loadPublicKey(
    "./received_tokens/sender_public_key.pem"
);

// 3. Perform ECDH key exchange (same as sender)
KeyExchange keyExchange = new KeyExchange();
KeyExchange.DerivedKeys keys = keyExchange.exchangeAndDeriveKeys(
    receiverKeyPair.getPrivate(),
    senderPublicKey
);

// 4. Verify: These are the SAME keys the sender used
String hashingKey = keys.getHashingKeyAsString();
String encryptionKey = keys.getEncryptionKeyAsString();

// 5. Decrypt tokens
DecryptTokenTransformer decryptTransformer = new DecryptTokenTransformer(
    encryptionKey
);

// Process decrypted tokens...
System.out.println("✓ Tokens decrypted successfully");
System.out.println("✓ Derived hashing key matches sender's key");
```

**Decrypt using Python:**

```python
from opentoken.keyexchange import KeyPairManager, PublicKeyLoader, KeyExchange
from opentoken.tokentransformer import DecryptTokenTransformer
import os

# 1. Load receiver's private key
receiver_key_manager = KeyPairManager()
receiver_private_key, _ = receiver_key_manager.load_key_pair(
    os.path.expanduser("~/.opentoken/keypair.pem")
)

# 2. Load sender's public key (from the received package)
public_key_loader = PublicKeyLoader()
sender_public_key = public_key_loader.load_public_key(
    "./received_tokens/sender_public_key.pem"
)

# 3. Perform ECDH key exchange (same as sender)
key_exchange = KeyExchange()
keys = key_exchange.exchange_and_derive_keys(
    receiver_private_key,
    sender_public_key
)

# 4. Verify: These are the SAME keys the sender used
hashing_key = keys.get_hashing_key_as_string()
encryption_key = keys.get_encryption_key_as_string()

# 5. Decrypt tokens
decrypt_transformer = DecryptTokenTransformer(encryption_key)

# Process decrypted tokens...
print("✓ Tokens decrypted successfully")
print("✓ Derived hashing key matches sender's key")
```

---

### Step 7: Verify Key Derivation

Both parties should derive **identical** keys. Here's how to verify:

**Create a verification script:**

```python
#!/usr/bin/env python3
"""
Verify that sender and receiver derive identical keys from ECDH.
"""
from opentoken.keyexchange import KeyPairManager, KeyExchange
import hashlib

# Simulate sender
sender_km = KeyPairManager("./sender_keys")
sender_priv, sender_pub = sender_km.generate_key_pair()

# Simulate receiver  
receiver_km = KeyPairManager("./receiver_keys")
receiver_priv, receiver_pub = receiver_km.generate_key_pair()

key_exchange = KeyExchange()

# Sender derives keys
sender_keys = key_exchange.exchange_and_derive_keys(sender_priv, receiver_pub)
sender_hash_key = sender_keys.get_hashing_key()
sender_enc_key = sender_keys.get_encryption_key()

# Receiver derives keys
receiver_keys = key_exchange.exchange_and_derive_keys(receiver_priv, sender_pub)
receiver_hash_key = receiver_keys.get_hashing_key()
receiver_enc_key = receiver_keys.get_encryption_key()

# Verify they match
print("Key Derivation Verification:")
print(f"  Hashing keys match:    {sender_hash_key == receiver_hash_key} ✓")
print(f"  Encryption keys match: {sender_enc_key == receiver_enc_key} ✓")

# Show key fingerprints (for debugging)
print("\nKey Fingerprints (SHA-256):")
print(f"  Sender hashing key:   {hashlib.sha256(sender_hash_key).hexdigest()[:16]}...")
print(f"  Receiver hashing key: {hashlib.sha256(receiver_hash_key).hexdigest()[:16]}...")
print(f"  Sender encrypt key:   {hashlib.sha256(sender_enc_key).hexdigest()[:16]}...")
print(f"  Receiver encrypt key: {hashlib.sha256(receiver_enc_key).hexdigest()[:16]}...")

if sender_hash_key == receiver_hash_key and sender_enc_key == receiver_enc_key:
    print("\n✓ SUCCESS: Both parties derived identical keys!")
else:
    print("\n✗ ERROR: Key derivation mismatch!")
```

**Run the verification:**

```bash
python3 verify_key_derivation.py
```

**Expected output:**

```
Key Derivation Verification:
  Hashing keys match:    True ✓
  Encryption keys match: True ✓

Key Fingerprints (SHA-256):
  Sender hashing key:   a3f5c8e2d1b6f9a7...
  Receiver hashing key: a3f5c8e2d1b6f9a7...
  Sender encrypt key:   7b4e9f1c8d3a5e2f...
  Receiver encrypt key: 7b4e9f1c8d3a5e2f...

✓ SUCCESS: Both parties derived identical keys!
```

---

## Key Concepts

### ECDH Key Exchange

**Elliptic Curve Diffie-Hellman** allows two parties to establish a shared secret over an insecure channel:

1. Each party generates a key pair (private + public)
2. Parties exchange public keys
3. Each party combines their private key with the other's public key
4. Both parties arrive at the **same shared secret**
5. Shared secret is used to derive encryption keys via HKDF

**Security:** An eavesdropper with both public keys **cannot** derive the shared secret (computational difficulty of the elliptic curve discrete logarithm problem).

### HKDF (Key Derivation)

**HMAC-based Key Derivation Function (RFC 5869)** derives multiple keys from a single shared secret:

```
Shared Secret (from ECDH)
    ↓
HKDF-Extract (with salt="HASHING")
    ↓
HKDF-Expand (with info="OpenToken-Hash")
    ↓
Hashing Key (32 bytes)

Shared Secret (from ECDH)
    ↓
HKDF-Extract (with salt="ENCRYPTION")
    ↓
HKDF-Expand (with info="OpenToken-Encrypt")
    ↓
Encryption Key (32 bytes)
```

**Why separate keys?** Using different salts ensures the hashing key and encryption key are cryptographically independent, preventing key reuse attacks.

### P-256 Curve

**secp256r1 (P-256)** provides:
- 128-bit security level (equivalent to AES-256)
- NIST FIPS 186-4 standardization
- Broad support in Java and Python crypto libraries
- Fast computation on modern hardware

---

## Security Considerations

### Private Key Protection

**DO:**
- ✅ Store private keys with restrictive permissions (0600 on Unix)
- ✅ Keep private keys encrypted at rest (future enhancement)
- ✅ Use secure key storage (HSM, TPM) for production environments
- ✅ Rotate keys periodically
- ✅ Delete old private keys after rotation

**DON'T:**
- ❌ Share private keys with anyone
- ❌ Transmit private keys over insecure channels
- ❌ Commit private keys to version control
- ❌ Store private keys in application logs
- ❌ Use the same key pair for multiple purposes

### Public Key Distribution

**DO:**
- ✅ Verify public key fingerprints via out-of-band channel (phone, SMS)
- ✅ Use authenticated channels (TLS, signed emails)
- ✅ Store public keys in a trusted directory service
- ✅ Include public key hashes in metadata for verification

**DON'T:**
- ❌ Assume public keys received are authentic without verification
- ❌ Use public keys from untrusted sources

### Key Lifecycle

1. **Generation:** Use cryptographically secure random number generator
2. **Storage:** Protect private keys with encryption and access controls
3. **Distribution:** Share only public keys, verify authenticity
4. **Usage:** Limit key usage to authorized operations
5. **Rotation:** Replace keys periodically (recommended: annually)
6. **Revocation:** Securely destroy compromised keys
7. **Archival:** Retain public keys for audit trails

---

## Troubleshooting

### Issue: "Public key file not found"

**Cause:** The receiver's public key was not saved to the expected location.

**Solution:**
```bash
# Verify the file exists
ls -la ~/opentoken_exchange/receiver_public_key.pem

# Check the file format
head -1 ~/opentoken_exchange/receiver_public_key.pem
# Should show: -----BEGIN PUBLIC KEY-----
```

### Issue: "Invalid key type: expected EC public key"

**Cause:** The public key is not an elliptic curve key or uses the wrong curve.

**Solution:**
- Regenerate the key pair using `KeyPairManager` (uses P-256 by default)
- Verify the key type:
```bash
openssl ec -pubin -in public_key.pem -text -noout
# Should show: ASN1 OID: prime256v1
```

### Issue: "Key derivation mismatch"

**Cause:** Sender and receiver are not using each other's correct public keys.

**Solution:**
- Verify public key fingerprints:
```python
import hashlib
with open("sender_public_key.pem", "rb") as f:
    print(hashlib.sha256(f.read()).hexdigest())
```
- Ensure sender used receiver's public key (not their own)
- Ensure receiver used sender's public key (from the package)

### Issue: "Decryption failed: bad padding"

**Cause:** Wrong encryption key used for decryption.

**Solution:**
- Verify ECDH key exchange is using correct key pairs
- Check that sender's public key in the package is correct
- Ensure receiver is using their original private key (not regenerated)

### Issue: "Permission denied when reading private key"

**Cause:** File permissions too restrictive or not set correctly.

**Solution:**
```bash
# Set correct permissions
chmod 600 ~/.opentoken/keypair.pem
chmod 644 ~/.opentoken/public_key.pem

# Verify ownership
ls -la ~/.opentoken/
```

---

## Next Steps

For production deployment:

1. **Implement CLI integration** (Phases 3-4 of the implementation plan)
2. **Add automated key rotation** capabilities
3. **Integrate with HSM/key management services**
4. **Set up key escrow** for disaster recovery
5. **Implement audit logging** for key operations
6. **Create key management policies** and procedures

For more information:
- [Implementation Plan](./public-key-implementation-plan.md)
- [OpenToken README](../README.md)
- [Security Best Practices](https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines)

---

---

## Demo Script

A complete working demo script is available at `tools/demo_public_key_workflow.py`. This script demonstrates the entire workflow programmatically.

**To run the demo:**

```bash
# Ensure opentoken library is installed
cd lib/python/opentoken
pip install -e .

# Run the demo
cd ../../../
python3 tools/demo_public_key_workflow.py
```

The demo will:
1. Generate receiver's key pair
2. Simulate sharing the public key
3. Generate sender's key pair
4. Perform ECDH key exchange (both sides)
5. Verify that both parties derived identical keys
6. Clean up temporary files

**Expected output:**
```
======================================================================
  OpenToken Public Key Exchange Workflow Demo
======================================================================
This demo simulates the complete workflow between sender and receiver
using ECDH key exchange as described in docs/public-key-workflow.md

[Step 1] Receiver Generates Key Pair
----------------------------------------------------------------------
  Generating P-256 ECDH key pair for receiver...
✓ Receiver key pair generated
  Private key: /tmp/opentoken_demo_xxx/receiver_keys/keypair.pem (0600 permissions)
  Public key:  /tmp/opentoken_demo_xxx/receiver_keys/public_key.pem
  Public key fingerprint: a3f5c8e2d1b6f9a7...

...

======================================================================
  SUCCESS: Key Exchange Verified!
======================================================================

✓ Both parties derived IDENTICAL keys from ECDH
✓ Sender can encrypt tokens, receiver can decrypt them
✓ No shared secrets were transmitted!
```

---

**Last Updated:** December 18, 2024  
**Version:** 1.13.0 (Draft)
