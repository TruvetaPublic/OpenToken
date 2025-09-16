# Interoperability Tests

This directory contains tests that validate compatibility and consistency between the Java and Python implementations of OpenToken.

## Prerequisites

- Python 3.9 or higher
- pip (Python package installer)

- Java 11 SDK or higher

## Test Categories

- **Token Generation Compatibility**: Verify both implementations generate identical tokens for the same input
- **Data Format Compatibility**: Ensure serialized data can be read cross-platform
- **Encryption/Decryption Compatibility**: Validate encrypted tokens from both implementations can be decrypted using the same decryptor tool
- **Metadata Consistency**: Check that metadata formats are consistent between implementations

## Running Tests

These tests require both Java and Python environments to be properly configured.

```bash
# Run all interoperability tests
python -m pytest tests/interoperability/ -v

# Run specific test categories
python -m pytest tests/interoperability/test_token_compatibility.py -v
```

## Test Data

Shared test data and expected outputs are stored in the `test_data/` subdirectory.
