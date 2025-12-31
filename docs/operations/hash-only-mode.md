# Hash-Only Mode

How to generate tokens using HMAC-SHA256 without AES encryption.

---

## Overview

Hash-only mode generates deterministic tokens without AES encryption:

```
Token Signature → SHA-256 Hash → HMAC-SHA256(hash, secret) → Base64 Encode
```

Compared to encryption mode:

```
Token Signature → SHA-256 Hash → HMAC-SHA256(hash, secret) → AES-256-GCM Encrypt → Base64 Encode
```

---

## When to Use Hash-Only Mode

**Use hash-only when:**

- Token matching within a single organization (no external sharing)
- Raw data is already protected at rest
- Encryption overhead is unnecessary
- Faster processing is preferred
- Smaller token output is desired

**Use encryption mode when:**

- Sharing tokens with external parties
- Defense in depth is required
- Regulatory requirements mandate encryption
- Tokens may be stored in less-secure systems

---

## CLI Usage

Use the `--hash-only` flag. Only the hashing secret is required (no encryption key).

### Java

```bash
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  --hash-only \
  -i ../../resources/sample.csv \
  -t csv \
  -o ../../resources/hashed-output.csv \
  -h "HashingKey"
```

### Python

```bash
python -m opentoken_cli.main \
  --hash-only \
  -i ../../../resources/sample.csv \
  -t csv \
  -o ../../../resources/hashed-output.csv \
  -h "HashingKey"
```

### Docker

```bash
docker run --rm -v $(pwd)/resources:/app/resources \
  opentoken:latest \
  --hash-only \
  -i /app/resources/sample.csv \
  -t csv \
  -o /app/resources/hashed-output.csv \
  -h "HashingKey"
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

### Encryption Mode Metadata

```json
{
  "HashingSecretHash": "abc123...",
  "EncryptionSecretHash": "def456..."
}
```

### Hash-Only Mode Metadata

```json
{
  "HashingSecretHash": "abc123..."
}
```

No `EncryptionSecretHash` field is present in hash-only mode.

---

## Security Trade-offs

| Aspect | Encryption Mode | Hash-Only Mode |
|--------|-----------------|----------------|
| **Token length** | ~80-100 chars | ~44-64 chars |
| **Processing speed** | Slower | Faster |
| **Secret required** | Hashing secret + encryption key | Hashing secret only |
| **Reversibility** | Decryptable (to HMAC hash) | Not decryptable |
| **External sharing** | Recommended | Not recommended |
| **Defense in depth** | Yes | No |

### Security Notes

- **Both modes are one-way**: Original attributes cannot be recovered from either token type
- **Same hashing secret = same tokens**: Hash-only tokens from different runs with the same secret will match
- **Cross-language parity**: Java and Python produce identical hash-only tokens for the same input

---

## Matching Hash-Only Tokens

Hash-only tokens can be matched directly without decryption:

```sql
-- Match records between datasets
SELECT a.RecordId AS RecordA, b.RecordId AS RecordB
FROM tokens_a a
JOIN tokens_b b ON a.Token = b.Token AND a.RuleId = b.RuleId
WHERE a.RuleId = 'T1';
```

For encrypted tokens, either:
1. Decrypt both datasets first, then match
2. Use the same encryption key for both datasets and match encrypted tokens directly

---

## Troubleshooting

### Tokens Don't Match Between Runs

**Cause:** Different hashing secrets.

**Solution:** Verify the same hashing secret is used for both runs:
```bash
# Check metadata for secret hash
cat output.metadata.json | jq '.HashingSecretHash'
```

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

**Cause:** Missing `--hash-only` flag.

**Solution:** Add `--hash-only` to skip encryption:
```bash
java -jar opentoken-cli-*.jar --hash-only -i data.csv -t csv -o out.csv -h "Key"
```

---

## Next Steps

- **Encryption mode**: [Decrypting Tokens](decrypting-tokens.md)
- **Batch processing**: [Running Batch Jobs](running-batch-jobs.md)
- **Security guidance**: [Security](../security.md)
