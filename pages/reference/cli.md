---
layout: default
---

# CLI Reference

Complete reference for OpenToken CLI commands, modes, and examples. This page is the single source of truth for CLI flags and options; other documentation (such as Configuration) links here instead of duplicating them.

## Security Note

Treat generated token outputs and metadata as **sensitive**. In particular, `--hash-only` output is intended for internal use and should not be shared externally (for example, in tickets, chats, or public repos).

Hash-only mode is primarily used to build **internal overlap-analysis datasets** that can be joined against **encrypted tokens received from external partners** (after decryption). If you need to **exchange** tokens across organizations, use encrypted mode and follow a controlled exchange process: [Sharing Tokenized Data](../operations/sharing-tokenized-data.md).

## Command Syntax

```bash
# Java
java -jar opentoken-cli-*.jar <command> [command options]

# Python
opentoken <command> [command options]

# (or, equivalent)
python -m opentoken_cli.main <command> [command options]
```

## Commands

OpenToken CLI is organized around three subcommands:

- `generate-keypair` — generate an ECDH keypair for exchange
- `tokenize` — tokenize person attributes using ECDH key exchange
- `decrypt` — decrypt tokens using ECDH key exchange

## `generate-keypair`

Generate a new keypair and write it to disk.

```bash
opentoken generate-keypair --output-dir ~/.opentoken --ecdh-curve P-384
```

**Options**

| Option         | Description                                           | Default        |
| -------------- | ----------------------------------------------------- | -------------- |
| `--output-dir` | Directory to write `keypair.pem` and `public_key.pem` | `~/.opentoken` |
| `--ecdh-curve` | Elliptic curve name for ECDH                          | `P-256`        |

## `tokenize`

Tokenize a CSV or Parquet file. For cross-organization exchange, output is typically a `.zip` package.

```bash
opentoken tokenize \
  -i input.csv -t csv -o output.zip \
  --receiver-public-key /path/to/receiver/public_key.pem \
  --sender-keypair-path /path/to/sender/keypair.pem \
  --ecdh-curve P-384
```

**Required arguments**

| Argument                | Short | Description                           |
| ----------------------- | ----- | ------------------------------------- |
| `--input`               | `-i`  | Path to input file                    |
| `--output`              | `-o`  | Path to output file (commonly `.zip`) |
| `--type`                | `-t`  | Input type: `csv` or `parquet`        |
| `--receiver-public-key` |       | Path to receiver public key (PEM)     |

**Optional arguments**

| Argument                | Short | Description                         | Default                    |
| ----------------------- | ----- | ----------------------------------- | -------------------------- |
| `--sender-keypair-path` |       | Sender keypair path (PEM)           | `~/.opentoken/keypair.pem` |
| `--hash-only`           |       | Hash-only mode (no encryption)      | `false`                    |
| `--output-type`         | `-ot` | Output type if different from input | Same as input              |
| `--ecdh-curve`          |       | Elliptic curve name for ECDH        | `P-256`                    |

## `decrypt`

Decrypt tokens (usually from a `.zip` package) back to their hash-only representation.

```bash
opentoken decrypt \
  -i output.zip -t csv -o decrypted.csv \
  --receiver-keypair-path /path/to/receiver/keypair.pem \
  --ecdh-curve P-384
```

**Required arguments**

| Argument   | Short | Description                         |
| ---------- | ----- | ----------------------------------- |
| `--input`  | `-i`  | Input file path (or `.zip` package) |
| `--output` | `-o`  | Output file path                    |
| `--type`   | `-t`  | Output type: `csv` or `parquet`     |

**Optional arguments**

| Argument                  | Short | Description                                                           | Default                    |
| ------------------------- | ----- | --------------------------------------------------------------------- | -------------------------- |
| `--receiver-keypair-path` |       | Receiver keypair path (PEM)                                           | `~/.opentoken/keypair.pem` |
| `--sender-public-key`     |       | Sender public key (PEM). If omitted, extracted from `.zip` if present |                            |
| `--output-type`           | `-ot` | Output type if different from input                                   | Same as input              |
| `--ecdh-curve`            |       | Elliptic curve name for ECDH                                          | `P-256`                    |

## Modes of Operation

### Encrypted Mode (default)

Produces an encrypted token package suitable for controlled exchange with external partners.

### Hash-Only Mode

Produces hashed tokens without encryption. Hash-only output is intended for **internal use** (for example, overlap analysis workflows).

## File Format Examples

### CSV Input

```csv
RecordId,FirstName,LastName,BirthDate,Sex,PostalCode,SSN
patient_001,John,Doe,1980-01-15,Male,98004,123-45-6789
patient_002,Jane,Smith,1975-03-22,Female,90210,987-65-4321
```

