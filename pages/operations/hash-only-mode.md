---
layout: default
---

# Hash-Only Mode

How to generate tokens without AES encryption.

---

## Overview

Hash-only mode generates deterministic tokens without AES encryption:

```
Token Signature → SHA-256 Hash → Keyed Hash (HMAC-SHA256) → Base64 Encode
```

Compared to encryption mode:

```
Token Signature → SHA-256 Hash → Keyed Hash (HMAC-SHA256) → AES-256-GCM Encrypt → Base64 Encode
```

---

## When to Use Hash-Only Mode

Hash-only mode is primarily used to support **overlap analysis workflows** where you receive **encrypted tokens from an external partner** and want to build an internal dataset that can be joined against those tokens.

**Use hash-only when:**

- You are creating an internal tokenized dataset that will be matched against **encrypted tokens received from an external partner** (after decrypting their tokens to the hash-only equivalent)
- You need faster processing or smaller token size for **internal analytics and overlap reporting**
- Raw data and tokens are already protected at rest within your environment

**Use encryption mode when:**

- Sharing tokens with external parties (encrypted tokens are the artifact that should be exchanged)
- Defense in depth is required for tokens stored outside your boundary
- Regulatory or contractual requirements mandate encryption of shared artifacts
- Tokens may be stored in less-secure systems or shared across multiple organizations

---

## CLI Usage

Use the `tokenize` command with `--hash-only`.

In key-exchange workflows, the hashing/encryption keys are derived via ECDH (sender private key + receiver public key), so you provide key *files* rather than shared symmetric secrets.

### Java

```bash
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  tokenize \
  --hash-only \
  -i ../../resources/sample.csv -t csv -o ../../resources/hashed-output.zip \
  --receiver-public-key ../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../resources/keys/sender/keypair.pem
```

### Python

```bash
python -m opentoken_cli.main \
  tokenize \
  --hash-only \
  -i ../../../resources/sample.csv -t csv -o ../../../resources/hashed-output.zip \
  --receiver-public-key ../../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../../resources/keys/sender/keypair.pem
```

### Docker

```bash
docker run --rm -v $(pwd)/resources:/app/resources \
  opentoken:latest \
  tokenize \
  --hash-only \
  -i /app/resources/sample.csv -t csv -o /app/resources/hashed-output.zip \
  --receiver-public-key /app/resources/keys/receiver/public_key.pem \
  --sender-keypair-path /app/resources/keys/sender/keypair.pem
```

---

## Output Comparison

### Encrypted Tokens (~80-100 characters)

```csv
RecordId,RuleId,Token
ID001,T1,Gn7t1Zj16E5Qy+z9iINtczP6fRDYta6C0XFrQtpjnVQSEZ5pQXAzo02Aa9LS9oNMOog6Ssw9GZE6fvJrX2sQ/cThSkB6m91L
```

### Hash-Only Tokens (~44-64 characters)

```csv
RecordId,RuleId,Token
ID001,T1,abc123def456ghi789jkl012mno345pqr678stu901vwx234
```

Hash-only tokens are shorter because they don't include the AES initialization vector (IV) and authentication tag.

---

## Metadata Differences

Hash-only runs still include key-exchange metadata (e.g., public key hashes) to support auditing and verification.

---

## Security Trade-offs

| Aspect                    | Encryption Mode            | Hash-Only Mode  |
| ------------------------- | -------------------------- | --------------- |
| **Token length**          | ~80-100 chars              | ~44-64 chars    |
| **Processing speed**      | Slower                     | Faster          |
| **Key material required** | ECDH key files             | ECDH key files  |
| **Reversibility**         | Decryptable (to HMAC hash) | Not decryptable |
| **External sharing**      | Recommended                | Not recommended |
| **Defense in depth**      | Yes                        | No              |

### Security Notes

- **Both modes are one-way**: Original attributes cannot be recovered from either token type
- **Same key exchange inputs = same tokens**: For a given curve + key material, hash-only tokens are deterministic
- **Cross-language parity**: Java and Python produce identical hash-only tokens for the same input

---

## Matching Hash-Only Tokens

Hash-only tokens can be matched directly without decryption when **both sides are in hash-only form**. In an external-partner workflow, this typically means:

1. Partner generates and shares **encrypted tokens**.
2. You run [Decrypting Tokens](decrypting-tokens.md) to convert the partner's encrypted tokens into their hash-only equivalent.
3. You generate hash-only tokens for your own dataset (your matching workflow must ensure both sides are in the same hash-only form).
4. You join the two hash-only datasets to measure overlap.

```sql
-- Match records between datasets
SELECT a.RecordId AS RecordA, b.RecordId AS RecordB
FROM tokens_a a
JOIN tokens_b b ON a.Token = b.Token AND a.RuleId = b.RuleId
WHERE a.RuleId = 'T1';
```

For encrypted tokens, decrypt before matching.

---

## Troubleshooting

### Tokens Don't Match Between Runs

**Cause:** Different key exchange inputs (curve, sender keypair, receiver public key).

**Solution:** Compare public key hashes in metadata to confirm both sides used the expected keys.

### Tokens Don't Match Between Java and Python

**Cause:** Attribute normalization differences or encoding issues.

**Solution:**
1. Verify secrets match exactly (including whitespace)
2. Run the interoperability test:
   ```bash
   cd tools/interoperability
   python java_python_interoperability_test.py
   ```
3. Compare normalized attributes (not raw input)

### "Encryption key not provided" Error

If you see legacy error messaging like this, ensure you're using the subcommand form (`tokenize`, `decrypt`) and check the [CLI Reference](../reference/cli.md).

---

## Next Steps

- **Encryption mode**: [Decrypting Tokens](decrypting-tokens.md)
- **Batch processing**: [Running Batch Jobs](running-batch-jobs.md)
- **Security guidance**: [Security](../security.md)
