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

## Validation Rules

All person attributes are validated before token generation. Invalid records are marked and tracked in metadata.

### FirstName Validation

| Requirement | Reason | Examples |
|-------------|--------|----------|
| Not null/empty | Must identify the person | ❌ "" or NULL |
| Not a placeholder | Distinguishes real names from data quality issues | ❌ "Unknown", "Test", "Patient" |
| At least one alphabetic character | Must contain actual name | ❌ "123", "---" |

**Placeholder Values Rejected:**
"Unknown", "Test", "NotAvailable", "Patient", "Sample", "Anonymous", "Missing", "N/A", "TBD"

### LastName Validation

| Requirement | Reason | Examples |
|-------------|--------|----------|
| Not null/empty | Must identify the person | ❌ "" or NULL |
| At least 2 characters (with exceptions) | Ensures sufficient match discrimination | ❌ "A", "Z" |
| Exception: "Ng" accepted | Common single-letter surname | ✓ "Ng" |
| For 2-char names: At least one vowel OR "Ng" | Reduces false matches on common letters | ❌ "AA", ✓ "An", "Ng" |
| Not a placeholder | Distinguishes real names | ❌ "Unknown", "Test" |

**Validation Examples:**
```
Valid: "Smith", "O'Brien", "García", "Ng", "Lee"
Invalid: "A", "AA", "Zz", "Unknown", "Test"
```

### BirthDate Validation

| Requirement | Reason | Examples |
|-------------|--------|----------|
| After 1910-01-01 | Eliminates implausible dates | ❌ 1900-01-01, ✓ 1950-01-01 |
| Not in the future | Validates data quality | ❌ 2025-01-01 (if today is 2024) |
| Valid calendar date | Must be a real date | ❌ 1980-02-30, ✓ 1980-02-29 (leap year) |

**Accepted Formats:**
- `YYYY-MM-DD` (recommended)
- `MM/DD/YYYY`
- `MM-DD-YYYY`
- `DD.MM.YYYY`

### Sex Validation

| Requirement | Reason | Examples |
|-------------|--------|----------|
| Must be "Male" or "Female" | Standard classification | ✓ "Male", "M", "Female", "F" |
| Case-insensitive | User input variations | ✓ "male", "FEMALE", "F" |

### PostalCode Validation

**US ZIP Codes:**

| Rule | Reason | Examples |
|------|--------|----------|
| Valid format: 5, 4, 3, or 9 digits | Standard USPS formats | ✓ "98004", "9800", "98004-1234" |
| Area codes 000, 555, 888 invalid for ZIP-3 | Reserved for testing | ❌ "000", "555", "888" |
| Not placeholder values | Data quality | ❌ "00000", "11111", "12345" |
| Auto-pad ZIP-3 to 5 digits | Normalization | "980" → "98000" |

**Canadian Postal Codes:**

| Rule | Reason | Examples |
|------|--------|----------|
| Format: `AdA dAd` (A=letter, d=digit) | Canada Post standard | ✓ "K1A 1A1", "M5V 3A8" |
| Valid inputs: 3–6 characters | Flexible input | ✓ "K1A", "K1A1", "K1A1A1", "K1A 1A1" |
| Auto-format with space | Normalization | "K1A1A1" → "K1A 1A1" |
| Not reserved/placeholder codes | Data quality | ❌ "K1A 1A1" (reserved), "X0X 0X0" (fictional) |

**Postal Code Examples:**
```
Valid US: "98004", "9800", "98004-1234"
Valid Canadian: "K1A 1A1", "M5V3A8", "V6B"
Invalid: "00000" (placeholder), "123" (too short), "ABCDE" (invalid format)
```

### SSN Validation

Social Security Numbers follow strict validation rules to prevent invalid or placeholder values.

| Validation | Reason | Valid | Invalid |
|------------|--------|-------|---------|
| **Area** (first 3 digits) | Government assigns valid ranges | 001–665, 667–899 | 000, 666, 900–999 |
| **Group** (middle 2 digits) | Never 00 | 01–99 | 00 |
| **Serial** (last 4 digits) | Never 0000 | 0001–9999 | 0000 |
| **Not common invalid sequences** | Reduce placeholder/test values | 123-45-6789 | 111-11-1111, 222-22-2222, etc. |

**Validation Examples:**
```
Valid: 123-45-6789, 987-65-4321, 555-12-3456
Invalid:
  000-00-0000 (area 000)
  666-00-0000 (area 666)
  123-00-0000 (group 00)
  123-45-0000 (serial 0000)
  111-11-1111 (common invalid sequence)
```

---

## Privacy Best Practices

### Secrets Management

**Hashing Secret & Encryption Key:**
- Store securely (e.g., AWS Secrets Manager, HashiCorp Vault, environment variables)
- Never commit to version control
- Rotate periodically (regenerate tokens with new secrets)
- Use strong, random keys (minimum 16 characters, mix of uppercase, lowercase, digits, symbols)

**In Test/Development:**
```
Hashing Secret: "HashingKey"
Encryption Key: "Secret-Encryption-Key-Goes-Here."
```

**In Production:**
```bash
# Use environment variables or secret management
export OPENTOKEN_HASHING_SECRET=$(aws secretsmanager get-secret-value --secret-id opentoken-hash-key --query SecretString --output text)
export OPENTOKEN_ENCRYPTION_KEY=$(aws secretsmanager get-secret-value --secret-id opentoken-enc-key --query SecretString --output text)

java -jar opentoken-cli-*.jar \
  -i data.csv -t csv -o tokens.csv \
  -h "$OPENTOKEN_HASHING_SECRET" \
  -e "$OPENTOKEN_ENCRYPTION_KEY"
```

### Token Handling

- **Do not log raw tokens**: Logs may be stored, indexed, or shared
- **Do not output raw person data**: Always use tokens for matching
- **Encrypt output files**: Store token files with encryption at rest (AWS S3 encryption, TDE, etc.)
- **Audit access**: Track who generates, accesses, or decrypts tokens

### Metadata Security

Metadata files contain **SHA-256 hashes of secrets**, not the secrets themselves:

```json
{
  "HashingSecretHash": "abc123...",  // SHA-256(hashing_secret), not the secret
  "EncryptionSecretHash": "def456..."  // SHA-256(encryption_key), not the key
}
```

**Hash verification:**
```bash
# Verify metadata hashes match your secrets
python tools/hash_calculator.py --hashing-secret "HashingKey" --encryption-key "Secret-Encryption-Key-Goes-Here."
```

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

| Mistake | Risk | Solution |
|---------|------|----------|
| Storing plain text secrets in code | Exposure in source control | Use environment variables or vault |
| Using weak secrets (< 8 chars) | Brute-force vulnerability | Use 16+ chars, mix upper/lower/digits/symbols |
| Not rotating secrets | Compromise detection delayed | Rotate periodically; maintain secret versions |
| Logging raw tokens | Exposure in logs | Log only high-level statistics |
| Sharing encrypted tokens without key protection | Decryption possible | Encrypt tokens at rest; protect encryption key |
| Using placeholder values in data | False matches possible | Validate and clean input data |

---

## Next Steps

- **View validation rules in detail**: [Reference](../reference/index.md)
- **Understand token generation**: [Concepts: Token Rules](../concepts/token-rules.md)
- **Verify metadata hashes**: [Reference: Metadata Format](../reference/metadata-format.md)
- **Configure secrets safely**: [Running OpenToken: CLI Guide](../running-opentoken/index.md#cli-guide)
