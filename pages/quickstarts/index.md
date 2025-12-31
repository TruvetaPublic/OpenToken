---
layout: default
---

# Quickstarts

Get OpenToken running in minutes. Choose your preferred environment and language.

## 30-Second Overview

OpenToken generates secure tokens from person data:

```bash
# Input: sample.csv with columns: FirstName, LastName, BirthDate, Sex, PostalCode, SSN
# Output: encrypted tokens for person matching

java -jar opentoken-cli-*.jar \
  -i sample.csv -t csv -o output.csv \
  -h "YourHashingKey" -e "YourEncryptionKey"
```

## Option 1: Docker (Recommended for Quick Testing)

No Java or Python install needed. Just Docker.

### Bash/Linux/Mac:

```bash
cd /path/to/OpenToken

./run-opentoken.sh \
  -i ./resources/sample.csv \
  -o ./resources/output.csv \
  -t csv \
  -h "HashingKey" \
  -e "Secret-Encryption-Key-Goes-Here."
```

### PowerShell/Windows:

```powershell
cd C:\path\to\OpenToken

.\run-opentoken.ps1 `
  -i .\resources\sample.csv `
  -o .\resources\output.csv `
  -FileType csv `
  -h "HashingKey" `
  -e "Secret-Encryption-Key-Goes-Here."
```

The script automatically builds the image and runs it. **That's it!**

For options (skip rebuild, verbose output, Parquet format): Run with `--help` (Bash) or `-Help` (PowerShell).

---

## Option 2: Java CLI

Requires Java 21+ and Maven.

```bash
cd lib/java

# Build the CLI
mvn clean install -DskipTests

# Generate tokens
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  -i ../../resources/sample.csv \
  -t csv \
  -o ../../resources/output.csv \
  -h "HashingKey" \
  -e "Secret-Encryption-Key-Goes-Here."

# View output
cat ../../resources/output.csv
cat ../../resources/output.metadata.json
```

---

## Option 3: Python CLI

Requires Python 3.10+.

```bash
cd lib/python

# Create and activate virtual environment (at repo root)
python -m venv ../../.venv
source ../../.venv/bin/activate  # On Windows: .\.venv\Scripts\activate

# Install dependencies
cd opentoken && pip install -r requirements.txt -e .
cd ../opentoken-cli && pip install -r requirements.txt -e .

# Generate tokens
python -m opentoken_cli.main \
  -i ../../../resources/sample.csv \
  -t csv \
  -o ../../../resources/output.csv \
  -h "HashingKey" \
  -e "Secret-Encryption-Key-Goes-Here."

# View output
cat ../../../resources/output.csv
cat ../../../resources/output.metadata.json
```

---

## What Just Happened?

Your `output.csv` now contains:

```
RecordId,RuleId,Token
id1,T1,Gn7t1Zj16E5Qy+z9iINtczP6fRDYta6C0XFrQtpjnVQSEZ5pQXAzo02Aa9LS9oNMOog6Ssw9GZE6fvJrX2sQ/cThSkB6m91L
id1,T2,pUxPgYL9+cMxkA+8928Pil+9W+dm9kISwHYPdkZS+I2nQ/bQ/8HyL3FOVf3NYPW5NKZZO1OZfsz7LfKYpTlaxyzMLqMF2Wk7
...
```

The `output.metadata.json` contains:

```json
{
  "TotalRows": 1,
  "TotalRowsWithInvalidAttributes": 0,
  "OpenTokenVersion": "1.0.0",
  "JavaVersion": "21.0.0",
  "Platform": "Java",
  "HashingSecretHash": "...",
  "EncryptionSecretHash": "...",
  "BlankTokensByRule": {},
  "InvalidAttributesByType": {}
}
```

---

## Test Your Data

Don't have sample data? Generate mock data:

```bash
cd tools/mockdata

# Create 100 test records
python data_generator.py 100 0.05 test_data.csv

# Then process it with OpenToken
# (use the CLI commands above, pointing to test_data.csv)
```

---

## Next Steps

- **Understand matching**: Read [Concepts: Token Rules](../concepts/token-rules.md)
- **Explore input formats**: See [Configuration](../config/configuration.md)
- **Decrypt tokens**: See [Decrypting Tokens](../operations/decrypting-tokens.md)
- **Advanced: PySpark**: See [Spark or Databricks](../operations/spark-or-databricks.md)
- **Troubleshooting**: See [Running OpenToken](../running-opentoken/index.md)

## Input File Requirements

Your CSV must have these columns (any of the listed aliases work):

| Column     | Aliases                      | Required | Example                  |
| ---------- | ---------------------------- | -------- | ------------------------ |
| FirstName  | GivenName                    | Yes      | John                     |
| LastName   | Surname                      | Yes      | Doe                      |
| BirthDate  | DateOfBirth                  | Yes      | 1975-03-15 or 03/15/1975 |
| Sex        | Gender                       | Yes      | Male, Female, M, F       |
| PostalCode | ZipCode, ZIP3, ZIP4, ZIP5    | Yes      | 98004                    |
| SSN        | NationalIdentificationNumber | Yes      | 123456789 or 123-45-6789 |
| RecordId   | Id                           | Optional | patient_id_123           |

**Note**: RecordId is optional. If omitted, a unique UUID is auto-generated for each record.

See [Configuration](../config/configuration.md) for detailed column mapping and format options.

---

## Common Issues

**"Encryption key not provided"**  
→ Either add `-e "YourKey"` or use `--hash-only` for hash-only mode.

**"Invalid BirthDate"**  
→ Use YYYY-MM-DD format or one of the accepted formats. Date must be between 1910-01-01 and today.

**"Docker image not found"**  
→ The script builds it automatically. Make sure you have Docker running.

For more troubleshooting, see [Running OpenToken](../running-opentoken/index.md).
