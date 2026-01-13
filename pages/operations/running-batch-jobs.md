---
layout: default
---

# Running Batch Jobs

How to run OpenToken in batch mode across CSV or Parquet files at scale using CLI or Docker.

---

## Overview

OpenToken processes input files (CSV or Parquet) and produces two outputs:

1. **Tokens file** (CSV or Parquet): Contains `RecordId`, `RuleId`, `Token` columns
2. **Metadata file** (JSON): Processing statistics, key fingerprints, and validation counts

---

## CLI Batch Processing

### Basic Syntax

```bash
opentoken <command> [OPTIONS]

# commands:
#   generate-keypair
#   tokenize
#   decrypt
```

See the [CLI Reference](../reference/cli.md) for the complete list of flags.

### Java CLI Example

```bash
cd lib/java
mvn clean install -DskipTests

java -jar opentoken-cli/target/opentoken-cli-*.jar \
  tokenize \
  -i ../../resources/sample.csv -t csv -o ../../resources/output.zip \
  --receiver-public-key ../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../resources/keys/sender/keypair.pem
```

### Python CLI Example

```bash
cd lib/python/opentoken-cli
source ../../.venv/bin/activate
pip install -r requirements.txt -e . -e ../opentoken

python -m opentoken_cli.main \
  tokenize \
  -i ../../../resources/sample.csv -t csv -o ../../../resources/output.zip \
  --receiver-public-key ../../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../../resources/keys/sender/keypair.pem
```

---

## Docker Batch Processing

### Convenience Scripts (Recommended)

**Bash (Linux/Mac):**
```bash
cd /path/to/OpenToken

./run-opentoken.sh \
  tokenize \
  -i ./resources/sample.csv \
  -o ./resources/output.csv \
  -t csv \
  --receiver-public-key ./resources/keys/receiver/public_key.pem \
  --sender-keypair-path ./resources/keys/sender/keypair.pem
```

**PowerShell (Windows):**
```powershell
cd C:\path\to\OpenToken

.\run-opentoken.ps1 `
  tokenize `
  -i .\resources\sample.csv `
  -o .\resources\output.csv `
  -FileType csv `
  --receiver-public-key .\resources\keys\receiver\public_key.pem `
  --sender-keypair-path .\resources\keys\sender\keypair.pem
```

### Script Options

| Option       | Bash | PowerShell   | Description          |
| ------------ | ---- | ------------ | -------------------- |
| File type    | `-t` | `-FileType`  | `csv` or `parquet`   |
| Skip rebuild | `-s` | `-SkipBuild` | Reuse existing image |
| Verbose      | `-v` | `-Verbose`   | Show detailed output |

### Manual Docker Commands

```bash
# Build the image
docker build -t opentoken:latest .

# Run with sample data
docker run --rm -v $(pwd)/resources:/app/resources \
  opentoken:latest \
  tokenize \
  -i /app/resources/sample.csv -t csv -o /app/resources/output.zip \
  --receiver-public-key /app/resources/keys/receiver/public_key.pem \
  --sender-keypair-path /app/resources/keys/sender/keypair.pem

# View output
cat resources/output.csv
cat resources/output.metadata.json
```

---

## Exit Codes

| Exit Code | Meaning                                                 |
| --------- | ------------------------------------------------------- |
| `0`       | Success                                                 |
| `1`       | General error (invalid arguments, file not found, etc.) |
| Non-zero  | Processing failure; check stderr for details            |

---

## Output Files

### Tokens File (CSV)

```csv
RecordId,RuleId,Token
ID001,T1,Gn7t1Zj16E5Qy+z9iINtczP6fRDYta6C0XFrQtpjnVQSEZ5pQXAzo02Aa9LS9oNMOog6Ssw9GZE6fvJrX2sQ/cThSkB6m91L
ID001,T2,pUxPgYL9+cMxkA+8928Pil+9W+dm9kISwHYPdkZS+I2nQ/bQ/8HyL3FOVf3NYPW5NKZZO1OZfsz7LfKYpTlaxyzMLqMF2Wk7
...
```

**Rows per input record:** 5 (one per rule T1–T5)

### Metadata File (JSON)

```json
{
  "Platform": "Java",
  "JavaVersion": "21.0.0",
  "OpenTokenVersion": "1.0.0",
  "KeyExchangeMethod": "ECDH-P-384",
  "Curve": "P-384",
  "SenderPublicKeyHash": "a85b4bd6...",
  "ReceiverPublicKeyHash": "32bc0e98...",
  "TotalRows": 100,
  "TotalRowsWithInvalidAttributes": 3,
  "InvalidAttributesByType": { "BirthDate": 2, "PostalCode": 1 },
  "BlankTokensByRule": { "T1": 2, "T2": 1 }
}
```

See [Reference: Metadata Format](../reference/metadata-format.md) for complete field descriptions.

---

## Common Patterns

### Environment Variables

Prefer file-based key material (`--receiver-public-key`, `--sender-keypair-path`, `--receiver-keypair-path`) instead of passing sensitive values directly on the command line.

### Logging and Monitoring

Check the metadata file after each run for:
- `TotalRowsWithInvalidAttributes`: Records that failed validation
- `InvalidAttributesByType`: Breakdown by attribute type
- `BlankTokensByRule`: Rules that produced blank tokens

---

## Troubleshooting

| Problem                       | Solution                                                                |
| ----------------------------- | ----------------------------------------------------------------------- |
| "Missing receiver public key" | Provide `--receiver-public-key` (tokenize)                              |
| "Invalid BirthDate"           | Use YYYY-MM-DD format; date must be 1910-01-01 to today                 |
| "Column not found"            | Check column names match [accepted aliases](../config/configuration.md) |
| Docker build fails            | Ensure Docker is running; use absolute paths                            |

---

## Next Steps

- **Distributed processing**: [Spark or Databricks](spark-or-databricks.md)
- **Hash-only mode**: [Hash-Only Mode](hash-only-mode.md)
- **Decrypt tokens**: [Decrypting Tokens](decrypting-tokens.md)
