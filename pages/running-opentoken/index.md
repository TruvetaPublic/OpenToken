---
layout: default
---

# Running OpenToken

Guides for generating tokens in different environments and use cases.

## CLI Guide

The OpenToken CLI accepts command-line arguments for flexible token generation. Both Java and Python CLIs support identical options.

### Basic Syntax

```bash
opentoken <command> [OPTIONS]

# commands:
#   generate-keypair
#   tokenize
#   decrypt
```

### Commands

For the complete, authoritative list of flags and defaults, see the [CLI Reference](../reference/cli.md).

- `generate-keypair`: Create a keypair directory containing `keypair.pem` and `public_key.pem`.
- `tokenize`: Produce tokens for a receiver using `--receiver-public-key` and (optionally) `--sender-keypair-path`.
- `decrypt`: Decrypt a token package back to hash-only form using `--receiver-keypair-path`.

### Usage Examples

#### Token Generation (Key Exchange / Encrypted)

**Java:**
```bash
cd lib/java
mvn clean install -DskipTests

java -jar opentoken-cli/target/opentoken-cli-*.jar \
  generate-keypair --output-dir ../../resources/keys/receiver --ecdh-curve P-384

java -jar opentoken-cli/target/opentoken-cli-*.jar \
  tokenize \
  -i ../../resources/sample.csv -t csv -o ../../resources/output.zip \
  --receiver-public-key ../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../resources/keys/sender/keypair.pem \
  --ecdh-curve P-384
```

**Python:**
```bash
cd lib/python/opentoken-cli
source ../../.venv/bin/activate
pip install -r requirements.txt -e . -e ../opentoken

python -m opentoken_cli.main \
  generate-keypair --output-dir ../../../resources/keys/receiver --ecdh-curve P-384

python -m opentoken_cli.main \
  tokenize \
  -i ../../../resources/sample.csv -t csv -o ../../../resources/output.zip \
  --receiver-public-key ../../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../../resources/keys/sender/keypair.pem \
  --ecdh-curve P-384
```

#### Token Generation (Hash-Only Mode)

Generates HMAC-SHA256 hashed tokens without AES encryption. Only hashing secret required.

**Java:**
```bash
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  tokenize \
  --hash-only \
  -i ../../resources/sample.csv -t csv -o ../../resources/hashed-output.zip \
  --receiver-public-key ../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../resources/keys/sender/keypair.pem
```

**Python:**
```bash
python -m opentoken_cli.main \
  tokenize \
  --hash-only \
  -i ../../../resources/sample.csv -t csv -o ../../../resources/hashed-output.zip \
  --receiver-public-key ../../../resources/keys/receiver/public_key.pem \
  --sender-keypair-path ../../../resources/keys/sender/keypair.pem
```

#### Token Decryption

Decrypts previously encrypted token packages back to hash-only form.

**Java:**
```bash
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  decrypt \
  -i ../../resources/output.zip -t csv -o ../../resources/decrypted.csv \
  --receiver-keypair-path ../../resources/keys/receiver/keypair.pem
```

**Python:**
```bash
python -m opentoken_cli.main \
  decrypt \
  -i ../../../resources/output.zip -t csv -o ../../../resources/decrypted.csv \
  --receiver-keypair-path ../../../resources/keys/receiver/keypair.pem
```

### Output Files

Token generation produces two files:

**Tokens File** (CSV or Parquet):
```
RecordId,RuleId,Token
record1,T1,Gn7t1Zj16E5Qy+z9iINtczP6fRDYta6C0XFrQtpjnVQSEZ5pQXAzo02Aa9LS9oNMOog6Ssw9GZE6fvJrX2sQ/cThSkB6m91L
record1,T2,pUxPgYL9+cMxkA+8928Pil+9W+dm9kISwHYPdkZS+I2nQ/bQ/8HyL3FOVf3NYPW5NKZZO1OZfsz7LfKYpTlaxyzMLqMF2Wk7
...
```

**Metadata File** (always JSON, suffixed `.metadata.json`):
```json
{
  "JavaVersion": "21.0.0",
  "OpenTokenVersion": "1.12.2",
  "Platform": "Java",
  "KeyExchangeMethod": "ECDH-P-384",
  "Curve": "P-384",
  "SenderPublicKeyHash": "a85b4bd6...",
  "ReceiverPublicKeyHash": "32bc0e98...",
  "TotalRows": 1,
  "TotalRowsWithInvalidAttributes": 0,
  "InvalidAttributesByType": {},
  "BlankTokensByRule": {}
}
```

See [Reference: Metadata Format](../reference/metadata-format.md) for detailed field descriptions.

---

## Docker

Use Docker for a containerized, dependency-free environment.

### Option 1: Convenience Scripts (Recommended)

Scripts automatically build and run the container.

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
  -c tokenize `
  -i .\resources\sample.csv `
  -o .\resources\output.csv `
  -t csv `
  -ReceiverPublicKey .\resources\keys\receiver\public_key.pem `
  -SenderKeypairPath .\resources\keys\sender\keypair.pem
```

#### Script Options

| Option       | Bash Alias | PowerShell       | Description          |
| ------------ | ---------- | ---------------- | -------------------- |
| File type    | `-t`       | `-t`             | `csv` or `parquet`   |
| Skip rebuild | `-s`       | `-SkipBuild`     | Reuse existing image |
| Verbose      | `-v`       | `-VerboseOutput` | Show detailed output |

Run with `--help` (Bash) or `-Help` (PowerShell) for full usage.

### Option 2: Manual Docker Commands

