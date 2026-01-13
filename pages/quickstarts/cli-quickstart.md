---
layout: default
---

# CLI Quickstart

For a high-level overview and other entry points, see [Quickstarts](index.md).

Run the OpenToken CLI end-to-end to generate tokens from a sample dataset in minutes.

## Prerequisites

Choose one of:

- **Docker** (recommended) - No other dependencies needed
- **Java 21+** and Maven 3.8+
- **Python 3.10+**

## Quick Start with Docker

The fastest way to get started. No Java or Python installation required.

### Linux/Mac

```bash
cd /path/to/OpenToken

# Generate a receiver keypair (share the public key with the sender)
mkdir -p ./target/keys/receiver
./run-opentoken.sh generate-keypair --output-dir ./target/keys/receiver

# Generate a sender keypair
mkdir -p ./target/keys/sender
./run-opentoken.sh generate-keypair --output-dir ./target/keys/sender

# Tokenize (sender encrypts tokens using receiver public key)
./run-opentoken.sh tokenize \
  -i ./resources/sample.csv -t csv -o ./target/output.zip \
  --receiver-public-key ./target/keys/receiver/public_key.pem \
  --sender-keypair-path ./target/keys/sender/keypair.pem

# Decrypt (receiver decrypts using receiver private key)
./run-opentoken.sh decrypt \
  -i ./target/output.zip -t csv -o ./target/decrypted.csv \
  --receiver-keypair-path ./target/keys/receiver/keypair.pem
```

### Windows PowerShell

```powershell
cd C:\path\to\OpenToken

mkdir .\target\keys\receiver -Force | Out-Null
.\run-opentoken.ps1 generate-keypair --output-dir .\target\keys\receiver

mkdir .\target\keys\sender -Force | Out-Null
.\run-opentoken.ps1 generate-keypair --output-dir .\target\keys\sender

.\run-opentoken.ps1 tokenize `
  -i .\resources\sample.csv -FileType csv -o .\target\output.zip `
  --receiver-public-key .\target\keys\receiver\public_key.pem `
  --sender-keypair-path .\target\keys\sender\keypair.pem `
 

.\run-opentoken.ps1 decrypt `
  -i .\target\output.zip -FileType csv -o .\target\decrypted.csv `
  --receiver-keypair-path .\target\keys\receiver\keypair.pem `
 
```

## CLI Commands

OpenToken CLI uses subcommands:

- `generate-keypair`
- `tokenize`
- `decrypt`

See the full flag reference in [CLI Reference](../reference/cli.md).

## Key Exchange Inputs (Tokenize/Decrypt)

| Option                    | Used by    | Purpose                                                     |
| ------------------------- | ---------- | ----------------------------------------------------------- |
| `--receiver-public-key`   | `tokenize` | Receiver public key used to derive encryption keys via ECDH |
| `--sender-keypair-path`   | `tokenize` | Sender private key used to perform ECDH                     |
| `--receiver-keypair-path` | `decrypt`  | Receiver private key used to perform ECDH                   |
| `--sender-public-key`     | `decrypt`  | Sender public key (usually included in the `.zip` output)   |

## Example: CSV Input

## Example: CSV Input

**Input file (`sample.csv`):**

```csv
RecordId,FirstName,LastName,BirthDate,Sex,PostalCode,SSN
patient_001,John,Doe,1980-01-15,Male,98004,123-45-6789
patient_002,Jane,Smith,1975-03-22,Female,90210,987-65-4321
```

**Tokenize command (Java example):**

```bash
java -jar opentoken-cli-*.jar tokenize \
  -i sample.csv -t csv -o output.zip \
  --receiver-public-key ./receiver/public_key.pem \
  --sender-keypair-path ./sender/keypair.pem \
 
```

**Output (`tokens.csv`):**

```csv
RecordId,RuleId,Token
patient_001,T1,Gn7t1Zj16E5Qy+z9iINtcz...
patient_001,T2,pUxPgYL9+cMxkA+8928Pi...
patient_001,T3,rwjfwIo5OcJUItTx8KCo...
patient_001,T4,9o7HIYZkhizczFzJL1HFy...
patient_001,T5,QpBpGBqaMhagfcHGZhVa...
patient_002,T1,...
```

## Example: Parquet Input

```bash
java -jar opentoken-cli-*.jar tokenize \
  -i input.parquet -t parquet -o output.zip \
  --receiver-public-key ./receiver/public_key.pem
```

## Hash-Only Mode

Generate tokens without encryption (faster, but tokens cannot be decrypted):

```bash
java -jar opentoken-cli-*.jar tokenize \
  -i sample.csv -t csv -o output.zip \
  --receiver-public-key ./receiver/public_key.pem \
  --sender-keypair-path ./sender/keypair.pem \
  --hash-only
```

### Security Note (Hash-Only)

`--hash-only` output is intended for **internal use** and should **not** be shared externally. Hash-only tokens are deterministic and can still be linkable across datasets.

The primary use case for hash-only mode is to build an **internal overlap-analysis dataset** that you join against **encrypted tokens received from an external partner** (after decrypting their tokens to the hash-only equivalent). If you need to exchange tokens across organizations, use encrypted mode and follow [Sharing Tokenized Data](../operations/sharing-tokenized-data.md).

## Understanding the Output

### Token File

Each input record produces 5 tokens (T1–T5):

| Column     | Description                           |
| ---------- | ------------------------------------- |
| `RecordId` | Original record identifier            |
| `RuleId`   | Token rule (T1, T2, T3, T4, or T5)    |
| `Token`    | Base64-encoded encrypted/hashed token |

### Metadata File

A `.metadata.json` file is created alongside the output:

```json
{
  "Platform": "Java",
  "JavaVersion": "21.0.0",
  "OpenTokenVersion": "1.7.0",
  "KeyExchangeMethod": "ECDH-P-384",
  "Curve": "P-384",
  "SenderPublicKeyHash": "a85b4bd6...",
  "ReceiverPublicKeyHash": "32bc0e98...",
  "TotalRows": 2,
  "TotalRowsWithInvalidAttributes": 0,
  "InvalidAttributesByType": {},
  "BlankTokensByRule": {}
}
```

## Troubleshooting

### "Expected a command"

Make sure you provide a subcommand like `tokenize`, `decrypt`, or `generate-keypair`.

### "Invalid BirthDate"

Ensure dates are in `YYYY-MM-DD` format and between 1910-01-01 and today.

### "File not found"

Check that input file path is correct and file exists.

### "Invalid SSN"

SSN must be 9 digits. Area code cannot be 000, 666, or 900-999.

## Next Steps

- [Java Quickstart](java-quickstart.md) - Build from source
- [Python Quickstart](python-quickstart.md) - Use Python CLI
- [Configuration](../config/configuration.md) - Advanced options
- [Token Rules](../concepts/token-rules.md) - Understand T1-T5
