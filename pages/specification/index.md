---
layout: default
---

# Specification

Complete technical specification for token generation, metadata format, and data structures.

## Token Generation Rules

OpenToken generates tokens using 5 distinct rules (T1–T5) that combine person attributes in specific ways.

### Rule Definitions

| Rule ID | Definition                                              | Attributes                                      | Purpose                  |
| ------- | ------------------------------------------------------- | ----------------------------------------------- | ------------------------ |
| **T1**  | `U(LastName)\|U(FirstName[0])\|U(Sex)\|BirthDate`       | Last name + first initial + sex + birthdate     | Standard matching        |
| **T2**  | `U(LastName)\|U(FirstName)\|BirthDate\|PostalCode[0:3]` | Last name + full first name + birthdate + ZIP-3 | Geographic variations    |
| **T3**  | `U(LastName)\|U(FirstName)\|U(Sex)\|BirthDate`          | Last name + full first name + sex + birthdate   | Flexible name matching   |
| **T4**  | `SocialSecurityNumber\|U(Sex)\|BirthDate`               | Full SSN + sex + birthdate                      | Authoritative identifier |
| **T5**  | `U(LastName)\|U(FirstName[0:3])\|U(Sex)`                | Last name + first 3 letters + sex               | Quick search             |

**Notation:**
- `U(X)` = Uppercase(X)
- `[0]` = First character
- `[0:3]` = First 3 characters
- `\|` = Pipe separator (used in token signature, not in actual output)

### Token Signature Generation

A token signature is the combination of normalized attributes per rule. Signatures are then hashed and encrypted to produce final tokens.

**Example for person: John Doe, 1980-01-15, Male, 98004, 123456789**

| Rule | Signature                     | Purpose               |
| ---- | ----------------------------- | --------------------- |
| T1   | `DOE\|J\|MALE\|1980-01-15`    | High confidence match |
| T2   | `DOE\|JOHN\|1980-01-15\|980`  | Geographic match      |
| T3   | `DOE\|JOHN\|MALE\|1980-01-15` | Flexible match        |
| T4   | `123456789\|MALE\|1980-01-15` | Authoritative match   |
| T5   | `DOE\|JOH\|MALE`              | Quick search match    |

### Token Encryption

Each token signature is transformed through the pipeline:

```
Token Signature
  ↓
SHA-256 Hash (one-way digest)
  ↓
HMAC-SHA256(hash, hashing_secret) (authenticated hash)
  ↓
AES-256-CBC Encrypt (encrypted hash)
  ↓
Base64 Encode (storable format)
```

**Pseudocode:**
```python
def generate_token(signature, hashing_secret, encryption_key):
    hash_digest = SHA256(signature)
    hmac = HMAC_SHA256(hash_digest, hashing_secret)
    iv = random_bytes(16)  # Initialization vector
    encrypted = AES_256_CBC_encrypt(hmac, encryption_key, iv)
    token = base64_encode(iv || encrypted)  # IV prepended
    return token
```

### Hash-Only Mode

Alternative token generation without encryption:

```
Token Signature
  ↓
SHA-256 Hash
  ↓
HMAC-SHA256(hash, hashing_secret)
  ↓
Base64 Encode
```

**Pseudocode:**
```python
def generate_token_hash_only(signature, hashing_secret):
    hash_digest = SHA256(signature)
    hmac = HMAC_SHA256(hash_digest, hashing_secret)
    token = base64_encode(hmac)
    return token
```

---

## Metadata Format

Metadata files provide processing statistics, system information, and secure hashes of secrets for audit and verification.

Every token generation run produces a `.metadata.json` file containing:
- Processing counts (total records, invalid attributes, blank tokens)
- Platform/version information
- SHA-256 hashes of secrets (not the secrets themselves)
- Input/output paths and timestamp

**Example metadata:**
```json
{
  "Platform": "Java",
  "JavaVersion": "21.0.0",
  "OpenTokenVersion": "1.7.0",
  "TotalRows": 101,
  "TotalRowsWithInvalidAttributes": 9,
  "InvalidAttributesByType": {
    "SocialSecurityNumber": 2,
    "FirstName": 1,
    "BirthDate": 3
  },
  "BlankTokensByRule": {
    "T1": 5,
    "T2": 12
  },
  "ProcessingTimestamp": "2024-01-15T10:30:45Z",
  "HashingSecretHash": "e0b4e60b...",
  "EncryptionSecretHash": "a1b2c3d4..."
}
```

