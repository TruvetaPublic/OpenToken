# OpenToken

Privacy-preserving tokenization and matching library for secure PII-based person linkage. OpenToken generates deterministic, cryptographically secure tokens from person attributes (name, birthdate, SSN, etc.) so datasets can be matched without exposing raw identifiers.

## Introduction

Our approach to person matching relies on building a set of matching tokens (or token signatures) per person which are derived from deterministic person data but preserve privacy by using cryptographically secure hashing algorithms.

- [OpenToken](#opentoken)
  - [Introduction](#introduction)
  - [Highlights](#highlights)
  - [Demo](#demo)
  - [Overview](#overview)
  - [Why OpenToken](#why-opentoken)
  - [Quickstart](#quickstart)
  - [Key Matching Ideas](#key-matching-ideas)
  - [Running OpenToken](#running-opentoken)
  - [Security Notes](#security-notes)
  - [Contributing \& Community](#contributing--community)
  - [Documentation](#documentation)

## Highlights

- Multi-language Support
- Cryptographically Secure encryption that prevents re-identification
- Enables straightforward person-matching by comparing 5 deterministic and unique tokens, providing a high degree of confidence in matches

## Demo

New to OpenToken? Start with the **[PPRL Superhero Demo](demos/pprl-superhero-example/)** — a beginner-friendly, end-to-end walkthrough showing how two parties (hospital and pharmacy) can privately find matching records without exposing raw identifiers.

The demo includes:

- **Interactive Jupyter notebook** with step-by-step explanations
- **One-command runner** (`run_end_to_end.sh`) for quick execution
- Synthetic superhero dataset generation
- Token generation and overlap analysis examples

Perfect for understanding privacy-preserving record linkage concepts before diving into production use.

## Overview

- **Multi-language parity**: Java and Python implementations produce identical token outputs
- **Deterministic tokens**: Same input always produces the same cryptographically secure token
- **Privacy-preserving**: Tokens cannot be reversed to recover original person data

## Why OpenToken

- Practical validation and normalization for common PII-derived attributes (names, birthdates, SSN, postal codes, sex)
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

See [Quickstarts](https://truvetapublic.github.io/OpenToken/quickstarts/) for Python CLI and detailed setup instructions.

## Key Matching Ideas

- **Token rules**: Five rules (T1–T5) combine attributes in different ways — see [Token Rules](https://truvetapublic.github.io/OpenToken/concepts/token-rules/)
- **Normalization**: Names, dates, postal codes normalized before tokenization — see [Normalization and Validation](https://truvetapublic.github.io/OpenToken/concepts/normalization-and-validation/)
- **Metadata**: Processing statistics and audit trail — see [Metadata Format](https://truvetapublic.github.io/OpenToken/reference/metadata-format/)

## Running OpenToken

- **CLI modes**: Encrypt (default), hash-only (`--hash-only`), decrypt (`-d`) — see [Running OpenToken](https://truvetapublic.github.io/OpenToken/running-opentoken/)
- **Docker**: Convenience scripts for containerized runs — see [Quickstarts](https://truvetapublic.github.io/OpenToken/quickstarts/)
- **PySpark**: Distributed processing for large datasets — see [Spark or Databricks](https://truvetapublic.github.io/OpenToken/operations/spark-or-databricks/)

## Security Notes

- **Crypto pipeline**: Token signature → SHA-256 → HMAC-SHA256 → AES-256 (or hash-only) — see [Security](https://truvetapublic.github.io/OpenToken/security/)
- **Secret management**: Handle hashing/encryption secrets securely; avoid committing secrets; prefer env/secret stores
- **Validation**: Reject placeholders and malformed attributes before tokenization

## Contributing & Community

- [Contributing Guide](https://truvetapublic.github.io/OpenToken/community/contributing/) — Branching, PR expectations, coding standards
- [Code of Conduct](https://truvetapublic.github.io/OpenToken/community/code-of-conduct/)

## Documentation

- [Documentation Index](https://truvetapublic.github.io/OpenToken/)
- [Quickstarts](https://truvetapublic.github.io/OpenToken/quickstarts/)
- [Specification](https://truvetapublic.github.io/OpenToken/specification/)
- [CLI Reference](https://truvetapublic.github.io/OpenToken/reference/cli/)
- [Metadata Format](https://truvetapublic.github.io/OpenToken/reference/metadata-format/)

For issues or support, file an issue in this repository.
