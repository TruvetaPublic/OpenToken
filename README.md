# OpenToken

Privacy-preserving tokenization and matching library for healthcare data. OpenToken generates deterministic, cryptographically secure tokens from person attributes (name, birthdate, SSN, etc.) so datasets can be matched without exposing raw identifiers.

## Overview

- **Multi-language parity**: Java and Python implementations produce identical token outputs
- **Deterministic tokens**: Same input always produces the same cryptographically secure token
- **Privacy-preserving**: Tokens cannot be reversed to recover original person data

## Why OpenToken

- Healthcare-grade validation and normalization for names, birthdates, SSN, postal codes, sex
- Secure pipeline: SHA-256 → HMAC-SHA256 → AES-256 (or hash-only mode)
- Multiple token rules (T1–T5) to increase match confidence across varied data quality

## Quickstart

**Docker/CLI workflow:**

```bash
./run-opentoken.sh \
  -i ./resources/sample.csv -t csv -o ./resources/output.csv \
  -h "HashingKey" -e "Secret-Encryption-Key-Goes-Here."
```

**Java CLI:**

```bash
cd lib/java && mvn clean install -DskipTests
java -jar opentoken-cli/target/opentoken-cli-*.jar \
  -i ../../resources/sample.csv -t csv -o ../../resources/output.csv \
  -h "HashingKey" -e "Secret-Encryption-Key-Goes-Here."
```

See [Quickstarts](docs/quickstarts/index.md) for Python CLI and detailed setup instructions.

## Key Matching Ideas

- **Token rules**: Five rules (T1–T5) combine attributes in different ways — see [Token Rules](docs/concepts/token-rules.md)
- **Normalization**: Names, dates, postal codes normalized before tokenization — see [Normalization and Validation](docs/concepts/normalization-and-validation.md)
- **Metadata**: Processing statistics and audit trail — see [Metadata Format](docs/reference/metadata-format.md)

## Running OpenToken

- **CLI modes**: Encrypt (default), hash-only (`--hash-only`), decrypt (`-d`) — see [Running OpenToken](docs/running-opentoken/index.md)
- **Docker**: Convenience scripts for containerized runs — see [Quickstarts](docs/quickstarts/index.md)
- **PySpark**: Distributed processing for large datasets — see [Spark or Databricks](docs/operations/spark-or-databricks.md)

## Security Notes

- **Crypto pipeline**: Token signature → SHA-256 → HMAC-SHA256 → AES-256 (or hash-only) — see [Security](docs/security.md)
- **Secret management**: Handle hashing/encryption secrets securely; avoid committing secrets; prefer env/secret stores
- **Validation**: Reject placeholders and malformed attributes before tokenization

## Contributing & Community

- [Contributing Guide](docs/community/contributing.md) — Branching, PR expectations, coding standards
- [Code of Conduct](docs/community/code-of-conduct.md)
- [Roadmap](docs/community/roadmap.md) and [Release Notes](docs/community/release-notes.md)

## Documentation

- [Documentation Index](docs/index.md)
- [Quickstarts](docs/quickstarts/index.md)
- [Specification](docs/specification.md)
- [CLI Reference](docs/reference/cli.md)
- [Metadata Format](docs/reference/metadata-format.md)

For issues or support, file an issue in this repository.