**For complete field descriptions, examples, and hash verification:**  
→ See [Reference: Metadata Format](../reference/metadata-format.md)

---

## Output File Format

### Tokens Output (CSV)

```csv
RecordId,RuleId,Token
ID001,T1,Gn7t1Zj16E5Qy+z9iINtczP6fRDYta6C0XFrQtpjnVQSEZ5pQXAzo02Aa9LS9oNMOog6Ssw9GZE6fvJrX2sQ/cThSkB6m91L
ID001,T2,pUxPgYL9+cMxkA+8928Pil+9W+dm9kISwHYPdkZS+I2nQ/bQ/8HyL3FOVf3NYPW5NKZZO1OZfsz7LfKYpTlaxyzMLqMF2Wk7
ID001,T3,rwjfwIo5OcJUItTx8KCoSZMtr7tVGSyXsWv/hhCWmD2pBO5JyfmujsosvwYbYeeQ4Vl1Z3eq0cTwzkvfzJVS/EKaRhtjMZz5
ID001,T4,9o7HIYZkhizczFzJL1HFyanlllzSa8hlgQWQ5gHp3Niuo2AvEGcUwtKZXChzHmAa8Jm3183XVoacbL/bFEJyOYYS4EQDppev
ID001,T5,QpBpGBqaMhagfcHGZhVavn23ko03jkyS9Vo4qe78E4sKw+Zq2CIw4MMWG8VXVwInnsFBVk6NSDUI79wECf5DchV5CXQ9AFqR
ID002,T1,...
```

**Rows per record:** 5 (one per rule)
**Columns:**
- `RecordId`: Input record ID (or auto-generated UUID)
- `RuleId`: Token rule (T1–T5)
- `Token`: Base64-encoded encrypted/hashed token

### Tokens Output (Parquet)

Parquet format contains the same schema as CSV but with native data types and compression.

```python
# Schema
RecordId: string
RuleId: string
Token: string
```

---

## Attribute Normalization

All attributes are normalized before token generation to ensure consistent tokens across datasets.

### FirstName Normalization

| Step | Process                                              | Example                         |
| ---- | ---------------------------------------------------- | ------------------------------- |
| 1    | Remove titles (Dr., Mr., Mrs., Ms., Prof.)           | "Dr. John Smith" → "John Smith" |
| 2    | Remove middle initials                               | "John J" → "John"               |
| 3    | Remove generational suffixes (Jr., Sr., II, III, IV) | "John Jr." → "John"             |
| 4    | Remove trailing periods                              | "John J." → "John"              |
| 5    | Remove non-alphabetic characters                     | "John-Marie" → "John Marie"     |
| 6    | Normalize diacritics (é→e, ñ→n, ü→u)                 | "José" → "Jose"                 |
| 7    | Uppercase for token generation                       | "john" → "JOHN"                 |

### LastName Normalization

| Step | Process                          | Example                |
| ---- | -------------------------------- | ---------------------- |
| 1    | Remove generational suffixes     | "Warner IV" → "Warner" |
| 2    | Remove non-alphabetic characters | "O'Brien" → "OBrien"   |
| 3    | Normalize diacritics             | "García" → "Garcia"    |
| 4    | Uppercase for token generation   | "garcia" → "GARCIA"    |

### BirthDate Normalization

| Input Format | Normalized | Example                     |
| ------------ | ---------- | --------------------------- |
| YYYY-MM-DD   | YYYY-MM-DD | "1980-01-15" → "1980-01-15" |
| MM/DD/YYYY   | YYYY-MM-DD | "01/15/1980" → "1980-01-15" |
| MM-DD-YYYY   | YYYY-MM-DD | "01-15-1980" → "1980-01-15" |
| DD.MM.YYYY   | YYYY-MM-DD | "15.01.1980" → "1980-01-15" |

### Sex Normalization

