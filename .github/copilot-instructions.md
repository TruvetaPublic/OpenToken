# OpenToken AI Coding Agent Instructions

## Overview

This document provides comprehensive guidance for AI coding agents working on the OpenToken project. Follow these instructions to ensure code quality, consistency, and compatibility across both Java and Python implementations.

### Task Suitability

**Good Tasks for AI Agents:**
- Adding new attributes (with validation/normalization)
- Adding new validation rules
- Bug fixes in existing attributes/validators
- Test coverage improvements
- Documentation updates
- Code refactoring within existing patterns

**Tasks Requiring Human Review:**
- Changes to core token generation logic
- Modifications to encryption/hashing algorithms
- Breaking API changes
- New token rules (T6+)
- Multi-language implementation changes

## Architecture Overview

**OpenToken** is a dual-implementation (Java/Python) library for privacy-preserving person matching using cryptographically secure token generation. Tokens are generated from deterministic person attributes (name, birthdate, SSN, etc.) using 5 distinct token rules (T1-T5). Both implementations must produce **identical tokens** for the same normalized input.

### Core Components

- **Attributes** (`lib/java/.../attributes/`, `lib/python/.../attributes/`): Person data fields with validation & normalization (e.g., `BirthDateAttribute`, `SocialSecurityNumberAttribute`)
- **Validators** (`validation/`): Composable validation rules (regex, date ranges, age ranges) applied during attribute processing
- **Tokens** (`tokens/`): Rules defining which attributes combine to form each of the 5 token types (T1-T5)
- **I/O Readers/Writers** (`io/`): CSV and Parquet file processors with streaming support
- **Token Transformers** (`tokentransformer/`): HMAC-SHA256 hashing + AES-256 encryption pipeline

### Registration Pattern (Critical)

**Java uses ServiceLoader SPI** - new attributes/tokens require:

1. Implement interface (e.g., extend `BaseAttribute`)
2. Add fully-qualified class name to `lib/java/src/main/resources/META-INF/services/com.truveta.opentoken.{attributes.Attribute|tokens.Token}`
3. Keep entries sorted alphabetically (one per line, no blank lines/comments)

**Python uses explicit imports** in loader files:

- `lib/python/src/main/opentoken/attributes/attribute_loader.py` → add to `AttributeLoader.load()` set
- `lib/python/src/main/opentoken/tokens/token_registry.py` → add to registry

**Both languages must be updated** or parity breaks. Use `tools/java_python_syncer.py` to verify cross-language sync.

## Development Workflows

### Build & Test

```bash
# Java (from lib/java/): Maven handles compile, Checkstyle, JaCoCo coverage, sanity checks
cd lib/java && mvn clean install

# Python (from lib/python/): Creates venv, installs deps, runs pytest
cd lib/python && python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt -r dev-requirements.txt -e .
pytest

# Note: No unified build script exists - build each language separately
```

### Version Bumping (MANDATORY for ALL PRs)

```bash
bump2version patch   # Bug fixes, minor changes (e.g., adding invalid SSN patterns)
bump2version minor   # New attributes, new token rules
bump2version major   # Breaking API changes
```

This updates `.bumpversion.cfg`, `pom.xml`, `setup.py`, `__init__.py`, `Dockerfile`, and `Metadata.java` automatically. **Never** manually edit version numbers.

### Branch Naming

`dev/<github-username>/<feature-description>` (e.g., `dev/mattwise-42/additional-attributes`)

## Project-Specific Conventions

### Attribute Development Pattern

1. **Extend `BaseAttribute`** (Java) or `SerializableAttribute` (Python)
2. **Validators are composable**: Pass list to super constructor (Java) or init validators in `__init__` (Python)
   - Example: `BirthDateAttribute` extends `DateAttribute` and adds `DateRangeValidator(LocalDate.of(1910, 1, 1), true)`
3. **Normalization happens before validation**: `normalize()` must handle edge cases (whitespace, case, diacritics)
4. **Thread-safety required**: Use `DateTimeFormatter` (Java) not `SimpleDateFormat`; avoid mutable shared state
5. **Test pattern**: Include serialization test, thread-safety test (100 threads), boundary value tests

### Test Structure

- **Java**: JUnit 5, tests mirror `src/main/` structure in `src/test/`
  - Integration tests: `PersonAttributesProcessorIntegrationTest.java` validates full pipeline
  - Sanity checks: Maven Antrun plugin runs CSV/Parquet end-to-end after build
- **Python**: pytest, uses `test_*.py` naming, includes interoperability tests against Java output
  - Hash calculator tests: `tools/test_hash_calculator.py` ensures token computation matches Java

### Validation Rules (Critical Business Logic)

- **SSN**: Area ≠ `000|666|900-999`, Group ≠ `00`, Serial ≠ `0000`, reject common patterns (`111-11-1111`, etc.)
- **BirthDate**: Range `1910-01-01` to today, normalized to `yyyy-MM-dd`
- **Name normalization**: Remove titles/suffixes, strip diacritics, uppercase for token generation
- **PostalCode**: US ZIP (5/9 digits), Canadian (`A1A 1A1` format), reject placeholders (`00000`, `12345`)

### Metadata Generation

Every token generation run produces `.metadata.json` with:

- Processing stats (valid/invalid counts per attribute)
- SHA-256 hashes of secrets (for audit, NOT the secrets themselves)
- System info (Java version, library version, timestamp)
  See `docs/metadata-format.md` for schema.

