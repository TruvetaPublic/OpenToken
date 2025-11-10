# OpenToken Development Guide

This guide centralizes contributor-facing information. It covers local setup, language-specific build instructions, development environment, versioning, and key contribution workflows.

> **For AI Coding Agents**: See the [Copilot Instructions](../.github/copilot-instructions.md) for comprehensive guidance on working with this codebase, including security guidelines, PR standards, and debugging tips.

## At a Glance

- Two implementations: Java (Maven) & Python (pip)
- Deterministic token generation logic is equivalent across languages
- Use this guide for environment setup & day‑to‑day development
- Use the Token & Attribute Registration guide for extending functionality

## Contents

- [OpenToken Development Guide](#opentoken-development-guide)
  - [At a Glance](#at-a-glance)
  - [Contents](#contents)
  - [Prerequisites](#prerequisites)
  - [Project Layout](#project-layout)
  - [Language Development (Java \& Python)](#language-development-java--python)
    - [Java](#java)
    - [Python](#python)
    - [Cross-language Tips](#cross-language-tips)
  - [Token \& Attribute Registration](#token--attribute-registration)
    - [When to Use](#when-to-use)
    - [Java Registration (ServiceLoader SPI)](#java-registration-serviceloader-spi)
    - [Python Registration](#python-registration)
    - [Cross-language Parity Checklist](#cross-language-parity-checklist)
    - [Version Bump Reminder](#version-bump-reminder)
    - [Quick Reference](#quick-reference)
  - [Building \& Testing](#building--testing)
    - [Full Multi-language Build](#full-multi-language-build)
    - [Docker Image](#docker-image)
  - [Running the Tool (CLI)](#running-the-tool-cli)
  - [Development Container](#development-container)
  - [Version Bumping Policy](#version-bumping-policy)
  - [Contributing Checklist](#contributing-checklist)
  - [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Tool | Recommended Version | Notes |
| ---- | ------------------- | ----- |
| Java JDK | 21.x | Required for Java module & CLI JAR (outputs Java 11 compatible bytecode) |
| Maven | 3.8+ | Build Java artifacts (`mvn clean install`) |
| Python | 3.10+ | For Python implementation & scripts |
| pip / venv | Latest | Manage Python dependencies |
| Docker (optional) | Latest | Build container image |

## Project Layout

```text
lib/
  java/      # Java implementation
  python/    # Python implementation
resources/   # Sample and test data
tools/       # Utility scripts (hash calculator, mock data, etc.)
docs/        # All developer documentation (this file!)
```
Key Docs:

- Development processes below

## Language Development (Java & Python)

This section combines the previous standalone Java and Python development sections for easier cross-language parity review.

### Java

Prerequisites:

- Java 21 SDK or higher (JAR output is Java 11 compatible)
- Maven 3.8.8 or higher

Build (from project root):

```shell
cd lib/java/opentoken && mvn clean install
```
Or from `lib/java/opentoken` directly:

```shell
mvn clean install
```
Resulting JAR: `lib/java/opentoken/target/opentoken-*.jar`.

Using as a Maven dependency:

```xml
<dependency>
  <groupId>com.truveta</groupId>
  <artifactId>opentoken</artifactId>
  <version>${opentoken.version}</version>
</dependency>
```

CLI usage:

```shell
cd lib/java/opentoken && java -jar target/opentoken-*.jar [OPTIONS]
```
Arguments:

- `-i, --input <path>` Input file
- `-t, --type <csv|parquet>` Input type
- `-o, --output <path>` Output file
- `-ot, --output-type <type>` Optional output type
- `-h, --hashingsecret <secret>` HMAC-SHA256 secret
- `-e, --encryptionkey <key>` AES-256 key

Example:

```shell
cd lib/java/opentoken && java -jar target/opentoken-*.jar \
  -i src/test/resources/sample.csv -t csv -o target/output.csv \
  -h "HashingKey" -e "Secret-Encryption-Key-Goes-Here."
```

Programmatic API (simplified):

```java
List<TokenTransformer> transformers = Arrays.asList(
  new HashTokenTransformer("your-hashing-secret"),
  new EncryptTokenTransformer("your-encryption-key")
);
try (PersonAttributesReader reader = new PersonAttributesCSVReader("input.csv");
     PersonAttributesWriter writer = new PersonAttributesCSVWriter("output.csv")) {
  PersonAttributesProcessor.process(reader, writer, transformers, metadata);
}
```

Testing:

```shell
mvn test
mvn clean test jacoco:report   # Coverage in target/site/jacoco/index.html
```

Style & docs:

```shell
mvn checkstyle:check
mvn clean javadoc:javadoc
```

Notes:

- Large inputs may require additional heap (`-Xmx4g`).
- Unicode normalized to ASCII equivalents.

### Python

Prerequisites:

- Python 3.10+
- pip

Create & activate virtual environment (recommended):

```shell
cd lib/python/opentoken
python -m venv .venv
source .venv/bin/activate
```

Install dependencies:

```shell
pip install -r requirements.txt -r dev-requirements.txt
```

Editable install for local CLI usage:

```shell
pip install -e .
```

CLI usage (from project root):

```shell
PYTHONPATH=lib/python/opentoken/src/main python3 lib/python/opentoken/src/main/opentoken/main.py [OPTIONS]
```
Arguments mirror Java implementation.

Example:

```shell
PYTHONPATH=lib/python/opentoken/src/main python3 lib/python/opentoken/src/main/opentoken/main.py \
  -i resources/sample.csv -t csv -o lib/python/opentoken/target/output.csv \
  -h "HashingKey" -e "Secret-Encryption-Key-Goes-Here."
```

Programmatic API (simplified):

```python
transformers = [
    HashTokenTransformer("your-hashing-secret"),
    EncryptTokenTransformer("your-encryption-key")
]
with PersonAttributesCSVReader("input.csv") as reader, \
     PersonAttributesCSVWriter("output.csv") as writer:
    PersonAttributesProcessor.process(reader, writer, transformers, metadata)
```

Testing:

```shell
cd lib/python/opentoken
PYTHONPATH=src/main pytest src/test
```

Key dependencies: pandas, cryptography, (optional) pyarrow for Parquet.

Parity notes:

- Outputs identical tokens to Java for the same normalized input & secrets.
- Maintain consistency when adding new token or attribute logic.

Contributing notes:

- Follow PEP 8, add type hints.
- Keep tests in sync with Java changes.

### Cross-language Tips

| Task | Java Command | Python Command |
|------|--------------|----------------|
| Build / Package | `mvn clean install` | `pip install -e .` |
| Run Tests | `mvn test` | `pytest src/test` |
| Lint / Style | `mvn checkstyle:check` | (pep8 / flake8 if configured) |
| Run CLI | `java -jar target/opentoken-<ver>.jar ...` | `PYTHONPATH=... python ...main.py ...` |
| Add Token | SPI entry & class | new module in `tokens/definitions` |
| Add Attribute | SPI entry & class | class + loader import |

Maintain the same functional behavior and normalization between languages.

## Token & Attribute Registration

This section unifies Java and Python guidance for adding new Tokens and Attributes.

### When to Use

- Adding a new token generation rule (Token)
- Adding a new source person attribute (Attribute)
- Refactoring or renaming existing implementations

### Java Registration (ServiceLoader SPI)

Java uses the standard `ServiceLoader` discovery mechanism.

Steps (Token example):

1. Create class in `com.truveta.opentoken.tokens.definitions` extending `Token`.
2. Implement required abstract methods (identifier, definition, etc.).
3. Add fully qualified class name to: `lib/java/opentoken/src/main/resources/META-INF/services/com.truveta.opentoken.tokens.Token` (one per line).
4. Run `mvn clean install` and add/adjust tests.

Attribute steps are identical except:

- Class extends `com.truveta.opentoken.attributes.Attribute` (e.g., in `attributes.person`).
- Register in: `lib/java/opentoken/src/main/resources/META-INF/services/com.truveta.opentoken.attributes.Attribute`.

Guidelines:

- No blank lines or comments in service files.
- Keep entries sorted alphabetically (recommended for diffs).
- Update service file if class is renamed/moved.

Troubleshooting:

- Not loading? Check for: typo in service file, missing no-arg constructor, class not public, duplicate class names.

### Python Registration

Python uses two mechanisms:

1. Dynamic discovery for Tokens in `opentoken/tokens/definitions`.
2. Explicit inclusion for Attributes via `attribute_loader.py`.

Add a Token:

1. Create `lib/python/opentoken/src/main/opentoken/tokens/definitions/t6_token.py` (example).
2. Define a class inheriting `Token` with `get_identifier()` & `get_definition()`.
3. Ensure file and class names are unique and public.
4. Run `pytest src/test` to verify auto-discovery.

Add an Attribute:

1. Create module, e.g., `opentoken/attributes/person/middle_name_attribute.py`.
2. Implement subclass of `Attribute`.
3. In `attribute_loader.py`, import the class and add an instance inside `AttributeLoader.load()`.

Python Troubleshooting:

- If a Token isn’t picked up: ensure directory has `__init__.py` and class file matches naming conventions.
- If an Attribute isn’t loaded: confirm it’s imported and added to the returned set.

### Cross-language Parity Checklist

- Same normalization logic unaffected.
- Matching token definitions (order & components) across Java & Python.
- Tests confirming identical hash/encryption output for shared fixtures.

### Version Bump Reminder
Adding or modifying Tokens / Attributes requires a version bump (`bump2version minor` for new features, `patch` for fixes, `major` for breaking changes).

### Quick Reference

| Operation | Java File(s) | Python File(s) |
|-----------|--------------|----------------|
| Add Token | `META-INF/services/...Token` | `tokens/definitions/<new>_token.py` |
| Add Attribute | `META-INF/services/...Attribute` | `attributes/.../<new>_attribute.py` + `attribute_loader.py` |
| Rename Implementation | Update service file entries | Rename file & ensure loader/discovery still finds it |

Maintain tests to guard consistency between languages.

## Building & Testing

### Full Multi-language Build

(Useful in CI or before PR submission.)

```shell
# Java
(cd lib/java/opentoken && mvn clean install)

# Python
(cd lib/python/opentoken && pytest src/test)
```

### Docker Image

```shell
docker build . -t opentoken
```

## Running the Tool (CLI)

Minimum required arguments:

```shell
java -jar target/opentoken-*.jar -i input.csv -t csv -o output.csv -h HashingKey -e Secret-Encryption-Key-Goes-Here.
```

Arguments:

| Flag | Description |
| ---- | ----------- |
| `-t, --type` | Input file type (`csv` or `parquet`) |
| `-i, --input` | Input file path |
| `-o, --output` | Output file path |
| `-ot, --output-type` | (Optional) Output file type (defaults to input) |
| `-h, --hashingsecret` | Hashing secret for HMAC-SHA256 |
| `-e, --encryptionkey` | AES-256 encryption key |

## Development Container

A Dev Container configuration provides a reproducible environment with:

- JDK 21 (with JDK 11 and 17 also available)
- Maven
- Python & tooling

Open the repository in VS Code and select: "Reopen in Container".

## Version Bumping Policy

All PRs MUST bump the version via `bump2version` (never edit versions manually):

- Bug fix / docs tweak: `bump2version patch`
- Backward-compatible feature: `bump2version minor`
- Breaking change: `bump2version major`

Ensure the working tree is clean before running the command.

## Contributing Checklist

Before opening a PR:

- [ ] Code compiles (`mvn clean install` for Java)
- [ ] Tests pass (Java & Python where changes apply)
- [ ] Added/updated docs if behavior changed
- [ ] Followed style guidelines (Checkstyle / Python conventions)
- [ ] Added registration entries (Java SPI files) or loader entries (Python) if new Token/Attribute
- [ ] Bumped version with `bump2version`

## Troubleshooting

| Issue | Hint |
| ----- | ---- |
| Java class not discovered | Confirm fully qualified name in `META-INF/services/*` file & no trailing spaces |
| Python attribute not loaded | Ensure it is imported & added in `attribute_loader.py` |
| Token mismatch between languages | Verify hashing & encryption secrets are identical and normalization logic unchanged |
| Build fails on Checkstyle | Run `mvn -q checkstyle:check` locally & fix warnings |


---
Maintainers: Keep this guide updated when changing build, versioning, or extension workflows.