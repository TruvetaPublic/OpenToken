---
layout: default
---

# Sharing Tokenized Data Between Organizations

How to securely exchange OpenToken outputs for cross-organization person matching.

---

## Overview

Organizations often need to identify overlapping individuals across datasets without exposing raw person data. OpenToken enables this by generating deterministic, cryptographically secure tokens that can be shared and matched externally.

**Typical scenario:**

1. Organization A and Organization B each hold patient records
2. The intended **receiver** shares a public key with the sender
3. The sender runs OpenToken using ECDH key exchange (sender private key + receiver public key)
4. The sender transfers a token package (no raw PII, no private keys)
5. The receiver decrypts to hash-only form and performs matching

```text
┌─────────────────┐                    ┌─────────────────┐
│  Organization A │                    │  Organization B │
│  (Patient Data) │                    │  (Patient Data) │
└────────┬────────┘                    └────────┬────────┘
         │                                      │
         ▼                                      ▼
┌─────────────────┐                    ┌─────────────────┐
│   OpenToken     │                    │   OpenToken     │
│ (ECDH exchange) │                    │ (ECDH exchange) │
└────────┬────────┘                    └────────┬────────┘
         │                                      │
         ▼                                      ▼
┌─────────────────┐     Exchange       ┌─────────────────┐
│  Tokens + Meta  │ ←───────────────→  │  Tokens + Meta  │
└────────┬────────┘                    └────────┬────────┘
         │                                      │
         └───────────────┬──────────────────────┘
                         ▼
               ┌─────────────────┐
               │  Token Matching │
               │  (find overlap) │
               └─────────────────┘
```

---

## Sender Workflow

The sending organization prepares tokenized data for sharing.

### Step 1: Obtain Receiver Public Key

The receiver generates a keypair and shares only their public key (`public_key.pem`) with the sender.

### Step 2: Generate (or Reuse) Sender Keypair

```bash
opentoken generate-keypair --output-dir ./keys/sender --ecdh-curve P-384
```

### Step 3: Tokenize for the Receiver

Generate a token package (commonly a `.zip`) using ECDH key exchange.

```bash
opentoken tokenize \
  -i patient_data.csv -t csv -o tokens_for_partner.zip \
  --receiver-public-key /path/to/receiver/public_key.pem \
  --sender-keypair-path ./keys/sender/keypair.pem \
  --ecdh-curve P-384
```

**Hash-only mode (internal helper):**

```bash
opentoken tokenize \
  --hash-only \
  -i local_patient_data.csv -t csv -o local_hash_only.zip \
  --receiver-public-key /path/to/receiver/public_key.pem \
  --sender-keypair-path ./keys/sender/keypair.pem
```

Typical pattern:

1. Both parties exchange **encrypted tokens** only.
2. You decrypt the partner's encrypted tokens to the hash-only equivalent (see [Decrypting Tokens](decrypting-tokens.md)).
3. You generate **hash-only tokens** for your own dataset.
4. You perform overlap analysis by joining the two hash-only datasets.

See [Hash-Only Mode](hash-only-mode.md) for trade-offs between encrypted and hash-only tokens.

### Step 4: Review Metadata

Check the generated `.metadata.json` for processing statistics:

```json
{
  "KeyExchangeMethod": "ECDH-P-384",
  "Curve": "P-384",
  "SenderPublicKeyHash": "a85b4bd6...",
  "ReceiverPublicKeyHash": "32bc0e98...",
  "TotalRows": 50000,
  "TotalRowsWithInvalidAttributes": 120,
  "InvalidAttributesByType": {
    "SocialSecurityNumber": 80,
    "BirthDate": 40
  },
  "BlankTokensByRule": {
    "T1": 80,
    "T3": 80
  }
}
```

**Key checks:**

- `TotalRowsWithInvalidAttributes`: High counts may indicate data quality issues
- `BlankTokensByRule`: T1 and T3 require SSN; blanks are expected if SSN is often missing
- `SenderPublicKeyHash` / `ReceiverPublicKeyHash`: Share these hashes so the receiver can confirm the expected keys were used

### Step 5: Prepare Transfer Package

Include in the transfer:

| File                       | Purpose                                               | Contains Private Keys? |
| -------------------------- | ----------------------------------------------------- | ---------------------- |
| `tokens_for_partner.zip`   | Token package (tokens + metadata + sender public key) | No                     |
| Data dictionary (optional) | Column definitions, RecordId mapping                  | No                     |

**Do NOT include:**

- Raw input data (PII)
- Any private keys (`keypair.pem`)
- Decrypted tokens

### Step 5: Transfer Securely

Use encrypted file transfer:

- SFTP with encryption
- Cloud storage with encryption at rest (S3, Azure Blob, GCS)
- Secure email with encrypted attachment

---

## Recipient Workflow

The receiving organization ingests shared tokens and matches against their own data.

### Step 1: Generate Receiver Keypair

The receiver generates a keypair and shares their public key with the sender.

```bash
opentoken generate-keypair --output-dir ./keys/receiver --ecdh-curve P-384
```

