# OpenToken Python Implementation

This is the Python implementation of the OpenToken library

## Prerequisites

- Python 3.9 or higher
- pip (Python package installer)

## Installation

### Installing Dependencies

```shell
# From the python directory
pip install -r requirements.txt
```

### Development Installation

```shell
# Install in development mode
pip install -e .
```

## Usage

### Command Line Interface

```shell
python src/main/main.py [OPTIONS]
```

**Arguments:**
- `-i, --input <path>`: Input file path
- `-t, --type <type>`: Input file type (`csv` or `parquet`)
- `-o, --output <path>`: Output file path
- `-ot, --output-type <type>`: Output file type (optional, defaults to input type)
- `-h, --hashingsecret <secret>`: Hashing secret for HMAC-SHA256
- `-e, --encryptionkey <key>`: Encryption key for AES-256

**Example:**
From Root
```shell
PYTHONPATH=lib/python/src/main  python3 lib/python/src/main/opentoken/main.py -i lib/python/src/test/resources/sample.csv -t csv -o lib/python/target/output.csv -h "HashingKey"  -e "Secret-Encryption-Key-Goes-Here."
```
From lib/python

```shell
$ PYTHONPATH=src/main python3 src/main/opentoken/main.py -i src/test/resources/sample.csv -t csv -o target/output.csv -h "HashingKey"  -e "Secret-Encryption-Key-Goes-Here." 
```

### Programmatic API

```python
from opentoken.processor import PersonAttributesProcessor
from opentoken.io.csv import PersonAttributesCSVReader, PersonAttributesCSVWriter
from opentoken.tokentransformer import HashTokenTransformer, EncryptTokenTransformer

# Initialize token transformers
transformers = [
    HashTokenTransformer("your-hashing-secret"),
    EncryptTokenTransformer("your-encryption-key")
]

# Process data
with PersonAttributesCSVReader("input.csv") as reader, \
     PersonAttributesCSVWriter("output.csv") as writer:
    
    PersonAttributesProcessor.process(reader, writer, transformers, metadata)
```

## Testing

Running all tests from lib/python
```shell
PYTHONPATH=src/main pytest src/test
```

## Development

### Project Structure

```
src/
├── main/opentoken/
│   ├── attributes/          # Person attribute definitions
│   ├── io/                  # Input/output handlers
│   ├── processor/           # Core processing logic
│   ├── tokens/              # Token generation
│   ├── tokentransformer/    # Token transformation
│   └── main.py              # CLI entry point
└── test/
    ├── opentoken/           # Unit tests
    └── resources/           # Test data
```

### Virtual Environment

It's recommended to use a virtual environment:

```shell
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Supported Input Formats

- **CSV**: Comma-separated values with required columns
- **Parquet**: Apache Parquet format (requires pyarrow)

## Output

The library generates:
- **Token file**: Contains RecordId, TokenId, and Token columns
- **Metadata file**: JSON file with processing statistics and system information

## Dependencies

Key dependencies include:
- `pandas`: Data manipulation and CSV handling
- `cryptography`: Encryption and hashing
- `pyarrow`: Parquet file support (optional)

## Compatibility with Java Implementation

The Python implementation produces identical tokens to the Java version when using the same secrets and input data. Cross-compatibility tests are available in the test suite.

## Contributing

1. Follow PEP 8 coding standards
2. Add type hints for new functions
3. Add unit tests for new features
4. Ensure compatibility with Java implementation

## Support

For Python-specific issues, please check:
1. Python version compatibility (3.11+)
2. Virtual environment setup
3. Dependency versions in requirements.txt

For general questions, see the main project README.