### Cross-Language Parity Requirements

- Token outputs must be **byte-identical** for same input (verified by `tools/interoperability/` tests)
- Normalization logic must match exactly (e.g., diacritic removal, case conversion)
- Update `tools/java-python-mapping.json` when adding new classes
- Run `tools/sync-check.sh` before PR submission

## File Structure Patterns

```
lib/java/src/main/java/com/truveta/opentoken/
├── attributes/
│   ├── general/        # DateAttribute, StringAttribute, RecordIdAttribute
│   ├── person/         # BirthDateAttribute, SexAttribute, SSN, etc.
│   └── validation/     # RegexValidator, DateRangeValidator, AgeRangeValidator
├── io/                 # CSV/Parquet readers & writers (streaming iterators)
├── tokens/             # Token interface, TokenRegistry, definitions/ (T1-T5)
└── tokentransformer/   # HashTokenTransformer, EncryptTokenTransformer

lib/python/src/main/opentoken/  # Mirrors Java structure with Pythonic naming
```

## Common Pitfalls

1. **Forgetting service registration**: Java won't discover attributes without `META-INF/services` entry
2. **Python loader not updated**: `AttributeLoader.load()` returns hardcoded set, not auto-discovered
3. **Validation vs normalization order**: Always normalize first, then validate the normalized value
4. **Thread-safety in validators**: Pre-compile regex patterns, avoid mutable state
5. **Checkstyle failures**: Run `mvn checkstyle:check` separately to catch before full build

## Documentation Requirements

- **JavaDoc**: Required for all public classes/methods (Checkstyle enforces)
- **Python docstrings**: Follow Google style (Args, Returns, Raises)
- **README.md updates**: Add new attributes to acceptance table, update token rules if changed
- **CHANGELOG**: Implicit via PR descriptions and version bumps

## Security Guidelines

### Secrets and Sensitive Data

- **Never commit secrets**: Hashing keys and encryption keys must only appear in test files with dummy values
- **Test data only**: Use placeholder values like `"HashingKey"` or `"Secret-Encryption-Key-Goes-Here."` in examples
- **Metadata files**: Contain SHA-256 hashes of secrets (for audit), not the secrets themselves
- **Validation patterns**: SSN validation logic is public but never log/expose actual SSN values

### Dependency Management

- **Java dependencies**: Declared in `pom.xml`, must pass security scans via GitHub Dependabot
- **Python dependencies**: Managed in `requirements.txt` and `dev-requirements.txt`
- **Version pinning**: Pin major versions, allow minor/patch updates (`~=` for Python, ranges for Maven)
- **Vulnerability scanning**: Both implementations use automated security scans (see `.github/workflows/`)

## Git Workflow & PR Standards

### Before Submitting

1. **Run all builds**: `mvn clean install` (Java) and `pytest` (Python)
2. **Check cross-language sync**: Run `tools/java_python_syncer.py`
3. **Version bump**: Use `bump2version` (patch/minor/major)
4. **Code style**: Java Checkstyle must pass, Python follows PEP 8
5. **Test coverage**: Add tests for new code paths

### PR Checklist

- [ ] Both Java and Python implementations updated (if applicable)
- [ ] Tests added/updated for changes
- [ ] Version bumped appropriately
- [ ] Documentation updated (README, JavaDoc, docstrings)
- [ ] Service registration files updated (Java: `META-INF/services/`, Python: loaders)
- [ ] No secrets or sensitive data committed
- [ ] CI/CD pipelines updated if necessary (GitHub Actions workflows, build configurations)
- [ ] All CI checks passing

### Commit Message Format

```
<type>: <short summary>

<detailed description if needed>

- Specific change 1
- Specific change 2
```

Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`

## Debugging & Troubleshooting

### Common Build Issues

**Java Checkstyle failures:**
```bash
cd lib/java && mvn checkstyle:check
```
Fix style issues before running full build.

**Python import errors:**
```bash
cd lib/python && source .venv/bin/activate
pip install -e .
```
Ensure editable install for local development.

**Token mismatch between Java/Python:**
- Verify normalization logic matches exactly
- Check attribute order in token signatures
- Use `tools/interoperability/` tests to compare outputs

### Testing Strategies

**Unit tests**: Test individual attributes/validators in isolation
**Integration tests**: `PersonAttributesProcessorIntegrationTest.java` tests full pipeline
**Interoperability tests**: Verify Java/Python produce identical tokens
**Sanity checks**: Maven runs end-to-end CSV/Parquet tests post-build

### Performance Considerations

- **Thread-safety**: All validators must be thread-safe (pre-compile regex patterns)
- **Streaming**: I/O readers use iterators, not loading entire files into memory
- **Batch processing**: Process large datasets in chunks for memory efficiency

## Code Review Standards

### What to Look For

1. **Correctness**: Logic matches requirements, edge cases handled
2. **Parity**: Java and Python implementations produce identical results
3. **Security**: No secrets committed, validation prevents injection
4. **Performance**: No unnecessary allocations, thread-safe patterns
5. **Documentation**: Public APIs documented, complex logic explained
6. **Tests**: New code has corresponding tests, tests are meaningful

### Red Flags

- Missing service registration (Java won't discover new attributes)
- Python loader not updated (hardcoded set won't include new classes)
- Validation after normalization (must validate normalized values)
- Mutable shared state in validators (causes race conditions)
- Breaking changes without major version bump