### Step 2: Verify Public Key Hashes (Optional but Recommended)

Compare the metadata hashes to your local public key file hashes:

```bash
sha256sum ./keys/receiver/public_key.pem
```

### Step 3: Decrypt Received Token Package

```bash
opentoken decrypt \
  -i tokens_for_partner.zip -t csv -o partner_decrypted.csv \
  --receiver-keypair-path ./keys/receiver/keypair.pem \
  --ecdh-curve P-384
```

### Step 4: Match Tokens

Join token files to find matching records:

```sql
-- Find overlapping patients
SELECT
    partner.RecordId AS PartnerRecordId,
    local.RecordId AS LocalRecordId,
    partner.RuleId
FROM partner_tokens partner
JOIN local_tokens local
    ON partner.Token = local.Token
    AND partner.RuleId = local.RuleId;
```

**Interpretation:**

- **T4 match**: Very high confidence (SSN + Sex + BirthDate)
- **T3 match**: High confidence (Last name + full first name + Sex + BirthDate)
- **Multiple rule matches**: Stronger confidence (more agreement across attributes)

See [Matching Model](../concepts/matching-model.md) for matching strategies.

### Step 5: Handle Encrypted Tokens (If Applicable)

Decryption is the normal step for matching workflows because encrypted tokens are randomized (due to AES-GCM IVs) and will not match by comparing ciphertext blobs directly.

See [Decrypting Tokens](decrypting-tokens.md) for details.

---

## Security Considerations

### Use Encrypted Tokens for External Sharing

Encrypted mode adds AES-256-GCM encryption on top of HMAC-SHA256:

| Mode      | External Sharing   | Defense in Depth | Reversible              |
| --------- | ------------------ | ---------------- | ----------------------- |
| Encrypted | ✓ Recommended      | Yes              | To HMAC hash (with key) |
| Hash-only | ⚠ Use with caution | No               | Not reversible          |

Encrypted tokens provide an additional security layer if token files are intercepted.

### Protect Private Keys

- Never send private keys with token files.
- Store private keys in a vault / secret store (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault).
- Limit access to private keys to authorized personnel only.
- Rotate keypairs periodically and coordinate rollovers with partners.

### Verify Partner Identity

Before sharing:

- Confirm the partner organization's identity through established business relationships
- Use signed data-sharing agreements
- Verify contact information through independent channels

### Audit and Logging

- **Log token generation events**: Who ran OpenToken, when, with which input file
- **Log token transfers**: When tokens were sent/received, to/from whom
- **Log matching events**: Who performed matching, what results were produced

### Minimize Data Exposure

- **Share only tokens**: Never share raw input data alongside tokens
- **Limit token rules if appropriate**: If only T2 matching is needed, consider generating only T2 tokens
- **Use RecordId mappings**: Map internal IDs to opaque identifiers before sharing

### What Tokens Do NOT Reveal

Tokens are one-way transformations. Without the required private keys:

- Attackers cannot reverse encrypted tokens to hash-only form
- Attackers cannot reverse hash-only tokens to original attributes
- Attackers cannot determine which attributes produced a token

With only the token file, an attacker cannot identify individuals.

---

## Common Pitfalls

| Issue                         | Cause                                             | Solution                                                                |
| ----------------------------- | ------------------------------------------------- | ----------------------------------------------------------------------- |
| Decrypt fails                 | Wrong receiver keypair or wrong sender public key | Verify key paths and compare public key hashes in metadata              |
| Zero matches between datasets | Different versions or different key material      | Ensure both sides use the same OpenToken version and key exchange curve |
| Partial matches only          | Normalization differences                         | Ensure both parties use the same OpenToken version                      |
| High invalid record counts    | Data quality issues                               | Clean data before tokenization; review validation rules                 |
| Private keys exposed in logs  | Logging misconfiguration                          | Configure logging to exclude sensitive parameters                       |
| Token file intercepted        | Insecure transfer                                 | Use encrypted file transfer; prefer encrypted tokens                    |

---

## Checklist: Before Sharing

**Sender:**

- [ ] Obtained receiver public key
- [ ] Generated tokens with correct receiver public key and sender keypair
- [ ] Verified metadata shows expected row counts
- [ ] Prepared transfer package (tokens + metadata only)
- [ ] Using encrypted file transfer

**Recipient:**

- [ ] Generated receiver keypair and shared receiver public key
- [ ] Verified public key hashes match metadata (recommended)
- [ ] Decrypted partner token package using receiver private key
- [ ] Matching logic uses correct RuleId and Token columns

---

## Related Documentation

- [Hash-Only Mode](hash-only-mode.md) — When to skip encryption
- [Decrypting Tokens](decrypting-tokens.md) — Reversing encrypted tokens for verification
- [Security](../security.md) — Cryptographic details and key management
- [Key Management](../security.md#key-management-ecdh-public-key-exchange) — Private key handling best practices
- [Configuration](../config/configuration.md) — CLI arguments and environment variables
- [Matching Model](../concepts/matching-model.md) — How token matching works