Build and run the image manually from the repository root.

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

**Dev Container:** If running in a dev container, use absolute path:
```bash
docker run --rm -v /workspaces/OpenToken/resources:/app/resources \
  opentoken:latest ...
```

---

## PySpark Bridge

For large-scale distributed token generation and dataset overlap analysis, use the PySpark bridge.

### When to Use PySpark

- **Large datasets**: Millions of records across multiple files
- **Distributed processing**: Leverage cluster computing
- **Overlap analysis**: Find matching records across datasets at scale
- **Cost-effective**: Process on cloud infrastructure (AWS, GCP, Azure)

### Installation

Ensure the Python root venv is active, then install:

```bash
source /workspaces/OpenToken/.venv/bin/activate

cd lib/python/opentoken-pyspark
pip install -r requirements.txt -e .
```

### Basic Usage

```python
from opentoken_pyspark import SparkPersonTokenProcessor

# Initialize Spark session
spark = SparkSession.builder \
    .appName("OpenToken") \
    .getOrCreate()

# Create processor
processor = SparkPersonTokenProcessor(
    spark=spark,
    hashing_secret="HashingKey",
    encryption_key="Secret-Encryption-Key"
)

# Process dataset
tokens_df = processor.process_dataframe(
    input_df=input_spark_df,
    input_type="csv"  # or "parquet"
)

# Write output
tokens_df.coalesce(1).write \
    .mode("overwrite") \
    .csv("output/tokens")
```

### Examples

See example notebooks in `lib/python/opentoken-pyspark/notebooks/`:

- [Custom_Token_Definition_Guide.ipynb](https://github.com/TruvetaPublic/OpenToken/blob/main/lib/python/opentoken-pyspark/notebooks/Custom_Token_Definition_Guide.ipynb) – Define custom token rules
- [Dataset_Overlap_Analysis_Guide.ipynb](https://github.com/TruvetaPublic/OpenToken/blob/main/lib/python/opentoken-pyspark/notebooks/Dataset_Overlap_Analysis_Guide.ipynb) – Find overlapping records across datasets

---

## Troubleshooting

### "Expected a command" / "Unknown command"

**Problem**: You invoked the CLI without a subcommand.

**Solution**: Use one of `generate-keypair`, `tokenize`, or `decrypt` (see [CLI Reference](../reference/cli.md)).

### "Missing receiver public key"

**Problem**: `tokenize` is missing `--receiver-public-key`.

**Solution**: Provide the receiver's `public_key.pem` path.

### "Invalid BirthDate" or "Date out of range"

**Problem**: BirthDate attribute fails validation.

**Causes**:
- Date is before January 1, 1910
- Date is in the future
- Format is not recognized

**Solution**: Use YYYY-MM-DD format or one of the accepted formats (MM/DD/YYYY, MM-DD-YYYY, DD.MM.YYYY):
```
Correct: 1980-01-15, 01/15/1980
Wrong:   1905-01-01, 2025-12-31, 01-15-80
```

### "Invalid SSN" or "SSN area/group/serial invalid"

**Problem**: SSN fails validation (area, group, or serial validation).

**Causes**:
- Area: 000, 666, or 900–999
- Group: 00
- Serial: 0000
- Common invalid sequences: 111-11-1111, 222-22-2222, etc.

**Solution**: Validate SSN before processing or regenerate test data. See [Security](../security.md) for full rules.

### "Invalid LastName" or "Name is placeholder"

**Problem**: Name is rejected as placeholder or invalid.

**Causes**:
- Value is placeholder: "Unknown", "Test", "N/A", "Anonymous", "Missing"
- LastName is too short (< 2 chars) without being special case ("Ng")
- Null or empty

**Solution**: Clean data before processing. Remove or replace placeholder values.

### "Docker image not found" or build fails

**Problem**: Docker image won't build or run.

**Causes**:
- Docker daemon not running
- Insufficient disk space
- File path issues on Windows

**Solution**:
1. Ensure Docker is running: `docker --version`
2. Use absolute paths, not relative: `/workspaces/OpenToken/resources`
3. Clear Docker cache if needed: `docker system prune`
4. Check file permissions: `ls -la resources/sample.csv`

### Tokens don't match across Java/Python

**Problem**: Same input produces different tokens in Java vs. Python.

**Causes**:
- Different hashing/encryption secrets
- Different attribute normalization
- Unicode handling differences

**Solution**:
1. Verify secrets match exactly
2. Check attribute normalization (see [Concepts: Normalization](../concepts/normalization-and-validation.md))
3. Run the interoperability test suite: `tools/interoperability/java_python_interoperability_test.py`
4. Decrypt and compare hashes to isolate the issue

### CSV parsing errors or column not found

**Problem**: "Column 'FirstName' not found" or CSV parse error.

**Causes**:
- Column names don't match expected aliases
- Commas within values without quoting
- Encoding issues (non-UTF-8)

**Solution**:
1. Verify column names match accepted aliases (see [Configuration](../config/configuration.md))
2. Quote values containing commas: `"Doe, Jr."`
3. Ensure UTF-8 encoding
4. Use Parquet format if CSV parsing continues to fail

---

## Next Steps

- **Get started**: [Quickstarts](../quickstarts/index.md)
- **Configure input formats**: [Configuration](../config/configuration.md)
- **Understand token matching**: [Concepts: Token Rules](../concepts/token-rules.md)
- **Read metadata format**: [Reference: Metadata Format](../reference/metadata-format.md)
- **Contribute improvements**: [Community: Contributing](../community/contributing.md)
