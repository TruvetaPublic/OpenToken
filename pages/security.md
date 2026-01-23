---
layout: default
---

# Security

Cryptographic building blocks, key management expectations, and security considerations for privacy-preserving person matching.

## Overview

OpenToken generates cryptographically secure tokens for privacy-preserving person matching across datasets. The system uses deterministic hashing and optional encryption to prevent re-identification while enabling matching on identical person attributes.

**Key security properties:**
- Tokens are one-way (cannot reverse to original data without the required private keys)
- Same input produces same token (deterministic matching)
- Metadata tracks processing statistics without exposing person data

---

## Cryptographic Building Blocks

### Token Transformation Pipeline

OpenToken transforms person attributes through multiple layers:

**Encryption mode (default):**
```
Token Signature (normalized attributes)
  ↓
SHA-256 Hash (one-way digest, 256-bit)
  ↓
HMAC-SHA256 (authenticated hash with derived hashing key)
  ↓
AES-256-GCM Encrypt (symmetric encryption with derived encryption key)
  ↓
Base64 Encode (storable format)
```

**Hash-only mode (alternative):**
```
Token Signature
  ↓
SHA-256 Hash
  ↓
HMAC-SHA256 (with derived hashing key)
  ↓
Base64 Encode
```

**Where the keys come from:**
- In key-exchange workflows, hashing + encryption keys are derived via ECDH from the sender's private key and the receiver's public key (no dedicated shared secrets are exchanged).

### SHA-256 (Secure Hash Algorithm)

- **Standard**: FIPS 180-4
- **Output**: 256-bit (32-byte) fixed-size digest
- **Collision resistance**: ~2^128 computational effort
- **Purpose**: Convert variable-length token signatures to fixed-size digests

**Properties:**
- One-way function (cannot reverse hash to input)
- Avalanche effect (small input change produces completely different hash)
- Deterministic (same input always produces same hash)

### HMAC-SHA256 (Hash-based Message Authentication Code)

- **Standard**: FIPS 198-1
- **Input**: SHA-256 hash + hashing key
- **Output**: 256-bit authenticated hash
- **Purpose**: Prevent rainbow table attacks and verify secret usage

**Security benefits:**
- Requires key material to generate matching hashes
- Prevents pre-computation of token values
- Different key material produces completely different output for same input

**Formula:**
```
HMAC-SHA256(message, key) = SHA256((key ⊕ opad) || SHA256((key ⊕ ipad) || message))
```

### AES-256-GCM (Advanced Encryption Standard with Galois/Counter Mode)

- **Standard**: FIPS 197
- **Key size**: 256-bit (32-byte)
- **Mode**: GCM (Galois/Counter Mode) with authentication
- **Purpose**: Encrypt tokens to prevent re-identification

**Technical details:**
- **Initialization Vector (IV)**: 12 bytes, randomly generated per token
- **Authentication tag**: 128-bit (16-byte) GCM tag for integrity
- **Padding**: NoPadding (GCM mode handles message length)
- **Algorithm**: `AES/GCM/NoPadding`

**Security properties:**
- Authenticated encryption (detects tampering)
- Unique IV per token prevents pattern analysis
- Computationally infeasible to brute-force (2^256 possible keys)
- Reversible only with correct encryption key

---

## Key Management & Secrets
## Key Management (ECDH Public Key Exchange)

OpenToken supports exchanging token data without sharing dedicated symmetric secrets. Instead, it uses ECDH key exchange:

- Each party generates an ECDH keypair (`keypair.pem`) and derives a public key (`public_key.pem`).
- The **receiver shares only their public key** with the sender.
- The **sender** tokenizes and encrypts using:
  - sender private key (`--sender-keypair-path`)
  - receiver public key (`--receiver-public-key`)
- The **receiver** decrypts using:
  - receiver private key (`--receiver-keypair-path`)
  - sender public key (usually included in the output package)

### What to Protect

- **Private keys are secrets.** Protect `keypair.pem` the same way you would protect encryption keys.
- Public keys can be shared, but still validate/verify them (see Metadata verification below).

### Development / Local Testing

```bash
# Receiver: generate keypair (share public key with sender)
opentoken generate-keypair --output-dir ./keys/receiver --ecdh-curve P-384

# Sender: generate keypair
opentoken generate-keypair --output-dir ./keys/sender --ecdh-curve P-384

# Sender: tokenize
opentoken tokenize \
  -i sample.csv -t csv -o output.zip \
  --receiver-public-key ./keys/receiver/public_key.pem \
  --sender-keypair-path ./keys/sender/keypair.pem \
  --ecdh-curve P-384

# Receiver: decrypt
opentoken decrypt \
  -i output.zip -t csv -o decrypted.csv \
  --receiver-keypair-path ./keys/receiver/keypair.pem \
  --ecdh-curve P-384
```

### Production

