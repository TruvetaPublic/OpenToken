---
layout: default
---

# Decrypting Tokens

How to decrypt previously encrypted tokens for debugging, verification, or re-encryption.

---

## When to Decrypt

Decryption is useful for:

- **Debugging**: Verifying attribute normalization produced expected token signatures
- **Verification**: Confirming tokens match between datasets
- **Re-encryption**: Decrypting tokens to re-encrypt with a different key
- **Cross-language validation**: Ensuring Java and Python produce identical tokens

**Note:** Decryption produces HMAC-SHA256 hashed tokens (base64 encoded), **not** the original attribute values. Token generation is one-way.

---

## CLI Decrypt Mode

Use the `decrypt` subcommand with the receiver keypair. In most workflows, the sender public key is included in the `.zip` output produced by `tokenize`.

### Java

```bash
java -jar opentoken-cli/target/opentoken-cli-*.jar decrypt \
  -i ../../resources/output.zip \
  -t csv \
  -o ../../resources/decrypted.csv \
  --receiver-keypair-path /path/to/receiver/keypair.pem \
  --ecdh-curve P-384
```

### Python

```bash
opentoken decrypt \
  -i ../../../resources/output.zip \
  -t csv \
  -o ../../../resources/decrypted.csv \
  --receiver-keypair-path /path/to/receiver/keypair.pem \
  --ecdh-curve P-384
```

### Docker

```bash
docker run --rm -v $(pwd)/resources:/app/resources \
  opentoken:latest \
  decrypt \
  -i /app/resources/output.zip \
  -t csv \
  -o /app/resources/decrypted.csv \
  --receiver-keypair-path /app/resources/receiver/keypair.pem
```

---

## Decrypted Output Format

Decrypted tokens are HMAC-SHA256 hashes (base64 encoded)—equivalent to `--hash-only` output:

```csv
RecordId,RuleId,Token
ID001,T1,abc123def456...  # Base64-encoded HMAC hash
ID001,T2,fed456abc123...  # Same format as hash-only mode
...
```

This output can be used to:
- Compare with hash-only tokens from another run
- Verify token consistency across datasets
- Debug normalization issues

---

## Standalone Decryptor Tool

A Python decryptor tool is available in `tools/decryptor/`:

**Note:** This tool is for the legacy shared-secret encryption flow (AES key provided directly). For ECDH public key exchange workflows, use the OpenToken CLI `decrypt` command.

```bash
cd tools/decryptor
pip install pycryptodome

python decryptor.py \
  -e "Secret-Encryption-Key-Goes-Here." \
  -i ../../resources/output.csv \
  -o ../../resources/decrypted.csv
```

### Requirements

- Python 3.10+
- `pycryptodome` library
- CSV with `RuleId`, `Token`, `RecordId` columns

---

## Cross-Language Decryption

Tokens encrypted by Java can be decrypted by Python and vice versa (as long as the receiver uses the correct keypair and the sender public key is available):

```bash
# Tokenize with Java
java -jar opentoken-cli-*.jar tokenize \
  -i data.csv -t csv -o tokens.zip \
  --receiver-public-key /path/to/receiver/public_key.pem \
  --sender-keypair-path /path/to/sender/keypair.pem \
  --ecdh-curve P-384

# Decrypt with Python
opentoken decrypt \
  -i tokens.zip -t csv -o decrypted.csv \
  --receiver-keypair-path /path/to/receiver/keypair.pem \
  --ecdh-curve P-384
```

**Requirements for cross-language compatibility:**
- Same curve (`--ecdh-curve`) and compatible key material
- Sender public key available (either provided explicitly or included in the `.zip`)
- Same token file format

---

## Security Considerations

### Key Handling

- **Never commit private keys** (`keypair.pem`) to version control
- **Store private keys in a secret store** and materialize them only at runtime (for example, via secret mounts)
- **Rotate keys periodically** and coordinate key rollovers with partners

### Access Control

- **Limit decryption access**: Only authorized personnel should have access to receiver private keys
- **Audit decryption events**: Log when and why tokens are decrypted
- **Secure decrypted output**: Decrypted tokens are still sensitive (HMAC hashes)

### What Decryption Does NOT Reveal

Decryption produces HMAC hashes, **not** original data:

```
Original: John Doe, 1980-01-15, Male
    ↓
Token Signature: DOE|JOHN|MALE|1980-01-15
    ↓
SHA-256 Hash: abc123...
    ↓
  HMAC-SHA256: def456...  ← Decryption reveals this
    ↓
  AES-256-GCM: xyz789...  ← Encrypted token
```

You **cannot** reverse the original attribute values from decrypted tokens.

---

## Troubleshooting

| Problem                             | Solution                                                                                 |
| ----------------------------------- | ---------------------------------------------------------------------------------------- |
| "Decryption error"                  | Verify receiver keypair and sender public key match the token package                    |
| "Public key file not found"         | Provide `--sender-public-key` or ensure the input `.zip` includes it                     |
| "Keypair file not found"            | Verify `--receiver-keypair-path` exists and is readable                                  |
| Blank tokens in output              | Blank tokens in input (from invalid records) remain blank                                |
| Tokens don't match across languages | Run interoperability test: `tools/interoperability/java_python_interoperability_test.py` |

---

## Next Steps

- **Hash-only mode**: [Hash-Only Mode](hash-only-mode.md) (no encryption needed)
- **Batch processing**: [Running Batch Jobs](running-batch-jobs.md)
- **Security guidance**: [Security](../security.md)
