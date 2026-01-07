---
layout: default
---

# Security

Cryptography, validation rules, and privacy-preserving best practices.

## Cryptography

OpenToken uses industry-standard algorithms to prevent re-identification and ensure data integrity.

### Token Transformation Pipeline

The token generation process uses a multi-layer approach for security:

```
Token Signature (plaintext attribute combination)
        ↓
    SHA-256 Hash (one-way digest)
        ↓
HMAC-SHA256 (keyed hash for integrity)
        ↓
AES-256 Encrypt (symmetric encryption)
        ↓
Base64 Encode (for storage/transmission)
```

### Hashing: SHA-256

- **Standard**: FIPS 180-4
- **Output**: 256-bit (32-byte) hash
- **Collision Resistance**: ~2^128 effort (cryptographically secure)
- **Purpose**: Convert token signature to fixed-size digest

**Example:**
```
Input: "DOE|JOHN|MALE|1980-01-15"
SHA-256: a3f5c7d9e1b4f2c8a6d3e9f1b4c7a0d2e5f8b1c4d7a0e3f6c9b2e5f8a1d4
```

### HMAC-SHA256 (Message Authentication Code)

- **Standard**: FIPS 198-1
- **Key Requirement**: Hashing secret (minimum 8 characters recommended)
- **Output**: 256-bit authenticated hash
- **Purpose**: Prevent token tampering and pre-computed rainbow tables

**Formula:**
```
HMAC-SHA256(message, secret_key) = SHA256((key ⊕ opad) || SHA256((key ⊕ ipad) || message))
```

**Properties:**
- Same message + different key = completely different hash
- Prevents attackers from pre-computing all possible token values (rainbow table attacks)
- Required for production use

### AES-256 Encryption (Advanced Encryption Standard)

- **Standard**: FIPS 197
- **Key Size**: 256-bit (32-byte)
- **Mode**: CBC (Cipher Block Chaining) with PKCS5/PKCS7 padding
- **Purpose**: Encrypt tokens to prevent re-identification without key

**Process:**
1. HMAC-SHA256 hash is encrypted
2. Initialization vector (IV) is randomly generated per token
3. AES-256-CBC is applied
4. Result is base64 encoded for storage

**Security Properties:**
- **Reversibility**: Only with encryption key; no backdoor
- **Randomness**: Different IV per token prevents pattern detection
- **Strength**: 256-bit key provides ~2^256 possible keys (computationally infeasible to brute-force)

---

## Key Management & Secrets

This section provides practical guidance for managing the cryptographic secrets OpenToken requires.

### Types of Secrets

OpenToken expects **two secrets** (one required, one optional depending on mode):

| Secret             | CLI Flag                 | Purpose                                   | Requirements                                                |
| ------------------ | ------------------------ | ----------------------------------------- | ----------------------------------------------------------- |
| **Hashing Secret** | `-h` / `--hashingsecret` | HMAC-SHA256 key for deterministic hashing | Required in all modes; 8+ characters recommended, 16+ ideal |
| **Encryption Key** | `-e` / `--encryptionkey` | AES-256-GCM symmetric key                 | Required for encryption mode; **exactly 32 characters**     |

**Hash-only mode** (`--hash-only`) skips AES encryption; only the hashing secret is needed.

### Handling Secrets in Practice

#### Development / Local Testing

Use clearly marked placeholder values:

```bash
# Placeholder secrets for local testing only
java -jar opentoken-cli-*.jar \
  -i sample.csv -t csv -o output.csv \
  -h "HashingKey" \
  -e "Secret-Encryption-Key-Goes-Here."
```

Store these in a local `.env` file (not committed):

```bash
# .env (add to .gitignore)
OPENTOKEN_HASHING_SECRET=HashingKey
OPENTOKEN_ENCRYPTION_KEY=Secret-Encryption-Key-Goes-Here.
```

Load and use:

```bash
source .env
java -jar opentoken-cli-*.jar \
  -i sample.csv -t csv -o output.csv \
  -h "$OPENTOKEN_HASHING_SECRET" \
  -e "$OPENTOKEN_ENCRYPTION_KEY"
```

#### Production

Store secrets in a managed secret store and inject via environment variables at runtime:

| Platform   | Secret Store       | Injection Method                                            |
| ---------- | ------------------ | ----------------------------------------------------------- |
| AWS        | Secrets Manager    | `aws secretsmanager get-secret-value` or ECS/Lambda secrets |
| Azure      | Key Vault          | `az keyvault secret show` or App Service key references     |
| GCP        | Secret Manager     | `gcloud secrets versions access` or workload identity       |
| On-prem    | HashiCorp Vault    | `vault kv get` or agent auto-auth                           |
| Databricks | Databricks Secrets | `dbutils.secrets.get("scope", "key")`                       |

**Example (AWS Secrets Manager):**

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

**Example (Databricks):**

```python
from opentoken_pyspark import SparkPersonTokenProcessor

processor = SparkPersonTokenProcessor(
    spark=spark,
    hashing_secret=dbutils.secrets.get("opentoken", "hashing_secret"),
    encryption_key=dbutils.secrets.get("opentoken", "encryption_key")
)
```

### Secret Rotation

1. **Generate new secrets** – use a cryptographically secure generator.
2. **Re-run token generation** – tokens are deterministic; same input + same secrets = same tokens. New secrets = new tokens.
3. **Version secrets in your store** – keep old versions for auditability.
4. **Coordinate downstream** – any system that decrypts tokens needs the matching encryption key.

