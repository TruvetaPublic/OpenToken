# Security

Cryptographic building blocks, key management expectations, and security considerations for privacy-preserving person matching.

## Overview

OpenToken generates cryptographically secure tokens for privacy-preserving person matching across datasets. The system uses deterministic hashing and optional encryption to prevent re-identification while enabling matching on identical person attributes.

**Key security properties:**
- Tokens are one-way (cannot reverse to original data without secrets)
- Same input produces same token (deterministic matching)
- Validation rejects placeholders and malformed data before tokenization
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
HMAC-SHA256 (authenticated hash with hashing secret)
  ↓
AES-256-GCM Encrypt (symmetric encryption with encryption key)
  ↓
Base64 Encode (storable format)
```

**Hash-only mode (alternative):**
```
Token Signature
  ↓
SHA-256 Hash
  ↓
HMAC-SHA256 (with hashing secret)
  ↓
Base64 Encode
```

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
- **Input**: SHA-256 hash + hashing secret
- **Output**: 256-bit authenticated hash
- **Purpose**: Prevent rainbow table attacks and verify secret usage

**Security benefits:**
- Requires secret key to generate matching hashes
- Prevents pre-computation of token values
- Different secret produces completely different output for same input

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

## Key and Secret Management

### Required Secrets

**Hashing Secret:**
- Used for HMAC-SHA256 authentication
- Required for all modes (encryption and hash-only)
- No strict length requirement (minimum 8 characters recommended)
- Recommended: 16+ characters with mixed case, digits, symbols

**Encryption Key:**
- Used for AES-256-GCM encryption/decryption
- Required for encryption mode only (omitted in hash-only mode)
- **Must be exactly 32 characters (32 bytes for UTF-8)**
- Example: `"12345678901234567890123456789012"`

### Storage Expectations

**Production environment:**
- Store secrets in secure secret management systems (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault)
- Use environment variables loaded from secret stores
- Never commit secrets to version control
- Rotate secrets periodically and maintain version history

**Example using environment variables:**
```bash
export OPENTOKEN_HASHING_SECRET=$(aws secretsmanager get-secret-value \
  --secret-id opentoken-hash-key --query SecretString --output text)
export OPENTOKEN_ENCRYPTION_KEY=$(aws secretsmanager get-secret-value \
  --secret-id opentoken-enc-key --query SecretString --output text)

java -jar opentoken-cli-*.jar \
  -i data.csv -t csv -o tokens.csv \
  -h "$OPENTOKEN_HASHING_SECRET" \
  -e "$OPENTOKEN_ENCRYPTION_KEY"
```

**Test/development environment:**
- Use placeholder values clearly marked as non-production
- Example: `HashingKey` and `Secret-Encryption-Key-Goes-Here.`

### Secret Verification

Metadata files contain SHA-256 hashes of secrets (not the secrets themselves):

```json
{
  "HashingSecretHash": "abc123...",
  "EncryptionSecretHash": "def456..."
}
```

**Purpose:**
- Verify correct secrets were used without exposing them
- Detect configuration errors (mismatched secrets between runs)
- Audit trail for compliance

**Verification process:**
```bash
python tools/hash_calculator.py \
  --hashing-secret "YourSecret" \
  --encryption-key "YourKey"
# Compare output hashes to metadata file
```

---

## Security Considerations and Limitations

### What OpenToken Protects Against

**✓ Re-identification without secrets:**
- Encrypted tokens cannot be reversed without encryption key
- Hashed tokens cannot be reversed (one-way HMAC-SHA256)
- Attacker with tokens alone cannot recover person data

**✓ Rainbow table attacks:**
- HMAC-SHA256 with secret prevents pre-computed lookup tables
- Different secret produces different tokens for same input

**✓ Data quality issues:**
- Validation rejects placeholders (`Unknown`, `Test`, `N/A`)
- Normalization handles format variations (dates, names, ZIP codes)
- Invalid records tracked in metadata (not silently processed)

### What OpenToken Does NOT Protect Against

**✗ Compromise of secrets:**
- If attacker obtains hashing secret + encryption key, they can regenerate tokens from known person data
- Token security depends entirely on secret protection

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

### Validation Safeguards

OpenToken validates all attributes before tokenization:

**FirstName and LastName:**
- Reject empty/null values
- Reject placeholders (`Unknown`, `Test`, `NotAvailable`, `Patient`, `Sample`)
- Require at least one alphabetic character after normalization

**BirthDate:**
- Reject dates before 1910-01-01 (implausible)
- Reject future dates (invalid)
- Normalize multiple formats to `YYYY-MM-DD`

**SocialSecurityNumber:**
- Reject reserved area codes (000, 666, 900-999)
- Reject zero group or serial numbers
- Reject common invalid sequences (111-11-1111, etc.)

**PostalCode:**
- Reject placeholder ZIP codes (00000, 11111, 12345)
- Reject reserved Canadian postal codes
- Auto-pad short ZIP codes to 5 digits

**Sex:**
- Accept only `Male` or `Female` (case-insensitive)

See [Concepts: Normalization and Validation](concepts/normalization-and-validation.md) for complete validation rules.

---

## Next Steps

- **View detailed crypto pipeline**: [Specification](specification.md)
- **Understand metadata security**: [Reference: Metadata Format](reference/metadata-format.md)
- **Review validation rules**: [Concepts: Normalization and Validation](concepts/normalization-and-validation.md)
- **Configure OpenToken**: [Configuration](config/configuration.md)
