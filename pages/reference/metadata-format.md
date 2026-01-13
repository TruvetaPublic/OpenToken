---
layout: default
---

# Metadata Format

Complete reference for the OpenToken metadata JSON file structure, fields, and usage for audit and verification.

## Overview

OpenToken generates a metadata file alongside every token output file. Metadata files provide:

- **Processing statistics**: Counts of total records, invalid attributes, and blank tokens
- **System information**: Platform (Java/Python), runtime version, library version
- **Key fingerprints**: SHA-256 hashes of public keys used for key exchange
- **Audit trail**: What was processed and how (platform, version, and validation statistics)

Metadata files:
- Always use JSON format with `.metadata.json` extension
- Are automatically generated (e.g., `output.csv` → `output.metadata.json`)
- Contain no raw person data or actual secrets

---

## Metadata Structure

### File Format

```
Filename: <output-file-name>.metadata.json
Format: JSON (UTF-8)
Extension: .metadata.json
```

**Example filenames:**
- `output.csv` → `output.metadata.json`
- `tokens.parquet` → `tokens.metadata.json`
- `/data/results.csv` → `/data/results.metadata.json`

### JSON Schema

```json
{
  "Platform": "string",
  "JavaVersion": "string (optional, Java only)",
  "PythonVersion": "string (optional, Python only)",
  "OpenTokenVersion": "string",
  "KeyExchangeMethod": "string (optional)",
  "Curve": "string (optional)",
  "SenderPublicKeyHash": "string (hex, optional)",
  "ReceiverPublicKeyHash": "string (hex, optional)",
  "TotalRows": integer,
  "TotalRowsWithInvalidAttributes": integer,
  "InvalidAttributesByType": {
    "AttributeName": integer,
    ...
  },
  "BlankTokensByRule": {
    "RuleId": integer,
    ...
  }
}
```

---

## Field Descriptions

### Platform Information

| Field              | Type   | Description                          | Example                |
| ------------------ | ------ | ------------------------------------ | ---------------------- |
| `Platform`         | String | Processing platform/language         | `"Java"` or `"Python"` |
| `JavaVersion`      | String | Java runtime version (Java only)     | `"21.0.0"`             |
| `PythonVersion`    | String | Python runtime version (Python only) | `"3.11.5"`             |
| `OpenTokenVersion` | String | OpenToken library version            | `"1.12.2"`             |

**Notes:**
- Only `JavaVersion` OR `PythonVersion` appears (not both)
- Platform value determines which version field is present

### Processing Statistics

| Field                            | Type    | Description                               | Example                            |
| -------------------------------- | ------- | ----------------------------------------- | ---------------------------------- |
| `TotalRows`                      | Integer | Total input records processed             | `101`                              |
| `TotalRowsWithInvalidAttributes` | Integer | Records with ≥1 invalid attribute         | `9`                                |
| `InvalidAttributesByType`        | Object  | Count of invalid values by attribute name | `{"FirstName": 1, "BirthDate": 3}` |
| `BlankTokensByRule`              | Object  | Count of blank tokens by rule ID          | `{"T1": 5, "T2": 12}`              |

**InvalidAttributesByType:**
- Keys: Attribute names (e.g., `FirstName`, `BirthDate`, `SocialSecurityNumber`)
- Values: Count of invalid occurrences across all records
- A single record with 2 invalid attributes contributes 2 to the sum
- Sum of counts ≥ `TotalRowsWithInvalidAttributes`

**BlankTokensByRule:**
- Keys: Rule IDs (`T1`, `T2`, `T3`, `T4`, `T5`)
- Values: Count of blank tokens for that rule
- Blank tokens occur when a rule requires an invalid attribute
- Example: Invalid `BirthDate` causes blank tokens for T1, T2, T3, T4 (but not T5)

### Key Exchange Fingerprints

| Field                   | Type   | Description                                     | Example         |
| ----------------------- | ------ | ----------------------------------------------- | --------------- |
| `KeyExchangeMethod`     | String | Key exchange method in use                      | `"ECDH-P-384"`  |
| `Curve`                 | String | Elliptic curve name                             | `"P-384"`       |
| `SenderPublicKeyHash`   | String | SHA-256 hash of sender public key bytes (hex)   | `"a85b4bd6..."` |
| `ReceiverPublicKeyHash` | String | SHA-256 hash of receiver public key bytes (hex) | `"32bc0e98..."` |

**Security:**
- Hashes are **not reversible** (SHA-256 is one-way)
- Used for verification: calculate hash of the public key file and compare to metadata
- These hashes are safe to share; they help confirm both parties used the expected keys

