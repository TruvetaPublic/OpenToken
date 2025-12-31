---
layout: default
---

# Reference: API Overview

OpenToken provides three interfaces for generating privacy-preserving tokens: a Java library, a Python library, and a command-line interface (CLI). All three produce identical tokens for the same input, enabling cross-language and cross-platform workflows.

---

## Choosing the Right Interface

| Interface      | Best for                                                                    | Example use case                                                         |
| -------------- | --------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| **Java API**   | JVM-based pipelines, enterprise integrations, high-throughput batch jobs    | Embedding token generation in a Spring or Spark (Scala/Java) application |
| **Python API** | Python data workflows, PySpark, Databricks notebooks, rapid prototyping     | Tokenizing DataFrames in a Jupyter notebook or Databricks cluster        |
| **CLI**        | One-off batch processing, scripted pipelines, CI/CD jobs, Docker containers | Processing CSV/Parquet files from a shell script or scheduled job        |

---

## Java Library API

The Java API integrates directly into JVM applications.

**Key classes:**

- `PersonAttributes` — Builder pattern for person data
- `TokenRegistry` — Access to T1–T5 token rules
- `HashTokenTransformer` / `EncryptTokenTransformer` — Token transformation

**Quick example:**

```java
PersonAttributes person = new PersonAttributes.Builder()
    .recordId("rec_001")
    .firstName("Elena")
    .lastName("Vasquez")
    .birthDate("1992-07-14")
    .sex("Female")
    .postalCode("30301")
    .socialSecurityNumber("452-38-7291")
    .build();

TokenRegistry registry = new TokenRegistry();
EncryptTokenTransformer transformer = new EncryptTokenTransformer(hashingSecret, encryptionKey);

for (Token token : registry.getAllTokens()) {
    String signature = token.getSignature(person);
    String encrypted = transformer.transform(signature);
    System.out.println(token.getRuleId() + ": " + encrypted);
}
```

**Full reference:** [Java API Reference](java-api.md)

---

## Python Library API

The Python API mirrors the Java API for cross-language parity.

**Key classes:**

- `PersonAttributes` — Constructor-based person data
- `TokenRegistry` — Access to T1–T5 token rules
- `HashTokenTransformer` / `EncryptTokenTransformer` — Token transformation

**Quick example:**

```python
from opentoken.attributes.person_attributes import PersonAttributes
from opentoken.tokens.token_registry import TokenRegistry
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer

person = PersonAttributes(
    record_id="rec_001",
    first_name="Elena",
    last_name="Vasquez",
    birth_date="1992-07-14",
    sex="Female",
    postal_code="30301",
    social_security_number="452-38-7291"
)

registry = TokenRegistry()
transformer = EncryptTokenTransformer(hashing_secret, encryption_key)

for token in registry.get_all_tokens():
    signature = token.get_signature(person)
    encrypted = transformer.transform(signature)
    print(f"{token.rule_id}: {encrypted}")
```

**Full reference:** [Python API Reference](python-api.md)

---

## Command-Line Interface (CLI)

The CLI processes CSV or Parquet files without writing code.

**Basic usage:**

```bash
java -jar opentoken-cli-*.jar \
  -i input.csv -t csv -o output.csv \
  -h "HashingSecret" -e "EncryptionKey32Chars!!!!!!!!!!"
```

Or with Python:

```bash
python -m opentoken_cli.main \
  -i input.csv -t csv -o output.csv \
  -h "HashingSecret" -e "EncryptionKey32Chars!!!!!!!!!!"
```

**Key options:**

| Flag                      | Purpose                        |
| ------------------------- | ------------------------------ |
| `-i` / `--input`          | Input file path                |
| `-o` / `--output`         | Output file path               |
| `-t` / `--type`           | File type (`csv` or `parquet`) |
| `-h` / `--hashing-secret` | HMAC-SHA256 secret             |
| `-e` / `--encryption-key` | AES-256 key (32 chars)         |
| `--hash-only`             | Skip encryption                |

**Full reference:** [CLI Reference](cli.md)

---

## Metadata Output

Every token generation run produces a `.metadata.json` file alongside the token output. This file contains:

- Processing statistics (total rows, invalid records)
- SHA-256 hashes of secrets (for verification, not the secrets themselves)
- Timestamp and platform information

**Full reference:** [Metadata Format](metadata-format.md)

---

## Custom Token Registration

OpenToken supports defining custom token rules beyond T1–T5. Custom rules can include additional attributes (e.g., MRN) or different attribute combinations.

**Full reference:** [Token Registration](token-registration.md)

---

## Additional Reference Pages

- [Java API Reference](java-api.md) — Complete Java class and method documentation
- [Python API Reference](python-api.md) — Complete Python class and method documentation
- [CLI Reference](cli.md) — All CLI flags, modes, and examples
- [Metadata Format](metadata-format.md) — Metadata file schema and fields
- [Token Registration](token-registration.md) — Adding custom token rules

---

## Related Documentation

- [Concepts: Token Rules](../concepts/token-rules.md) — How T1–T5 are composed
- [Concepts: Normalization](../concepts/normalization-and-validation.md) — Attribute standardization
- [Configuration](../config/configuration.md) — Environment variables and input formats
- [Security](../security.md) — Cryptographic details and key management