### What NOT to Do

- **Never commit secrets to source control.** Add `.env` and similar files to `.gitignore`.
- **Never log secrets.** CLI output and metadata files contain hashes of secrets, not the secrets themselves.
- **Never hard-code secrets in scripts checked into git.** Use environment variables or secret-store references.

### Secret Verification via Metadata

Each run produces a `.metadata.json` with SHA-256 hashes of secrets:

```json
{
  "HashingSecretHash": "e0b4e60b...",
  "EncryptionSecretHash": "a1b2c3d4..."
}
```

Use [tools/hash_calculator.py](https://github.com/TruvetaPublic/OpenToken/blob/main/tools/hash_calculator.py) to verify:

```bash
python tools/hash_calculator.py \
  --hashing-secret "YourSecret" \
  --encryption-key "YourEncryptionKey"
# Compare output hashes to metadata file
```

### Cross-References

- **CLI flags for secrets**: [CLI Reference](../reference/cli.md)
- **Environment variable usage**: [Configuration](../config/configuration.md#environment-variables)
- **Databricks / Spark secrets**: [Spark or Databricks](../operations/spark-or-databricks.md)
- **Running the CLI**: [Running OpenToken](../running-opentoken/index.md)
- **Metadata format (hash fields)**: [Reference: Metadata Format](../reference/metadata-format.md)

---

## Privacy Best Practices

### Token Handling

- **Do not log raw tokens**: Logs may be stored, indexed, or shared
- **Do not output raw person data**: Always use tokens for matching
- **Encrypt output files**: Store token files with encryption at rest (AWS S3 encryption, TDE, etc.)
- **Audit access**: Track who generates, accesses, or decrypts tokens

### Data Minimization

- **Only process required attributes**: FirstName, LastName, BirthDate, Sex, PostalCode, SSN
- **Do not retain raw data**: Delete input files after token generation
- **Use tokens for matching**: Match on encrypted tokens, not raw person data

---

## Preventing Re-Identification

### Token Uniqueness

Tokens are derived from deterministic person attributes, ensuring:

- **Same person → Same token** (enables matching)
- **Different person → Different token** (in most cases; collisions are rare due to SHA-256)

**Example:**
```
Person A: John Doe, 1980-01-15, Male, 98004
  T1: DOE|J|MALE|1980-01-15 → [Token X]

Person B: John Doe, 1980-01-15, Male, 98005  (different ZIP)
  T1: DOE|J|MALE|1980-01-15 → [Token X]  (same token, different ZIP)
  T2: DOE|JOHN|1980-01-15|980 → [Token Y]  (different token, different ZIP)
```

### Encryption Protects Against Analysis

Without the encryption key, tokens are **non-invertible**:

- ✓ Attacker has encrypted tokens → Cannot recover original data
- ✓ Attacker has hashing secret → Still cannot recover original data without encryption key
- ✓ Attacker has both secrets → Can regenerate tokens from known person data (need raw data)

**Conclusion:** Encryption key is the primary security boundary. Protect it carefully.

### Metadata Hash Verification

Verify that metadata hashes match your secrets to detect tampering:

```bash
# Calculate hashes for your secrets
python tools/hash_calculator.py --hashing-secret "HashingKey" --encryption-key "Secret-Encryption-Key"

# Compare with metadata file
cat output.metadata.json | grep -E "HashingSecretHash|EncryptionSecretHash"
```

---

## Compliance Considerations

### HIPAA

OpenToken aligns with HIPAA Privacy Rule requirements:

- **De-identification through hashing**: Tokens do not constitute direct identifiers
- **Encryption at rest**: AES-256 encryption protects token data
- **Audit trail**: Metadata logs processing events
- **Data minimization**: No raw PHI retained after token generation

**Recommended safeguards:**
- Store tokens in encrypted file systems
- Limit access to tokens and secrets
- Maintain audit logs of token generation and access

### GDPR (Right to be Forgotten)

Tokens are deterministic and cannot be anonymized without losing matching capability. Consider:

- **Token retention**: Tokens are de-identified but tied to record IDs
- **Data deletion**: Remove raw person data after token generation
- **Pseudonymization**: Use record IDs instead of names for ongoing tracking

---

## Common Security Mistakes

| Mistake                                         | Risk                         | Solution                                       |
| ----------------------------------------------- | ---------------------------- | ---------------------------------------------- |
| Storing plain text secrets in code              | Exposure in source control   | Use environment variables or vault             |
| Using weak secrets (< 8 chars)                  | Brute-force vulnerability    | Use 16+ chars, mix upper/lower/digits/symbols  |
| Not rotating secrets                            | Compromise detection delayed | Rotate periodically; maintain secret versions  |
| Logging raw tokens                              | Exposure in logs             | Log only high-level statistics                 |
| Sharing encrypted tokens without key protection | Decryption possible          | Encrypt tokens at rest; protect encryption key |
| Using placeholder values in data                | False matches possible       | Validate and clean input data                  |

---

## Next Steps

- **View validation rules in detail**: [Reference](../reference/index.md)
- **Understand token generation**: [Concepts: Token Rules](../concepts/token-rules.md)
- **Verify metadata hashes**: [Reference: Metadata Format](../reference/metadata-format.md)
- **Configure secrets safely**: [Running OpenToken: CLI Guide](../running-opentoken/index.md#cli-guide)
- **Share tokens across organizations**: [Sharing Tokenized Data](../operations/sharing-tokenized-data.md)
