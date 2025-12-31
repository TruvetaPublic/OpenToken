---
layout: default
---

# Python Quickstart

Install the Python packages and run the OpenToken CLI with a virtual environment.

## Prerequisites

- **Python 3.10+**
- **pip** (usually included with Python)

Verify your installation:
```bash
python --version   # Should show 3.10 or higher
pip --version
```

## Setup Virtual Environment

**Important:** The virtual environment should be created at the repository root.

```bash
cd /path/to/OpenToken

# Create virtual environment at repo root
python -m venv .venv

# Activate (Linux/Mac)
source .venv/bin/activate

# Activate (Windows)
.\.venv\Scripts\activate
```

## Install Dependencies

```bash
# Install core library
cd lib/python/opentoken
pip install -r requirements.txt -e .

# Install CLI
cd ../opentoken-cli
pip install -r requirements.txt -e .
```

## Run Token Generation

### Basic Encrypted Tokens

```bash
python -m opentoken_cli.main \
  -i ../../../resources/sample.csv \
  -t csv \
  -o ../../../resources/output.csv \
  -h "YourHashingSecret" \
  -e "YourEncryptionKey-32Chars-Here!"
```

### Hash-Only Mode (No Encryption)

```bash
python -m opentoken_cli.main \
  -i ../../../resources/sample.csv \
  -t csv \
  -o ../../../resources/output.csv \
  -h "YourHashingSecret" \
  --hash-only
```

### Parquet Format

```bash
python -m opentoken_cli.main \
  -i input.parquet \
  -t parquet \
  -o output.parquet \
  -h "YourHashingSecret" \
  -e "YourEncryptionKey-32Chars-Here!"
```

## Verify Output

```bash
# View token output
head ../../../resources/output.csv

# View metadata
cat ../../../resources/output.metadata.json
```

## Using the Python API Programmatically

```python
from opentoken.attributes.person_attributes import PersonAttributes
from opentoken.tokens.token_registry import TokenRegistry
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer

# Create person attributes
person = PersonAttributes(
    record_id="patient_123",
    first_name="John",
    last_name="Doe",
    birth_date="1980-01-15",
    sex="Male",
    postal_code="98004",
    social_security_number="123-45-6789"
)

# Check validation
if not person.is_valid():
    print(f"Invalid attributes: {person.get_invalid_attributes()}")

# Generate tokens
registry = TokenRegistry()
transformer = EncryptTokenTransformer(
    hashing_secret="HashingSecret",
    encryption_key="EncryptionKey-32Characters-Here"
)

for token in registry.get_all_tokens():
    signature = token.get_signature(person)
    encrypted_token = transformer.transform(signature)
    print(f"{token.rule_id}: {encrypted_token}")
```

## Cross-Language Parity

OpenToken guarantees that Java and Python produce **identical tokens** for the same input. This is verified by interoperability tests:

```bash
cd tools/interoperability
pip install -r requirements.txt
python java_python_interoperability_test.py
```

The test:
1. Generates tokens using Java CLI
2. Generates tokens using Python CLI
3. Compares all tokens byte-by-byte
4. Fails if any mismatch is found

## PySpark Integration

For distributed processing on Spark or Databricks:

```bash
cd lib/python/opentoken-pyspark
pip install -r requirements.txt -e .
```

See [Spark or Databricks](../operations/spark-or-databricks.md) for usage.

## Troubleshooting

### "ModuleNotFoundError: No module named 'opentoken'"
Make sure you installed with `-e .` (editable mode) from the correct directory.

### "Python version not supported"
OpenToken requires Python 3.10+. Check with `python --version`.

### Virtual Environment Not Activated
If commands fail, ensure venv is active:
```bash
source /path/to/OpenToken/.venv/bin/activate
```

### Import Errors After Updates
Reinstall the packages:
```bash
pip install -e . --force-reinstall
```

## Development Setup

For contributing to OpenToken:

```bash
# Install development dependencies
pip install -r dev-requirements.txt

# Run tests
pytest

# Run with coverage
pytest --cov=opentoken --cov-report=html
```

## Next Steps

- [Java Quickstart](java-quickstart.md) - Cross-language reference
- [CLI Reference](../reference/cli.md) - All command options
- [Python API Reference](../reference/python-api.md) - Programmatic usage
- [Spark Integration](../operations/spark-or-databricks.md) - Distributed processing