| Input              | Normalized |
| ------------------ | ---------- |
| "Male", "M", "m"   | "MALE"     |
| "Female", "F", "f" | "FEMALE"   |

### PostalCode Normalization

**US ZIP Codes:**

| Input        | Normalized | Reason                               |
| ------------ | ---------- | ------------------------------------ |
| "98004"      | "98004"    | 5-digit format                       |
| "9800"       | "98000"    | 4-digit auto-padded to 5             |
| "980"        | "98000"    | ZIP-3 auto-padded to 5               |
| "98004-1234" | "98004"    | Dash removed for T1–T3; first 5 used |

**Canadian Postal Codes:**

| Input     | Normalized | Format                 |
| --------- | ---------- | ---------------------- |
| "K1A1A1"  | "K1A 1A1"  | Space added            |
| "K1A 1A1" | "K1A 1A1"  | Already formatted      |
| "K1A1"    | "K1A 100"  | Auto-padded with zeros |
| "K1A"     | "K1A 000"  | Auto-padded with zeros |

### SSN Normalization

| Input         | Normalized  |
| ------------- | ----------- |
| "123-45-6789" | "123456789" |
| "123456789"   | "123456789" |

---

## Data Types & Constraints

### RecordId (Optional)

- **Type**: String (UTF-8)
- **Constraints**: Unique per input (recommended)
- **Default**: Auto-generated UUID if omitted
- **Max Length**: Unlimited (practical limit: 1000 characters)

### FirstName, LastName (Required)

- **Type**: String (UTF-8)
- **Constraints**: After normalization, at least one alphabetic character
- **Max Length**: Unlimited (practical limit: 200 characters)

### BirthDate (Required)

- **Type**: Date (YYYY-MM-DD)
- **Constraints**: 1910-01-01 to today
- **Format**: ISO 8601 (YYYY-MM-DD)

### Sex (Required)

- **Type**: Enum (Male | Female)
- **Constraints**: Exact values after normalization
- **Case**: Input is case-insensitive

### PostalCode (Required)

- **Type**: String
- **Constraints**: Valid US ZIP or Canadian postal code
- **Max Length**: 10 characters (after normalization)

### SSN (Required)

- **Type**: String (numeric)
- **Constraints**: 9 digits, no non-numeric characters allowed
- **Format**: DDD-DD-DDDD or DDDDDDDDD

### Token (Output)

- **Type**: String (base64)
- **Length**: ~80–100 characters (encrypted); ~50–80 characters (hash-only)
- **Format**: Base64 encoded; contains ASCII printable characters + +, /, =

---

## Error Handling

### Invalid Attribute Behavior

When an attribute fails validation:

1. **Record is marked invalid** in metadata
2. **Tokens still generated** for that record (with blank tokens for affected rules)
3. **Metadata tracks the error** in `InvalidAttributesByType`

**Example:** Record with invalid BirthDate:
- T1, T3, T4 will have **blank tokens** (use BirthDate)
- T2, T5 will have **valid tokens** (don't use BirthDate... wait, T5 doesn't use it)

Actually, let me reconsider: all rules use BirthDate except T5.

- T1, T2, T3, T4 will have **blank tokens**
- T5 will have **valid token**

### Processing Failure Modes

| Scenario                          | Behavior                                          | Metadata                |
| --------------------------------- | ------------------------------------------------- | ----------------------- |
| **Unsupported file format**       | Processing fails; no output                       | Error logged to stderr  |
| **Missing required column**       | Processing fails; no output                       | Error logged to stderr  |
| **Corrupted input file**          | Processing stops at error; partial output written | Error count in metadata |
| **Output file exists**            | Overwrites by default (add safety flag if needed) | New metadata generated  |
| **Invalid secret (empty string)** | Processing may succeed but tokens may be invalid  | Warning logged          |

---

## Next Steps

- **Understand token generation**: [Concepts: Token Rules](../concepts/token-rules.md)
- **View validation rules**: [Security](../security.md)
- **Configure inputs/outputs**: [Configuration](../config/configuration.md)
- **Verify metadata hashes**: [Reference: Metadata Format](../reference/metadata-format.md)
