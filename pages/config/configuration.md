---
layout: default
---

# Configuration

Configuration options for OpenToken inputs, outputs, secrets, and runtime behavior.

---

## CLI Arguments

OpenToken can be run from Java or Python CLIs, or via the helper shell/PowerShell scripts.

At a high level you must always specify:

- the input path and type (CSV or Parquet)
- an output path for tokens
- a key exchange configuration (receiver public key for `tokenize`; receiver keypair for `decrypt`)
- optionally `--hash-only` when you want hash-only output

For the complete, authoritative list of flags, short options, and defaults, see the [CLI Reference](../reference/cli.md).

---

## Environment Variables

Prefer passing key file paths (not raw key material) via CLI flags.

### Docker Environment

```bash
docker run --rm \
  -v $(pwd)/resources:/app/resources \
  opentoken:latest \
  tokenize \
  -i /app/resources/sample.csv -t csv -o /app/resources/output.zip \
  --receiver-public-key /app/resources/keys/receiver/public_key.pem \
  --sender-keypair-path /app/resources/keys/sender/keypair.pem
```

---

## Input File Format

### Supported Formats

| Format  | Extension  | Description                                          |
| ------- | ---------- | ---------------------------------------------------- |
| CSV     | `.csv`     | Comma-separated values with header row               |
| Parquet | `.parquet` | Columnar binary format (recommended for large files) |

### Column Names & Aliases

Input columns are **case-insensitive** and support common aliases:

| Attribute       | Accepted Column Names                                  | Required | Type   |
| --------------- | ------------------------------------------------------ | -------- | ------ |
| **Record ID**   | `RecordId`, `Id`                                       | Optional | String |
| **First Name**  | `FirstName`, `GivenName`                               | Yes      | String |
| **Last Name**   | `LastName`, `Surname`                                  | Yes      | String |
| **Birth Date**  | `BirthDate`, `DateOfBirth`                             | Yes      | Date   |
| **Sex**         | `Sex`, `Gender`                                        | Yes      | String |
| **Postal Code** | `PostalCode`, `ZipCode`, `ZIP3`, `ZIP4`, `ZIP5`        | Yes      | String |
| **SSN**         | `SocialSecurityNumber`, `NationalIdentificationNumber` | Yes      | String |

### Date Formats Accepted

- `YYYY-MM-DD` (recommended)
- `MM/DD/YYYY`
- `MM-DD-YYYY`
- `DD.MM.YYYY`

### Sex Values Accepted

- `Male`, `M`
- `Female`, `F`

(Case-insensitive)

### SSN Formats Accepted

- `123-45-6789` (preferred input format)
- Digits-only values (normalized automatically; dashes removed internally)

### Postal Code Formats

**US ZIP Codes:**
- `98004` (5 digits)
- `98004-1234` (9 digits, dash removed)
- `980` (ZIP-3, auto-padded to `98000`)

**Canadian Postal Codes:**
- `K1A 1A1` (with space)
- `K1A1A1` (without space, auto-formatted)

---

## Output Configuration

### Output Type Override

Use `-ot` to specify a different output format:

```bash
# Input CSV, output Parquet
java -jar opentoken-cli-*.jar \
  tokenize \
  -i data.csv -t csv \
  -o tokens.parquet -ot parquet \
  --receiver-public-key /path/to/receiver/public_key.pem \
  --sender-keypair-path /path/to/sender/keypair.pem
```

### Output Files Generated

Each run produces two files:

1. **Tokens file**: `<output_path>` (CSV or Parquet)
2. **Metadata file**: `<output_path>.metadata.json` (always JSON)

---

## Processing Modes

OpenToken supports three CLI modes:

- **Tokenize (default)** – produces an encrypted token package suitable for external exchange.
- **Tokenize + `--hash-only`** – produces hash-only output (no encryption).
- **Decrypt** – takes a previously encrypted token package and decrypts it back to hash-only output.

For the exact CLI flags that enable each mode, see the [CLI Reference](../reference/cli.md).

---

## Secret Requirements

### Key Requirements (CLI)

- **Receiver public key**: Required for `tokenize` (`--receiver-public-key`).
- **Sender keypair**: Optional for `tokenize` (`--sender-keypair-path`, default is typically `~/.opentoken/keypair.pem`).
- **Receiver keypair**: Required for `decrypt` (`--receiver-keypair-path`).

### Library APIs (Java/Python/PySpark)

Some library APIs still accept explicit hashing secrets and encryption keys for in-process token generation. See the relevant API references for details.

---

## Environment-Specific Configuration

### Local Development

```bash
# Java
cd lib/java
mvn clean install -DskipTests
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  tokenize \
  -i ../../resources/sample.csv -t csv -o ../../resources/output.zip \
  --receiver-public-key ../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../resources/keys/sender/keypair.pem

# Python
source /workspaces/OpenToken/.venv/bin/activate
python -m opentoken_cli.main \
  tokenize \
  -i ../../../resources/sample.csv -t csv -o ../../../resources/output.zip \
  --receiver-public-key ../../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../../resources/keys/sender/keypair.pem
```

### Docker Container

```bash
./run-opentoken.sh \
  tokenize \
  -i ./resources/sample.csv \
  -o ./resources/output.csv \
  -t csv \
  --receiver-public-key ./resources/keys/receiver/public_key.pem \
  --sender-keypair-path ./resources/keys/sender/keypair.pem
```

### Spark/Databricks Cluster

```python
from opentoken_pyspark import OpenTokenProcessor

processor = OpenTokenProcessor(
    hashing_secret=dbutils.secrets.get("opentoken", "hashing_secret"),
    encryption_key=dbutils.secrets.get("opentoken", "encryption_key")
)
```

See [Spark or Databricks](../operations/spark-or-databricks.md) for cluster configuration.

---

## Handling Missing/Invalid Data

| Scenario                    | Behavior                                              |
| --------------------------- | ----------------------------------------------------- |
| **RecordId missing**        | Auto-generates UUID for each record                   |
| **Required column missing** | Processing fails with column name mismatch error      |
| **NULL/empty value**        | Record marked invalid; counted in metadata            |
| **Invalid attribute**       | Record marked invalid; blank token for affected rules |

---

## Next Steps

- **Batch processing**: [Running Batch Jobs](../operations/running-batch-jobs.md)
- **Metadata format**: [Reference: Metadata Format](../reference/metadata-format.md)