**Column Aliases Accepted:**

| Standard Name | Accepted Aliases                                   |
| ------------- | -------------------------------------------------- |
| RecordId      | Id                                                 |
| FirstName     | GivenName                                          |
| LastName      | Surname                                            |
| BirthDate     | DateOfBirth                                        |
| Sex           | Gender                                             |
| PostalCode    | ZipCode, ZIP3, ZIP4, ZIP5                          |
| SSN           | SocialSecurityNumber, NationalIdentificationNumber |

### CSV Output

```csv
RecordId,RuleId,Token
patient_001,T1,Gn7t1Zj16E5Qy+z9iINtczP6fRDYta6C0XFr...
patient_001,T2,pUxPgYL9+cMxkA+8928Pil+9W+dm9kISwHYP...
patient_001,T3,rwjfwIo5OcJUItTx8KCoSZMtr7tVGSyXsWv/...
patient_001,T4,9o7HIYZkhizczFzJL1HFyanlllzSa8hlgQWQ...
patient_001,T5,QpBpGBqaMhagfcHGZhVavn23ko03jkyS9Vo4...
```

### Parquet Schema

**Input:**
```
RecordId: string
FirstName: string
LastName: string
BirthDate: string (YYYY-MM-DD)
Sex: string
PostalCode: string
SSN: string
```

**Output:**
```
RecordId: string
RuleId: string
Token: string
```

## Metadata Output

Every run generates a `.metadata.json` file:

```json
{
  "Platform": "Java",
  "JavaVersion": "21.0.0",
  "OpenTokenVersion": "1.12.2",
  "KeyExchangeMethod": "ECDH-P-384",
  "Curve": "P-384",
  "SenderPublicKeyHash": "a85b4bd6...",
  "ReceiverPublicKeyHash": "32bc0e98...",
  "TotalRows": 100,
  "TotalRowsWithInvalidAttributes": 3,
  "InvalidAttributesByType": {
    "BirthDate": 2,
    "SSN": 1
  },
  "BlankTokensByRule": {
    "T1": 2,
    "T4": 1
  }
}
```

## Docker Script Options

### Bash (run-opentoken.sh)

```bash
./run-opentoken.sh tokenize \
  -i ./input.csv -t csv -o ./output.zip \
  --receiver-public-key ./keys/receiver/public_key.pem \
  [--sender-keypair-path ./keys/sender/keypair.pem] \
  [--ecdh-curve P-384] \
  [--hash-only] \
  [--skip-build] [--verbose]
```

| Option         | Description               |
| -------------- | ------------------------- |
| `--skip-build` | Skip Docker image rebuild |
| `--verbose`    | Show detailed output      |
| `--help`       | Show help message         |

### PowerShell (run-opentoken.ps1)

```powershell
.\run-opentoken.ps1 -c tokenize `
  -i .\input.csv -t csv -o .\output.zip `
  -ReceiverPublicKey .\keys\receiver\public_key.pem `
  [-SenderKeypairPath .\keys\sender\keypair.pem] `
  [-EcdhCurve P-384] `
  [-HashOnly] `
  [-SkipBuild] [-VerboseOutput]
```

## Error Messages

| Error                          | Cause                                                       | Solution                                         |
| ------------------------------ | ----------------------------------------------------------- | ------------------------------------------------ |
| "Expected a command"           | No subcommand provided                                      | Use `generate-keypair`, `tokenize`, or `decrypt` |
| "Input file not found"         | Invalid path                                                | Check file exists                                |
| "Public key file not found"    | Invalid `--receiver-public-key` / `--sender-public-key`     | Check file exists and is PEM                     |
| "Keypair file not found"       | Invalid `--sender-keypair-path` / `--receiver-keypair-path` | Check file exists and is PEM                     |
| "Unknown file type"            | Invalid `-t` value                                          | Use `csv` or `parquet`                           |
| "Invalid attribute: BirthDate" | Date validation failed                                      | Use YYYY-MM-DD format                            |

## Exit Codes

| Code | Meaning           |
| ---- | ----------------- |
| 0    | Success           |
| 1    | Invalid arguments |
| 2    | File not found    |
| 3    | Processing error  |

## Performance Tips

- Use Parquet for large datasets (faster I/O, compression)
- Use `--hash-only` if decryption not needed (faster, smaller outputs)
- For very large files, consider [PySpark integration](../operations/spark-or-databricks.md)

## Next Steps

- [Java API Reference](java-api.md) - Programmatic usage
- [Python API Reference](python-api.md) - Programmatic usage
- [Configuration](../config/configuration.md) - Advanced settings
- [Decrypting Tokens](../operations/decrypting-tokens.md) - Reverse encrypted tokens
