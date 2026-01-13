---
layout: default
---

# Metadata and Audit

Overview of the metadata produced alongside tokens and how to use it for auditing and verification.

## Overview

Every OpenToken run generates a metadata file (`.metadata.json`) alongside the token output. This metadata provides:

- Processing statistics (records processed, validation failures, blank tokens)
- System information (platform and version)
- Key verification (SHA-256 hashes of public keys, not private keys)
- Audit trail (platform, library version, and validation statistics)

## Key Concepts

### Processing Statistics

Metadata tracks:
- **Total records processed** (`TotalRows`)
- **Records with errors** (`TotalRowsWithInvalidAttributes`)
- **Invalid attributes by type** (`InvalidAttributesByType`)
- **Blank tokens by rule** (`BlankTokensByRule`)

**Why this matters:**
- Understand data quality issues
- Track which attributes fail validation most often
- Identify patterns in invalid data

### Key Verification

In ECDH key exchange mode, metadata includes **SHA-256 hashes of the public keys** used during tokenization/decryption:

- `SenderPublicKeyHash`
- `ReceiverPublicKeyHash`

**Purpose:**
- Verify correct keys were used without exposing private keys
- Create an audit trail for controlled partner exchange
- Detect configuration errors (wrong public key, wrong keypair)

You can verify these values locally:

```bash
sha256sum ./keys/sender/public_key.pem
sha256sum ./keys/receiver/public_key.pem
```

### Audit Trail

Metadata provides:
- What platform and version (`Platform`, `OpenTokenVersion`, and `JavaVersion`/`PythonVersion`)
- What data quality outcomes (record counts and attribute-level statistics)

**Use cases:**
- Compliance audits (who/when/where)
- Troubleshooting historic runs
- Version tracking for reproducibility

## Complete Reference

For full field descriptions, JSON schema, examples, and hash verification details:

→ **See [Reference: Metadata Format](../reference/metadata-format.md)**

## Next Steps

- **View metadata structure**: [Reference: Metadata Format](../reference/metadata-format.md)
- **Understand validation rules**: [Normalization & Validation](normalization-and-validation.md)
- **Verify key hashes**: compare `sha256sum` output with metadata fields
