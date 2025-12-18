# Public-Key Cryptography Implementation Plan

## Executive Summary

This document provides a comprehensive technical analysis and implementation plan for transitioning OpenToken from secret-based encryption to public-key cryptography with Elliptic Curve Diffie-Hellman (ECDH) key exchange. This enhancement will modernize the security architecture while maintaining backward compatibility and cross-language parity between Java and Python implementations.

**Target Version:** 1.13.0  
**Estimated Effort:** 6-8 weeks  
**Risk Level:** Medium

---

## Table of Contents

- [Current State Analysis](#current-state-analysis)
  - [Token Generation Process](#token-generation-process)
  - [Current Encryption Flow](#current-encryption-flow)
  - [Output Structure](#output-structure)
- [Requirements from Issue](#requirements-from-issue)
- [Proposed Solution Architecture](#proposed-solution-architecture)
  - [Cryptographic Approach](#cryptographic-approach)
  - [Why ECDH Over Traditional DH](#why-ecdh-over-traditional-dh)
- [Implementation Components](#implementation-components)
  - [Key Management Module](#1-key-management-module)
  - [Modified Token Transformers](#2-modified-token-transformers)
  - [Output Packaging](#3-output-packaging)
  - [CLI Argument Changes](#4-cli-argument-changes)
- [Implementation Phases](#implementation-phases)
- [Security Considerations](#security-considerations)
- [File Format Specifications](#file-format-specifications)
- [Testing Strategy](#testing-strategy)
- [Documentation Updates](#documentation-updates)
- [Dependencies](#dependencies)
- [Risk Assessment](#risk-assessment)
- [Rollout Plan](#rollout-plan)
- [Architecture Diagrams](#architecture-diagrams)

---

## Current State Analysis

### Token Generation Process

The current implementation uses a symmetric encryption approach:

1. **HMAC-SHA256 Hashing:** Tokens are hashed using HMAC-SHA256 with a shared secret (`HashTokenTransformer`)
2. **AES-256-GCM Encryption:** Hashed tokens are encrypted using AES-256-GCM with a shared secret (`EncryptTokenTransformer`)
3. **Dual Implementation:** Both Java and Python implementations must produce identical outputs
4. **Secret Distribution:** Secrets are passed as command-line arguments (`--hashing-secret`, `--encryption-key`)

**Key Files:**
- Java: `lib/java/opentoken/src/main/java/com/truveta/opentoken/tokentransformer/`
- Python: `lib/python/opentoken/src/main/opentoken/tokentransformer/`

### Current Encryption Flow

```
Person Attributes
    ↓
Normalize & Validate
    ↓
Generate Token Signatures
    ↓
HMAC-SHA256(hashing_secret)
    ↓
Base64 Encode
    ↓
AES-256-GCM(encryption_key)
    ↓
Base64 Encode
    ↓
Output to CSV/Parquet
```

### Output Structure

Current outputs include:

- **Token file:** CSV or Parquet format with encrypted tokens
- **Metadata file:** `.metadata.json` containing:
  - Processing statistics
  - SHA-256 hashes of secrets (for audit)
  - System information
  - No ZIP packaging currently

**Limitations:**
- Requires secure out-of-band secret sharing
- No forward secrecy
- Manual key distribution burden

---

## Requirements from Issue

The transition to public-key cryptography addresses the following requirements:

1. **Switch from secrets to public keys:** Accept receiver's public key as input instead of shared secrets
2. **Auto-generate sender key pair:** Generate ephemeral key pair if no local key pair exists
3. **Use Diffie-Hellman for hashing key:** Derive a common hash key using ECDH that both sender and receiver can compute independently
4. **Keep encryption:** Continue using AES encryption with derived keys for token protection
5. **Output as ZIP:** Package CSV/Parquet, metadata.json, and sender's public key into a single ZIP archive

---

## Proposed Solution Architecture

### Cryptographic Approach

**Elliptic Curve Diffie-Hellman (ECDH) for Key Agreement**

The proposed solution uses ECDH with the P-256 curve (secp256r1) to establish shared secrets:

1. **Key Generation:**
   - Sender generates or loads an ECDH key pair
   - Sender receives receiver's public key

2. **Key Exchange:**
   - Both parties compute: `shared_secret = ECDH(sender_private, receiver_public)`
   - The same shared secret is derived by both parties without transmission

3. **Key Derivation:**
   - Use HKDF-SHA256 (RFC 5869) to derive two separate keys from the shared secret:
     - `hashing_key = HKDF(shared_secret, salt="HASHING", info="OpenToken-Hash")`
     - `encryption_key = HKDF(shared_secret, salt="ENCRYPTION", info="OpenToken-Encrypt")`

4. **Token Processing:**
   - Use derived keys for HMAC-SHA256 and AES-256-GCM (same algorithms as before)

**Security Properties:**
- **Forward Secrecy:** Ephemeral key pairs provide forward secrecy if regenerated per session
- **Mutual Derivation:** Both sender and receiver derive identical keys without key transmission
- **Key Separation:** HKDF ensures hashing and encryption keys are cryptographically independent
- **Algorithm Agility:** Metadata tracks key exchange method for future upgrades

### Why ECDH Over Traditional DH

| Feature | ECDH (P-256) | Traditional DH (3072-bit) |
|---------|--------------|---------------------------|
| **Security Level** | ~128 bits | ~128 bits |
| **Key Size** | 256 bits | 3072 bits |
| **Performance** | Fast | Slower |
| **Standard Support** | NIST FIPS 186-4, SEC2 | NIST FIPS 186-4 |
| **Java Support** | `java.security` (built-in) | `java.security` (built-in) |
| **Python Support** | `cryptography` library | `cryptography` library |
| **Maturity** | Widely deployed (TLS 1.3) | Mature but larger |

**Conclusion:** ECDH provides equivalent security with better performance and smaller key sizes, making it the preferred choice for modern cryptographic applications.

---

## Implementation Components

### 1. Key Management Module

**New Package:** `com.truveta.opentoken.keyexchange` (Java), `opentoken.keyexchange` (Python)

#### KeyPairManager

Manages the lifecycle of sender's ECDH key pairs.

**Responsibilities:**
- Load existing key pair from file (PEM format)
- Generate new ECDH key pair (P-256 curve) if none exists
- Save key pair to secure location with appropriate permissions
- Export public key for distribution

**Default Key Locations:**
- Private key: `~/.opentoken/keypair.pem` (permissions: 0600)
- Public key: `~/.opentoken/public_key.pem` (permissions: 0644)

**Methods:**
```java
// Java
public class KeyPairManager {
    public KeyPair loadKeyPair(Path keyPath) throws IOException
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException
    public void saveKeyPair(KeyPair keyPair, Path keyPath) throws IOException
    public PublicKey exportPublicKey(KeyPair keyPair)
    public void savePublicKey(PublicKey publicKey, Path publicKeyPath) throws IOException
}
```

```python
# Python
class KeyPairManager:
    def load_key_pair(self, key_path: Path) -> ec.EllipticCurvePrivateKey
    def generate_key_pair(self) -> ec.EllipticCurvePrivateKey
    def save_key_pair(self, private_key: ec.EllipticCurvePrivateKey, key_path: Path) -> None
    def export_public_key(self, private_key: ec.EllipticCurvePrivateKey) -> ec.EllipticCurvePublicKey
    def save_public_key(self, public_key: ec.EllipticCurvePublicKey, public_key_path: Path) -> None
```

#### KeyExchange

Performs ECDH key agreement and derives cryptographic keys.

**Responsibilities:**
- Perform ECDH agreement between sender's private key and receiver's public key
- Derive hashing key using HKDF-SHA256
- Derive encryption key using HKDF-SHA256
- Ensure consistent key derivation across Java and Python

**Methods:**
```java
// Java
public class KeyExchange {
    public byte[] performKeyExchange(PrivateKey senderPrivate, PublicKey receiverPublic) 
        throws GeneralSecurityException
    
    public byte[] deriveHashingKey(byte[] sharedSecret) 
        throws NoSuchAlgorithmException, InvalidKeyException
    
    public byte[] deriveEncryptionKey(byte[] sharedSecret) 
        throws NoSuchAlgorithmException, InvalidKeyException
}
```

```python
# Python
class KeyExchange:
    def perform_key_exchange(
        self, 
        sender_private: ec.EllipticCurvePrivateKey, 
        receiver_public: ec.EllipticCurvePublicKey
    ) -> bytes
    
    def derive_hashing_key(self, shared_secret: bytes) -> bytes
    
    def derive_encryption_key(self, shared_secret: bytes) -> bytes
```

**HKDF Parameters:**
- **Algorithm:** HKDF-SHA256 (RFC 5869)
- **Hashing Key Derivation:**
  - Salt: `b"HASHING"`
  - Info: `b"OpenToken-Hash"`
  - Output length: 32 bytes
- **Encryption Key Derivation:**
  - Salt: `b"ENCRYPTION"`
  - Info: `b"OpenToken-Encrypt"`
  - Output length: 32 bytes

#### PublicKeyLoader

Loads and validates receiver's public key.

**Responsibilities:**
- Load public keys from PEM format
- Load public keys from DER format
- Validate key format and curve (must be P-256)
- Reject malformed or unsupported keys

**Methods:**
```java
// Java
public class PublicKeyLoader {
    public PublicKey loadFromPEM(Path keyPath) throws IOException, GeneralSecurityException
    public PublicKey loadFromDER(Path keyPath) throws IOException, GeneralSecurityException
    public void validateKey(PublicKey publicKey) throws InvalidKeyException
}
```

```python
# Python
class PublicKeyLoader:
    def load_from_pem(self, key_path: Path) -> ec.EllipticCurvePublicKey
    def load_from_der(self, key_path: Path) -> ec.EllipticCurvePublicKey
    def validate_key(self, public_key: ec.EllipticCurvePublicKey) -> None
```

---

### 2. Modified Token Transformers

**No changes to core hashing/encryption logic** are required. The token transformers continue using HMAC-SHA256 and AES-256-GCM, but receive derived keys instead of user-provided secrets.

**HashTokenTransformer:**
- Accepts 32-byte key derived from ECDH + HKDF
- HMAC-SHA256 logic remains unchanged

**EncryptTokenTransformer:**
- Accepts 32-byte key derived from ECDH + HKDF
- AES-256-GCM logic remains unchanged

**Integration Point:**
- CLI argument parser derives keys via `KeyExchange` before instantiating transformers
- Transformers remain agnostic to key source (secret vs. derived)

---

### 3. Output Packaging

**New Class:** `OutputPackager`

Creates ZIP archives containing all token generation outputs.

**Responsibilities:**
- Create ZIP archive with standard structure
- Include token output file (CSV or Parquet)
- Include enhanced metadata JSON
- Include sender's public key (PEM format)
- Maintain file permissions and timestamps

**ZIP Archive Structure:**
```
opentoken_output.zip
├── tokens.csv              (or tokens.parquet)
├── tokens.metadata.json    (enhanced with key exchange metadata)
└── sender_public_key.pem   (sender's public key for receiver)
```

**Methods:**
```java
// Java
public class OutputPackager {
    public void createZipArchive(
        Path tokenFile,
        Path metadataFile,
        Path senderPublicKeyFile,
        Path outputZipPath
    ) throws IOException
}
```

```python
# Python
class OutputPackager:
    def create_zip_archive(
        self,
        token_file: Path,
        metadata_file: Path,
        sender_public_key_file: Path,
        output_zip_path: Path
    ) -> None
```

---

### 4. CLI Argument Changes

#### New Arguments

**Token Generation Mode:**
- `--receiver-public-key <path>`
  - **Required** for public-key mode
  - Path to receiver's public key file (PEM or DER format)
  - Validates key format and curve

- `--sender-keypair-path <path>`
  - **Optional**
  - Path to sender's private key (PEM format)
  - If omitted, generates ephemeral key pair at default location
  - Default: `~/.opentoken/keypair.pem`

- `--output-public-key <path>`
  - **Optional**
  - Where to save sender's public key separately (in addition to ZIP)
  - Useful for distributing sender's public key independently

**Decryption Mode:**
- `--sender-public-key <path>`
  - **Required** for decryption
  - Sender's public key (extracted from ZIP or provided separately)

- `--receiver-keypair-path <path>`
  - **Required** for decryption
  - Receiver's private key (PEM format)
  - Used to derive decryption keys

#### Deprecated Arguments (Backward Compatibility)

- `--hashing-secret`
  - Still functional but triggers deprecation warning
  - Used for legacy secret-based mode
  - Warning: "Secret-based mode is deprecated. Consider migrating to public-key mode."

- `--encryption-key`
  - Still functional but triggers deprecation warning
  - Used for legacy secret-based mode
  - Warning: "Secret-based mode is deprecated. Consider migrating to public-key mode."

#### Mode Detection Logic

```
if --receiver-public-key provided:
    → Public-Key Mode (new)
    → Require: receiver-public-key
    → Optional: sender-keypair-path, output-public-key
    → Output: ZIP archive

else if --hashing-secret provided:
    → Legacy Secret Mode (deprecated)
    → Require: hashing-secret, encryption-key
    → Output: CSV/Parquet + metadata.json
    → Display deprecation warning

else:
    → Error: Must provide either --receiver-public-key or --hashing-secret
```

---

## Implementation Phases

### Phase 1: Core Cryptographic Infrastructure (Java)

**Duration:** 1.5 weeks  
**Owner:** Java team

**Tasks:**
1. Create `com.truveta.opentoken.keyexchange` package
2. Implement `KeyPairManager`:
   - Key generation using `KeyPairGenerator` with P-256 curve
   - PEM encoding/decoding using Bouncy Castle or built-in APIs
   - File I/O with permission management
3. Implement `KeyExchange`:
   - ECDH using `KeyAgreement` with "ECDH" algorithm
   - HKDF implementation (RFC 5869) using `Mac` and `MessageDigest`
   - Derive separate hashing and encryption keys
4. Implement `PublicKeyLoader`:
   - PEM/DER parsing
   - Key validation (curve check, format check)
5. Add comprehensive unit tests:
   - Test with known test vectors (RFC 5869 HKDF test cases)
   - Key generation determinism
   - Cross-validation with Python implementation

**Deliverables:**
- Functional key management module
- Unit tests with ≥90% coverage
- Documentation for key management APIs

---

### Phase 2: Core Cryptographic Infrastructure (Python)

**Duration:** 1.5 weeks  
**Owner:** Python team

**Tasks:**
1. Create `opentoken.keyexchange` module
2. Implement `KeyPairManager`:
   - Key generation using `cryptography.hazmat.primitives.asymmetric.ec`
   - PEM serialization using `cryptography` serialization APIs
   - File I/O with `os.chmod` for permissions
3. Implement `KeyExchange`:
   - ECDH using `ec.EllipticCurvePrivateKey.exchange()`
   - HKDF using `cryptography.hazmat.primitives.kdf.hkdf.HKDF`
   - Ensure byte-identical outputs with Java
4. Implement `PublicKeyLoader`:
   - PEM/DER parsing using `cryptography` serialization
   - Key validation (curve check)
5. Add comprehensive unit tests:
   - Test against same RFC 5869 vectors as Java
   - Interoperability tests with Java implementation
   - Validate byte-identical derived keys

**Deliverables:**
- Functional key management module matching Java behavior
- Unit tests with ≥90% coverage
- Interoperability validation report

---

### Phase 3: Integration with Token Generation (Java)

**Duration:** 2 weeks  
**Owner:** Java team

**Tasks:**
1. Modify `Main.java`:
   - Add new CLI argument parsing for public-key mode
   - Implement mode detection logic (public-key vs. legacy)
   - Integrate `KeyPairManager` and `KeyExchange`
   - Add deprecation warnings for secret-based mode
2. Create `OutputPackager`:
   - ZIP file creation using `java.util.zip`
   - Include token file, metadata, and sender public key
   - Preserve file metadata (timestamps, permissions)
3. Update `Metadata` class:
   - Add `KeyExchangeMethod` field
   - Add `SenderPublicKeyHash` field
   - Add `ReceiverPublicKeyHash` field
   - Add `KeyDerivationFunction` field
   - Maintain backward compatibility with legacy format
4. Integration testing:
   - End-to-end token generation with public keys
   - ZIP extraction and validation
   - Legacy mode still functional
5. Update sanity checks (Maven Antrun):
   - Add public-key mode test cases
   - Validate ZIP structure and contents

**Deliverables:**
- Fully integrated public-key mode in CLI
- ZIP output packaging
- Enhanced metadata format
- Integration test suite

---

### Phase 4: Integration with Token Generation (Python)

**Duration:** 2 weeks  
**Owner:** Python team

**Tasks:**
1. Modify `main.py`:
   - Mirror Java CLI argument changes
   - Implement identical mode detection logic
   - Integrate key management and exchange modules
   - Add deprecation warnings
2. Create `OutputPackager`:
   - ZIP file creation using `zipfile`
   - Ensure identical structure to Java output
3. Update `Metadata` class:
   - Add same fields as Java implementation
   - Ensure JSON output matches Java format
4. Integration testing:
   - End-to-end token generation matching Java
   - Cross-validation: Java sender → Python receiver
   - Cross-validation: Python sender → Java receiver
5. Update pytest test suite:
   - Add public-key mode test cases
   - Validate interoperability with Java

**Deliverables:**
- Fully integrated public-key mode in Python CLI
- ZIP output packaging matching Java
- Enhanced metadata format
- Cross-language validation passing

---

### Phase 5: Backward Compatibility & Decryption

**Duration:** 1 week  
**Owner:** Both teams

**Tasks:**
1. Implement graceful deprecation:
   - Display clear warning messages
   - Provide migration documentation link
   - Ensure legacy mode remains fully functional
2. Implement decryption mode:
   - Add `--decrypt` flag to CLI
   - Accept sender's public key and receiver's private key
   - Derive same keys and decrypt tokens
   - Support both ZIP input and loose files
3. Add mode detection tests:
   - Test all argument combinations
   - Validate error messages for invalid combinations
   - Test backward compatibility thoroughly
4. Documentation:
   - Update CLI help text
   - Create migration guide
   - Document decryption workflow

**Deliverables:**
- Backward-compatible secret mode with warnings
- Functional decryption mode
- Migration documentation

---

### Phase 6: Cross-Language Validation

**Duration:** 1 week  
**Owner:** Both teams + QA

**Tasks:**
1. Run comprehensive interoperability tests:
   - Java sender → Python receiver (100 test cases)
   - Python sender → Java receiver (100 test cases)
   - Validate byte-identical derived keys
   - Validate identical ZIP outputs
2. Performance testing:
   - Measure key generation overhead
   - Measure ECDH computation time
   - Compare secret-based vs. public-key performance
3. Security review:
   - Key storage permissions validation
   - Audit key derivation implementation
   - Review error handling for security issues
4. Documentation review:
   - Validate all examples work
   - Check for completeness
   - User acceptance testing
5. Prepare release notes

**Deliverables:**
- Interoperability validation report
- Performance benchmark report
- Security audit report
- Complete documentation
- Release candidate

---

## Security Considerations

### Key Storage

**Private Key Security:**
- Store private keys with file permissions `0600` (owner read/write only)
- Default location: `~/.opentoken/keypair.pem`
- Warn users if permissions are too permissive
- Future enhancement: Support encrypted private keys with passphrase

**Public Key Distribution:**
- Public keys can be freely shared (stored with `0644` permissions)
- Include in ZIP output for receiver
- Can be distributed via insecure channels

**Key Management Best Practices:**
- Rotate key pairs periodically (e.g., annually)
- Generate ephemeral keys per data exchange for forward secrecy
- Document key lifecycle in user guide
- Provide tools for key rotation

### Curve Selection

**Chosen Curve: P-256 (secp256r1)**

**Rationale:**
- NIST FIPS 186-4 approved
- Widely standardized (SEC 2, ANSI X9.62)
- Supported by major browsers and libraries
- ~128-bit security level
- Resistant to known attacks (Pohlig-Hellman, Pollard's rho)

**Alternative Curves Considered:**
- **P-384:** Higher security (~192 bits) but slower, unnecessary for this use case
- **Curve25519:** Modern alternative but less universally supported in enterprise Java
- **P-521:** Overkill for security requirements, slower performance

**Validation:**
- Reject keys not on P-256 curve
- Validate key is in correct subgroup
- Check for weak keys (identity element, etc.)

### Key Derivation Function (KDF)

**HKDF-SHA256 (RFC 5869)**

**Properties:**
- **Extract-then-Expand paradigm:** First extract a pseudorandom key, then expand to needed length
- **Domain separation:** Different salt and info for hashing vs. encryption keys
- **Standardized:** Widely reviewed and deployed (TLS 1.3, WireGuard)

**Implementation Details:**
- **Extract phase:** `PRK = HMAC-SHA256(salt, IKM)` where IKM = shared_secret
- **Expand phase:** `OKM = HMAC-SHA256(PRK, info || counter)`

**Hashing Key Derivation:**
```
salt = "HASHING"
info = "OpenToken-Hash"
output_length = 32 bytes
```

**Encryption Key Derivation:**
```
salt = "ENCRYPTION"
info = "OpenToken-Encrypt"
output_length = 32 bytes
```

**Security Properties:**
- Keys are cryptographically independent
- Impossible to derive encryption key from hashing key or vice versa
- Forward secrecy if ephemeral ECDH keys used

### Metadata Security

**Public Key Hashes:**
- Store SHA-256 hashes of public keys (not the keys themselves) in metadata
- Allows audit without exposing keys in JSON logs
- Format: 64-character hex string

**Enhanced Metadata Fields:**
```json
{
  "KeyExchangeMethod": "ECDH-P256",
  "SenderPublicKeyHash": "sha256_hex_hash",
  "ReceiverPublicKeyHash": "sha256_hex_hash",
  "KeyDerivationFunction": "HKDF-SHA256",
  ...
}
```

**Audit Trail:**
- Metadata proves which keys were used
- Enables verification without secret access
- Supports compliance and forensics

### Threat Model

**Protected Against:**
- Eavesdropping: Derived keys never transmitted
- Key reuse attacks: Separate keys for hashing and encryption
- Cryptanalysis: Strong algorithms (ECDH, HKDF, AES-GCM)
- Forward secrecy: Ephemeral keys protect past sessions

**Not Protected Against:**
- Private key theft: If attacker obtains sender/receiver private key
- Implementation bugs: Side-channel attacks, timing attacks (use constant-time implementations)
- Quantum computers: P-256 vulnerable to Shor's algorithm (future migration to post-quantum crypto needed)

**Mitigation Strategies:**
- Secure key storage with proper permissions
- Use constant-time cryptographic implementations
- Plan for post-quantum migration in future versions

---

## File Format Specifications

### Private Key (PEM Format)

**Format:** PKCS#8 unencrypted private key

```pem
-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgVcB/UNPxalR9zDYA
jQIBA6FEA0IABPUZCV/IQJJ5hUKqVV3SIXdnLLDWoUxBvTfNuB8Y2B4X7Hj3xZWh
VVZdVTmLwS5gPQqUzR6wfE8+EFj7cZhbLFQ=
-----END PRIVATE KEY-----
```

**Java Generation:**
```java
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
keyGen.initialize(ecSpec);
KeyPair keyPair = keyGen.generateKeyPair();
```

**Python Generation:**
```python
from cryptography.hazmat.primitives.asymmetric import ec
private_key = ec.generate_private_key(ec.SECP256R1())
```

### Public Key (PEM Format)

**Format:** X.509 SubjectPublicKeyInfo

```pem
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9RkJX8hAknmFQqpVXdIhd2cssNah
TEG9N824HxjYHhfsePfFlaFVVl1VOYvBLmA9CpTNHrB8Tz4QWPtxmFssVA==
-----END PUBLIC KEY-----
```

**Java Export:**
```java
byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
String pem = "-----BEGIN PUBLIC KEY-----\n" 
    + Base64.getMimeEncoder().encodeToString(publicKeyBytes) 
    + "\n-----END PUBLIC KEY-----";
```

**Python Export:**
```python
from cryptography.hazmat.primitives import serialization
pem = public_key.public_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)
```

### ZIP Archive Structure

**File:** `opentoken_output.zip`

**Contents:**

```
opentoken_output.zip
├── tokens.csv                    # Token output (CSV format)
│   OR
├── tokens.parquet                # Token output (Parquet format)
├── tokens.metadata.json          # Enhanced metadata
└── sender_public_key.pem         # Sender's public key (for receiver)
```

**ZIP Creation Requirements:**
- Compression: DEFLATE algorithm
- No encryption on ZIP itself (keys are for token encryption, not ZIP)
- Preserve file timestamps
- Standard ZIP format (compatible with all unzip tools)

### Enhanced Metadata Format

**New Fields Added:**

```json
{
  "Platform": "Java",
  "JavaVersion": "21.0.9",
  "OpenTokenVersion": "1.13.0",
  "KeyExchangeMethod": "ECDH-P256",
  "KeyDerivationFunction": "HKDF-SHA256",
  "SenderPublicKeyHash": "a1b2c3d4e5f6...",
  "ReceiverPublicKeyHash": "f6e5d4c3b2a1...",
  "TotalRows": "1000",
  "TotalRowsWithInvalidAttributes": "10",
  "InvalidAttributesByType": {
    "SocialSecurityNumber": 2,
    "FirstName": 1,
    "PostalCode": 1,
    "LastName": 2,
    "BirthDate": 4
  },
  "BlankTokensByRule": {
    "T1": 5,
    "T2": 12,
    "T3": 3,
    "T4": 8,
    "T5": 7
  }
}
```

**Field Descriptions:**

| Field | Description | Example |
|-------|-------------|---------|
| `KeyExchangeMethod` | Key exchange algorithm and curve | `"ECDH-P256"` |
| `KeyDerivationFunction` | KDF used for key derivation | `"HKDF-SHA256"` |
| `SenderPublicKeyHash` | SHA-256 hash of sender's public key (hex) | `"a1b2c3d4..."` |
| `ReceiverPublicKeyHash` | SHA-256 hash of receiver's public key (hex) | `"f6e5d4c3..."` |

**Backward Compatibility:**
- Legacy secret-based mode continues to output `HashingSecretHash` and `EncryptionSecretHash`
- Public-key mode outputs key exchange fields instead
- Consumers can detect mode by presence/absence of fields

---

## Testing Strategy

### Unit Tests

**Key Management Tests:**
- Key pair generation (validate curve, key size)
- Key serialization/deserialization (PEM round-trip)
- Key loading from file (valid and invalid files)
- Permission validation (reject world-readable private keys)

**Key Exchange Tests:**
- ECDH agreement with known test vectors
- Verify sender and receiver derive identical shared secrets
- HKDF derivation with RFC 5869 test vectors
- Validate hashing and encryption keys are different

**Output Packaging Tests:**
- ZIP creation and extraction
- Validate ZIP structure (file names, contents)
- Verify file integrity after packaging
- Test with both CSV and Parquet inputs

**Test Data:**
```java
// RFC 5869 HKDF-SHA256 Test Case 1
IKM = 0x0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b (22 octets)
salt = 0x000102030405060708090a0b0c (13 octets)
info = 0xf0f1f2f3f4f5f6f7f8f9 (10 octets)
L = 42

Expected OKM:
0x3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf
  34007208d5b887185865
```

### Integration Tests

**End-to-End Token Generation:**
- Generate tokens using public-key mode
- Validate ZIP output contains all required files
- Verify metadata contains correct key exchange fields
- Extract and parse tokens from ZIP

**Decryption Mode:**
- Generate tokens with sender key pair
- Decrypt tokens with receiver key pair
- Validate decrypted tokens match originals
- Test with both CSV and Parquet formats

**Backward Compatibility:**
- Generate tokens in legacy secret mode
- Verify output format unchanged
- Validate deprecation warnings appear
- Test migration from secret to public-key mode

**Test Matrix:**

| Test Case | Input Format | Output Format | Mode | Expected Result |
|-----------|--------------|---------------|------|-----------------|
| TC-1 | CSV | ZIP | Public-key | Success |
| TC-2 | Parquet | ZIP | Public-key | Success |
| TC-3 | CSV | CSV+metadata | Legacy | Success + warning |
| TC-4 | CSV | ZIP | Decryption | Decrypted tokens |

### Cross-Language Interoperability Tests

**Critical Tests:**
1. **Java Sender → Python Receiver:**
   - Java generates tokens with public-key mode
   - Python decrypts using same key pair
   - Validate tokens match

2. **Python Sender → Java Receiver:**
   - Python generates tokens with public-key mode
   - Java decrypts using same key pair
   - Validate tokens match

3. **Key Derivation Consistency:**
   - Same ECDH inputs in Java and Python
   - Validate byte-identical shared secrets
   - Validate byte-identical derived keys

**Test Automation:**
```bash
# Run interoperability test suite
cd tools/interoperability
./run_pubkey_tests.sh

# Expected output:
# [PASS] Java → Python: 100/100 tokens match
# [PASS] Python → Java: 100/100 tokens match
# [PASS] Key derivation: byte-identical
```

### Security Tests

**Key Validation:**
- Reject keys on wrong curve (e.g., P-384, Curve25519)
- Reject malformed PEM files
- Reject invalid DER encoding
- Reject identity element as public key

**Permission Checks:**
- Warn if private key has world-readable permissions
- Validate key files are not symlinks to insecure locations
- Test key generation creates files with correct permissions

**Cryptographic Correctness:**
- Test against NIST CAVP test vectors (ECDH, HKDF)
- Verify constant-time key comparison
- Test handling of edge cases (null keys, empty files)

### Performance Tests

**Benchmarks:**
- Key pair generation time (target: <10ms)
- ECDH computation time (target: <5ms)
- HKDF derivation time (target: <1ms)
- ZIP packaging overhead (target: <100ms for 1MB file)

**Comparison:**
- Legacy secret mode: baseline
- Public-key mode: target <5% overhead
- Measure on representative datasets (1K, 10K, 100K records)

**Test Environment:**
- Standard laptop (Intel i7 or equivalent)
- 8GB RAM
- OpenJDK 21, Python 3.11

---

## Documentation Updates

### 1. README.md Updates

**Changes Required:**
- Update "Overview" section to mention public-key support
- Add new usage examples for public-key mode
- Update "Token Encryption Process" diagram
- Add migration notice for existing users

**New Sections:**
- "Public-Key Cryptography" overview
- "Quick Start with Public Keys" examples
- Link to key exchange documentation

**Example:**
```markdown
## Public-Key Cryptography (v1.13.0+)

OpenToken now supports public-key cryptography for enhanced security:

### Generate Tokens with Public Keys

```bash
# Receiver generates their key pair
opentoken --generate-keypair --output-public-key receiver_public_key.pem

# Sender generates tokens using receiver's public key
opentoken \
  --input persons.csv \
  --receiver-public-key receiver_public_key.pem \
  --output tokens.zip

# Output: tokens.zip containing encrypted tokens and sender's public key
```

See [Key Exchange Documentation](docs/key-exchange.md) for details.
```

### 2. New Documentation: `docs/key-exchange.md`

**Contents:**
- Detailed cryptographic specification
- ECDH and HKDF explanation
- Key management best practices
- Security considerations
- Troubleshooting guide

**Outline:**
```markdown
# OpenToken Key Exchange Documentation

## Overview
## Cryptographic Approach
  - Elliptic Curve Diffie-Hellman (ECDH)
  - Key Derivation Function (HKDF)
  - Token Processing
## Key Management
  - Generating Key Pairs
  - Storing Keys Securely
  - Distributing Public Keys
  - Key Rotation
## Usage Examples
  - Token Generation Workflow
  - Token Decryption Workflow
  - Multi-Party Scenarios
## Security Best Practices
## Troubleshooting
## FAQ
```

### 3. New Documentation: `docs/migration-guide.md`

**Contents:**
- Migration from v1.12 to v1.13
- Backward compatibility notes
- Step-by-step migration instructions
- Comparison: secret vs. public-key modes

**Outline:**
```markdown
# Migration Guide: Secret-Based to Public-Key Cryptography

## Overview
## Why Migrate?
  - Enhanced Security
  - Forward Secrecy
  - Simplified Key Management
## Backward Compatibility
  - Legacy Mode Support
  - Deprecation Timeline
## Migration Steps
  1. Install OpenToken v1.13.0
  2. Generate Key Pairs
  3. Distribute Public Keys
  4. Update Workflows
  5. Test and Validate
## Comparison Table
  | Feature | Secret-Based | Public-Key |
  |---------|--------------|------------|
  | ... | ... | ... |
## Common Issues
## Rollback Procedure
```

### 4. Update: `docs/metadata-format.md`

**Changes:**
- Add documentation for new metadata fields
- Update example JSON
- Explain key hash format
- Document legacy vs. public-key mode differences

**New Sections:**
```markdown
## Public-Key Mode Metadata (v1.13.0+)

In public-key mode, the metadata includes additional fields:

| Field | Description |
|-------|-------------|
| `KeyExchangeMethod` | Key exchange algorithm (e.g., "ECDH-P256") |
| `KeyDerivationFunction` | KDF used (e.g., "HKDF-SHA256") |
| `SenderPublicKeyHash` | SHA-256 hash of sender's public key |
| `ReceiverPublicKeyHash` | SHA-256 hash of receiver's public key |

### Example (Public-Key Mode)

```json
{
  "Platform": "Java",
  "JavaVersion": "21.0.9",
  "OpenTokenVersion": "1.13.0",
  "KeyExchangeMethod": "ECDH-P256",
  "KeyDerivationFunction": "HKDF-SHA256",
  ...
}
```

### Legacy Mode vs. Public-Key Mode

| Mode | Fields Present |
|------|----------------|
| Legacy | `HashingSecretHash`, `EncryptionSecretHash` |
| Public-Key | `KeyExchangeMethod`, `SenderPublicKeyHash`, ... |
```

### 5. CLI Help Text Updates

**Java (`Main.java`):**
```java
// Add to command-line help
@Option(names = {"--receiver-public-key"}, 
        description = "Path to receiver's public key (PEM format) [public-key mode]")
private File receiverPublicKey;

@Option(names = {"--sender-keypair-path"}, 
        description = "Path to sender's private key (default: ~/.opentoken/keypair.pem)")
private File senderKeypairPath;
```

**Python (`main.py`):**
```python
# Add to argparse
parser.add_argument(
    '--receiver-public-key',
    type=Path,
    help='Path to receiver\'s public key (PEM format) [public-key mode]'
)
```

### 6. Developer Documentation

**Update:** `docs/dev-guide-development.md`

**Add Sections:**
- "Implementing Key Exchange Features"
- "Testing Key Exchange Implementations"
- "Cross-Language Parity for Cryptography"

---

## Dependencies

### Java (pom.xml)

**Existing Dependencies (Sufficient):**
- `java.security.*` - Built-in support for ECDH, HKDF, AES-GCM
- No additional dependencies required for core functionality

**Optional Dependency:**
```xml
<!-- Optional: For additional curve support and utilities -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.77</version>
    <optional>true</optional>
</dependency>
```

**Note:** Bouncy Castle is not strictly required for P-256 ECDH but may be added for:
- Additional curve support (Curve25519, etc.)
- Utility methods for key format conversion
- Enhanced PEM parsing

### Python (requirements.txt)

**Existing Dependency:**
```
cryptography>=42.0.2
```

**Status:** Already present and sufficient. Includes:
- `cryptography.hazmat.primitives.asymmetric.ec` - ECDH support
- `cryptography.hazmat.primitives.kdf.hkdf` - HKDF support
- `cryptography.hazmat.primitives.serialization` - PEM/DER support
- `cryptography.hazmat.primitives.ciphers` - AES-GCM support

**No additional dependencies required.**

### Build Tools

**Maven Plugins (Java):**
- No changes required
- Existing Checkstyle, JaCoCo, and test plugins sufficient

**pytest Plugins (Python):**
- No changes required
- Existing pytest and pytest-cov sufficient

---

## Risk Assessment

### High Risks

**None identified.**

### Medium Risks

1. **Cross-Language Parity for ECDH**
   - **Risk:** Java and Python may produce different shared secrets or derived keys
   - **Impact:** Receiver cannot decrypt tokens
   - **Mitigation:**
     - Use identical test vectors for both implementations
     - Extensive interoperability testing
     - Validate byte-identical outputs at each step
   - **Probability:** Low-Medium

2. **Key Format Compatibility**
   - **Risk:** PEM encoding differences between Java and Python
   - **Impact:** Keys cannot be loaded across implementations
   - **Mitigation:**
     - Use standardized PEM formats (PKCS#8, X.509)
     - Test key exchange between Java and Python
     - Validate against external tools (OpenSSL)
   - **Probability:** Low

3. **Performance Impact**
   - **Risk:** ECDH computation adds latency to token generation
   - **Impact:** Slower processing for large datasets
   - **Mitigation:**
     - Benchmark ECDH overhead (expected <5ms)
     - Optimize key caching for batch processing
     - Consider key reuse for large batches
   - **Probability:** Low

### Low Risks

1. **ZIP File Size**
   - **Risk:** ZIP adds overhead to output files
   - **Impact:** Slightly larger file sizes
   - **Mitigation:** Negligible (public key ~100 bytes, ZIP overhead ~100 bytes)
   - **Probability:** N/A

2. **Backward Compatibility**
   - **Risk:** Breaking existing workflows using secret-based mode
   - **Impact:** Users cannot upgrade without changes
   - **Mitigation:** Maintain legacy mode indefinitely with warnings
   - **Probability:** Very Low

3. **Documentation Lag**
   - **Risk:** Documentation incomplete at release time
   - **Impact:** User confusion, support burden
   - **Mitigation:** Documentation as part of each phase, review before release
   - **Probability:** Low

### Risk Mitigation Summary

| Risk | Mitigation Strategy | Owner | Status |
|------|---------------------|-------|--------|
| Cross-language parity | Extensive interop testing | Both teams | Planned |
| Key format compatibility | Standard formats + validation | Both teams | Planned |
| Performance impact | Benchmarking + optimization | Both teams | Planned |
| Documentation lag | Docs in each phase | Both teams | Planned |

---

## Rollout Plan

### Phase 1: Alpha Release (1.13.0-alpha)

**Timeline:** Week 8  
**Audience:** Internal developers only

**Scope:**
- Core functionality implemented (Java and Python)
- Public-key mode works end-to-end
- No backward compatibility yet
- Minimal documentation

**Success Criteria:**
- All unit tests passing
- Basic integration tests passing
- Cross-language key exchange works

**Deliverables:**
- Alpha build artifacts
- Internal testing guide

---

### Phase 2: Beta Release (1.13.0-beta)

**Timeline:** Week 10  
**Audience:** Early adopters, trusted partners

**Scope:**
- Backward compatibility with legacy mode
- Complete documentation
- Migration guide available
- Enhanced error messages

**Success Criteria:**
- All tests passing (unit, integration, interop)
- Documentation reviewed and approved
- Beta testers successfully migrate

**Deliverables:**
- Beta build artifacts
- Complete documentation suite
- Migration guide
- Known issues list

---

### Phase 3: Release Candidate (1.13.0-rc)

**Timeline:** Week 11  
**Audience:** All users (pre-release)

**Scope:**
- Feature complete
- Bug fixes from beta feedback
- Performance optimization
- Security review completed

**Success Criteria:**
- No critical bugs
- Performance benchmarks met
- Security audit passed
- User acceptance testing completed

**Deliverables:**
- RC build artifacts
- Release notes draft
- Security audit report
- Performance benchmark report

---

### Phase 4: General Availability (1.13.0)

**Timeline:** Week 12  
**Audience:** All users

**Scope:**
- Production-ready release
- Complete documentation
- Full support available

**Success Criteria:**
- No blocking issues
- Documentation complete
- Support processes in place

**Deliverables:**
- GA build artifacts
- Final release notes
- Updated README
- Blog post / announcement

---

### Post-Release

**Deprecation Timeline:**
- **v1.13.0:** Legacy mode supported with warnings
- **v1.14.0:** Legacy mode still supported (Q2 2026)
- **v2.0.0:** Legacy mode removed (Q4 2026, breaking change)

**Support Plan:**
- Active support for public-key mode
- Maintenance support for legacy mode
- Migration assistance for enterprise users

---

## Architecture Diagrams

### New Encryption Flow with ECDH

```mermaid
flowchart TD
    A[Person Attributes] --> B[Normalize & Validate]
    B --> C[Generate Token Signatures]
    
    D[Receiver Public Key] --> E[Load Key]
    F[Sender Key Pair] -->|exists?| G{Check}
    G -->|Yes| H[Load Existing]
    G -->|No| I[Generate New]
    H --> J[Sender Private Key]
    I --> J
    
    E --> K[ECDH Key Exchange]
    J --> K
    K --> L[Shared Secret]
    
    L --> M[HKDF: Derive Hashing Key]
    L --> N[HKDF: Derive Encryption Key]
    
    C --> O[HMAC-SHA256]
    M --> O
    O --> P[Hashed Tokens]
    
    P --> Q[AES-256-GCM Encrypt]
    N --> Q
    Q --> R[Encrypted Tokens]
    
    R --> S[Write to CSV/Parquet]
    S --> T[Create Metadata JSON]
    J --> U[Export Sender Public Key]
    
    T --> V[Package into ZIP]
    S --> V
    U --> V
    
    V --> W[Output: opentoken_output.zip]
    
    style K fill:#90EE90
    style L fill:#90EE90
    style M fill:#90EE90
    style N fill:#90EE90
    style V fill:#FFD700
```

---

### Key Exchange Sequence Diagram

```mermaid
sequenceDiagram
    participant Sender
    participant SenderKeyMgr as Sender Key Manager
    participant Receiver
    participant ReceiverKey as Receiver Public Key
    
    Sender->>ReceiverKey: Receive public key file
    Sender->>SenderKeyMgr: Check for existing key pair
    
    alt Key pair exists
        SenderKeyMgr->>Sender: Load private key
    else No key pair
        SenderKeyMgr->>SenderKeyMgr: Generate ECDH key pair (P-256)
        SenderKeyMgr->>Sender: Return new private key
        SenderKeyMgr->>SenderKeyMgr: Save to ~/.opentoken/keypair.pem
    end
    
    Sender->>Sender: Perform ECDH(sender_private, receiver_public)
    Sender->>Sender: shared_secret = ECDH output
    
    Sender->>Sender: hashing_key = HKDF(shared_secret, "HASHING", "OpenToken-Hash")
    Sender->>Sender: encryption_key = HKDF(shared_secret, "ENCRYPTION", "OpenToken-Encrypt")
    
    Sender->>Sender: Process tokens with derived keys
    Sender->>Sender: Package output with sender's public key
    
    Sender->>Receiver: Send ZIP (tokens + metadata + sender_public_key.pem)
    
    Note over Receiver: Decryption Flow (Symmetric)
    Receiver->>Receiver: Extract sender's public key from ZIP
    Receiver->>Receiver: Perform ECDH(receiver_private, sender_public)
    Receiver->>Receiver: Derive same hashing_key and encryption_key
    Receiver->>Receiver: Decrypt tokens
```

---

### Component Architecture Diagram

```mermaid
graph TB
    subgraph "New Components"
        A[KeyPairManager]
        B[KeyExchange]
        C[PublicKeyLoader]
        D[OutputPackager]
    end
    
    subgraph "Modified Components"
        E[Main CLI]
        F[CommandLineArguments]
        G[Metadata]
    end
    
    subgraph "Existing Components<br/>(Unchanged Core Logic)"
        H[HashTokenTransformer]
        I[EncryptTokenTransformer]
        J[PersonAttributesProcessor]
    end
    
    subgraph "Output"
        K[ZIP Archive]
        L[CSV/Parquet]
        M[metadata.json]
        N[sender_public_key.pem]
    end
    
    E -->|Uses| A
    E -->|Uses| B
    E -->|Uses| C
    E -->|Uses| D
    E -->|Updates| F
    
    B -->|Derives Keys| H
    B -->|Derives Keys| I
    
    J -->|Uses| H
    J -->|Uses| I
    
    D -->|Packages| K
    K -->|Contains| L
    K -->|Contains| M
    K -->|Contains| N
    
    G -->|Enhances| M
    A -->|Generates| N
    
    style A fill:#ADD8E6
    style B fill:#ADD8E6
    style C fill:#ADD8E6
    style D fill:#ADD8E6
    style K fill:#FFD700
```

---

### Backward Compatibility Flow

```mermaid
flowchart TD
    A[CLI Arguments] --> B{Mode Detection}
    
    B -->|--receiver-public-key provided| C[Public Key Mode]
    B -->|--hashing-secret provided| D[Legacy Secret Mode]
    
    C --> E[Load/Generate Sender Keys]
    C --> F[Load Receiver Public Key]
    E --> G[Perform ECDH]
    F --> G
    G --> H[Derive Keys via HKDF]
    
    D --> I[Warn: Deprecated Mode]
    I --> J[Use Provided Secrets Directly]
    
    H --> K[Token Processing]
    J --> K
    
    K --> L{Output Format}
    
    L -->|Public Key Mode| M[Create ZIP Archive]
    L -->|Legacy Mode| N[CSV/Parquet + metadata.json]
    
    style D fill:#ff9999
    style I fill:#ff9999
    style N fill:#ffcccc
    style C fill:#90EE90
    style M fill:#FFD700
```

---

### Data Flow: Token Generation with Public Keys

```mermaid
flowchart LR
    A[Input CSV/Parquet] --> B[PersonAttributesProcessor]
    
    C[Receiver Public Key] --> D[PublicKeyLoader]
    E[Sender Key Pair] --> F[KeyPairManager]
    
    D --> G[KeyExchange]
    F --> G
    
    G --> H[Derived Hashing Key]
    G --> I[Derived Encryption Key]
    
    B --> J[Token Signatures]
    
    J --> K[HashTokenTransformer]
    H --> K
    K --> L[Hashed Tokens]
    
    L --> M[EncryptTokenTransformer]
    I --> M
    M --> N[Encrypted Tokens]
    
    N --> O[CSV/Parquet Writer]
    O --> P[tokens.csv/parquet]
    
    B --> Q[Metadata Generator]
    Q --> R[tokens.metadata.json]
    
    F --> S[sender_public_key.pem]
    
    P --> T[OutputPackager]
    R --> T
    S --> T
    
    T --> U[opentoken_output.zip]
    
    style U fill:#FFD700
    style G fill:#90EE90
```

---

## Summary

This implementation plan provides a comprehensive roadmap for transitioning OpenToken to public-key cryptography:

### Key Benefits

1. **Enhanced Security:**
   - Eliminates need for secure secret distribution
   - Provides forward secrecy with ephemeral keys
   - Cryptographic key separation (hashing vs. encryption)

2. **Preserved Compatibility:**
   - Token generation algorithms unchanged
   - Cross-language parity maintained
   - Backward compatibility with legacy mode

3. **Modern Packaging:**
   - ZIP output for convenient distribution
   - Enhanced metadata with key exchange information
   - Simplified receiver workflow (all files in one archive)

4. **Production Ready:**
   - Comprehensive testing strategy
   - Phased rollout plan
   - Complete documentation suite

### Implementation Timeline

- **Weeks 1-3:** Core cryptographic infrastructure (both languages)
- **Weeks 4-7:** CLI integration and output packaging
- **Week 8:** Alpha release (internal testing)
- **Weeks 9-10:** Beta release (early adopters)
- **Week 11:** Release candidate (final testing)
- **Week 12:** General availability (v1.13.0)

### Success Metrics

- ✅ Cross-language interoperability: 100% test pass rate
- ✅ Performance overhead: <5% vs. legacy mode
- ✅ Security audit: No critical findings
- ✅ Documentation coverage: Complete user and developer guides
- ✅ Backward compatibility: Legacy mode functional with warnings

### Next Steps

1. **Approval:** Review and approve this implementation plan
2. **Kickoff:** Assign teams and schedule phase 1 work
3. **Dependencies:** Ensure development environments have required libraries
4. **Tracking:** Set up project board for phase tracking
5. **Communication:** Notify stakeholders of upcoming changes

---

## Appendix

### References

- **RFC 5869:** HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
- **NIST FIPS 186-4:** Digital Signature Standard (DSS) - Elliptic Curve Cryptography
- **SEC 2:** Recommended Elliptic Curve Domain Parameters
- **RFC 8439:** ChaCha20 and Poly1305 for IETF Protocols (AES-GCM reference)

### Glossary

- **ECDH:** Elliptic Curve Diffie-Hellman key exchange
- **HKDF:** HMAC-based Key Derivation Function
- **P-256:** NIST P-256 elliptic curve (secp256r1)
- **PEM:** Privacy-Enhanced Mail format for keys
- **KDF:** Key Derivation Function
- **Forward Secrecy:** Property where compromise of long-term keys does not compromise past session keys

### Contact

For questions about this implementation plan, contact:
- **Technical Lead:** [Name]
- **Security Review:** [Name]
- **Project Manager:** [Name]

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-18  
**Status:** Proposed  
**Next Review:** After approval