---

## Example Metadata

### Example (ECDH Key Exchange)

```json
{
  "Platform": "Java",
  "JavaVersion": "21.0.0",
  "OpenTokenVersion": "1.12.2",
  "KeyExchangeMethod": "ECDH-P-384",
  "Curve": "P-384",
  "SenderPublicKeyHash": "a85b4bd68f9e2b7546920572b3191fae6b35bbd18dcb4c8c2b1ef4aa448158a8",
  "ReceiverPublicKeyHash": "32bc0e98de8e04882ba7201a2041622b4e47d528c644118789ea0c089452d4f8",
  "TotalRows": 101,
  "TotalRowsWithInvalidAttributes": 9,
  "InvalidAttributesByType": {
    "SocialSecurityNumber": 2,
    "FirstName": 1,
    "PostalCode": 1,
    "LastName": 2,
    "BirthDate": 3
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

---

## Interpreting Metadata

### Valid vs Invalid Records

Calculate valid records:
```
Valid Records = TotalRows - TotalRowsWithInvalidAttributes
```

**Example:**
```json
{
  "TotalRows": 100,
  "TotalRowsWithInvalidAttributes": 5
}
```
- 100 records processed
- 5 records had errors
- 95 records were fully valid

### Invalid Attribute Counts

Count totals:
```
Sum of InvalidAttributesByType values ≥ TotalRowsWithInvalidAttributes
```

**Why ≥?** A single record can have multiple invalid attributes.

**Example:**
```json
{
  "TotalRows": 100,
  "TotalRowsWithInvalidAttributes": 5,
  "InvalidAttributesByType": {
    "FirstName": 2,
    "PostalCode": 3,
    "BirthDate": 1
  }
}
```
- Total invalid attribute instances: 2 + 3 + 1 = 6
- Records with errors: 5
- At least one record had 2+ invalid attributes

### Blank Token Analysis

Blank tokens occur when a rule requires an invalid attribute.

**Token rule dependencies:**
- **T1**: LastName, FirstName, Sex, BirthDate
- **T2**: LastName, FirstName, BirthDate, PostalCode
- **T3**: LastName, FirstName, Sex, BirthDate
- **T4**: SocialSecurityNumber, Sex, BirthDate
- **T5**: LastName, FirstName, Sex

**Example:**
```json
{
  "InvalidAttributesByType": {
    "BirthDate": 3
  },
  "BlankTokensByRule": {
    "T1": 3,
    "T2": 3,
    "T3": 3,
    "T4": 3,
    "T5": 0
  }
}
```
- 3 records had invalid BirthDate
- T1–T4 all use BirthDate → 3 blank tokens each
- T5 doesn't use BirthDate → 0 blank tokens

---

## Key Verification

### Purpose

Verify that the public keys used for token generation match expected values.

### Verification Process

1. **Calculate hash of the public key file**:
   ```bash
   sha256sum ./keys/receiver/public_key.pem
   sha256sum ./keys/sender/public_key.pem
   ```

2. **Compare to metadata**:
   ```bash
   cat output.metadata.json | grep ReceiverPublicKeyHash
   cat output.metadata.json | grep SenderPublicKeyHash
   ```

3. **Match = correct keys used**

---

## Usage Notes

### Audit Trail

Metadata provides an audit record of:
- What was processed (record counts and attribute-level statistics)
- When it was processed (inferred from surrounding system logs or job metadata)
- How it was processed (platform, version)
- What key material was used (via public key hashes)
- What errors occurred (invalid attributes)

Store metadata files alongside token outputs for compliance and troubleshooting.

### Cross-Language Consistency

Both Java and Python implementations produce identical metadata structure. Only differences:
- `JavaVersion` vs `PythonVersion` field name
- Timestamp format may vary slightly (both ISO 8601 compliant)

### Retention

Consider retaining metadata longer than token files:
- Metadata contains no person data
- Provides audit trail for compliance
- Useful for troubleshooting historic runs

### Security

Metadata files contain SHA-256 hashes of public keys:
- ✓ Safe to log, store, and share (no private keys exposed)
- ✓ Enables verification without revealing private keys
- ✗ Cannot reverse hashes to recover private keys
- ✗ Attacker with metadata alone cannot decrypt tokens

---

## Next Steps

- **View token rules**: [Concepts: Token Rules](../concepts/token-rules.md)
- **Understand validation**: [Security](../security.md)
- **Verify key hashes**: compare `sha256sum` output with metadata fields
- **See full examples**: [Quickstarts](../quickstarts/index.md)