Store private keys in a managed secret store or HSM-backed solution and inject them into jobs at runtime.

| Platform   | Key Store / Secret Store | Injection Method                            |
| ---------- | ------------------------ | ------------------------------------------- |
| AWS        | KMS + Secrets Manager    | ECS/Lambda secrets, SSM, or file mounts     |
| Azure      | Key Vault                | App Service key references or secret mounts |
| GCP        | KMS + Secret Manager     | Workload identity + secret mounts           |
| On-prem    | HashiCorp Vault          | Vault agent + file template                 |
| Databricks | Databricks Secrets       | `dbutils.secrets.get(...)` + ephemeral file |

### Key Rotation

1. Generate a new keypair.
2. Distribute the new public key to partners.
3. Re-run token generation and decryption for the desired period.
4. Retire old private keys only after all dependent downstream jobs have moved.

### What NOT to Do

- Never commit private keys to source control.
- Never log private keys.
- Avoid hard-coding key paths in scripts checked into git; use environment-specific configuration.

### Key Verification via Metadata

Each run produces a `.metadata.json` with SHA-256 hashes of the public keys used:

```json
{
  "KeyExchangeMethod": "ECDH-P-384",
  "SenderPublicKeyHash": "a85b4bd6...",
  "ReceiverPublicKeyHash": "32bc0e98..."
}
```

You can verify these hashes locally using the public key files:

```bash
sha256sum ./keys/sender/public_key.pem
sha256sum ./keys/receiver/public_key.pem
```

### Cross-References

- **CLI commands and flags**: [CLI Reference](reference/cli.md)
- **Environment variable usage**: [Configuration](config/configuration.md#environment-variables)
- **Databricks / Spark secrets (shared-secret mode)**: [Spark or Databricks](operations/spark-or-databricks.md)
- **Running the CLI**: [Running OpenToken](running-opentoken/index.md)
- **Metadata format (key hashes)**: [Reference: Metadata Format](reference/metadata-format.md)

---

## Security Considerations and Limitations

### What OpenToken Protects Against

**✓ Re-identification without the required private keys:**
- Encrypted tokens cannot be reversed without the receiver private key
- Hashed tokens cannot be reversed (one-way HMAC-SHA256)
- Attacker with tokens alone cannot recover person data

**✓ Rainbow table attacks:**
- HMAC-SHA256 with key material prevents pre-computed lookup tables
- Different key material produces different tokens for same input

**✓ Data quality issues:**
Metadata captures processing statistics; data quality guidance lives in the concepts documentation.

### What OpenToken Does NOT Protect Against

**✗ Compromise of private keys:**
- If an attacker obtains the required private key(s), they can decrypt token packages intended for that receiver
- Token security depends on private key protection and secure operational controls

**✗ Side-channel attacks:**
- Timing attacks, memory access patterns not specifically mitigated
- Use secure execution environments for sensitive workloads

**✗ Statistical analysis with auxiliary data:**
- If attacker has auxiliary demographic data and token frequency distributions, statistical attacks may be possible
- Consider differential privacy techniques for high-risk scenarios

**✗ Token distribution analysis:**
- Tokens are deterministic (same person always produces same token)
- Frequency analysis may reveal population patterns
- Mitigate by limiting token distribution and enforcing access controls

### User Responsibilities

OpenToken provides cryptographic primitives but **users are responsible for:**

- **Secret management**: Storing, rotating, and protecting hashing secrets and encryption keys
- **Access control**: Limiting who can generate, access, or decrypt tokens
- **Token storage**: Encrypting token files at rest (file system encryption, database encryption)
- **Audit logging**: Tracking token generation, access, and decryption events
- **Data minimization**: Deleting raw person data after token generation
- **Compliance**: Ensuring usage aligns with HIPAA, GDPR, or organizational policies

### Threat Model Assumptions

**Assumptions:**
- Secrets are stored securely and not accessible to unauthorized parties
- Execution environment is trusted (no malware or unauthorized access)
- Token outputs are protected with access controls
- Users validate data quality before token generation

**Out of scope:**
- Protection against compromised execution environments
- Protection after decryption (decrypted tokens are plaintext hashes)
- Protection against authorized users misusing tokens

### Data Quality: Normalization and Validation

Normalization and validation rules are documented separately to keep this page focused on cryptography and secret management.

See [Concepts: Normalization and Validation](concepts/normalization-and-validation.md).

---

## Next Steps

- **View detailed crypto pipeline**: [Specification](specification.md)
- **Understand metadata security**: [Reference: Metadata Format](reference/metadata-format.md)
- **Review validation rules**: [Concepts: Normalization and Validation](concepts/normalization-and-validation.md)
- **Configure OpenToken**: [Configuration](config/configuration.md)
- **Share tokens across organizations**: [Sharing Tokenized Data](operations/sharing-tokenized-data.md)
