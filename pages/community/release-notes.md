---
layout: default
---

# Release Notes

Track notable changes, fixes, and enhancements for each OpenToken release.

---

## Version History

### v1.7.0 (Latest)

**Release Date**: 2024

**New Features**
- Added `ZIP3` attribute for partial postal code matching
- Introduced performance tuning configuration options
- PySpark integration for distributed token processing

**Improvements**
- Enhanced SSN validation patterns
- Improved birth date normalization for international formats
- Better error messages for validation failures

**Bug Fixes**
- Fixed race condition in multi-threaded token generation
- Resolved edge case in postal code normalization

---

### v1.6.0

**New Features**
- Added encrypted token output mode
- Introduced metadata JSON generation for audit trails
- Support for Parquet file format

**Improvements**
- Reduced memory footprint for large batch processing
- Faster CSV parsing with streaming iterators

---

### v1.5.0

**New Features**
- Five-tier token system (T1-T5) fully implemented
- Cross-language parity between Java and Python
- CLI tool for batch processing

**Improvements**
- Thread-safe attribute validation
- Pre-compiled regex patterns for performance

---

### v1.4.0

**New Features**
- Added `PostalCodeAttribute` with US/Canadian format support
- Introduced `AgeRangeValidator` for birth date validation

**Bug Fixes**
- Fixed diacritic removal in name normalization
- Corrected SSN area code validation

---

### v1.3.0

**New Features**
- `SocialSecurityNumberAttribute` with comprehensive validation
- `SexAttribute` for biological sex matching

**Improvements**
- Enhanced name normalization (title/suffix removal)
- Case-insensitive attribute matching

---

### v1.2.0

**New Features**
- `DateAttribute` base class for date handling
- `BirthDateAttribute` with date range validation

**Improvements**
- Thread-safe `DateTimeFormatter` usage
- Improved date parsing error messages

---

### v1.1.0

**New Features**
- `FirstNameAttribute` and `LastNameAttribute` implementation
- Basic regex validation framework

**Improvements**
- Modular attribute architecture
- ServiceLoader-based attribute discovery (Java)

---

### v1.0.0

**Initial Release**

- Core tokenization framework
- HMAC-SHA256 hashing pipeline
- AES-256 encryption support
- Basic CSV input/output
- Multi-language foundation (Java + Python)

---

## Interoperability Notes

Both Java and Python implementations are designed to produce **byte-identical tokens** for the same normalized input. This is verified by:

- Automated interoperability tests in `tools/interoperability/`
- SHA-256 hash comparison of outputs
- Cross-language test data validation

### Verifying Parity

```bash
# Run interoperability tests
cd tools/interoperability
python java_python_interoperability_test.py
```

---

## Upgrade Guide

### From v1.6.x to v1.7.x

No breaking changes. New features are additive.

### From v1.5.x to v1.6.x

- Update metadata handling if using custom parsers
- New Parquet dependency required for Parquet support

### From v1.4.x to v1.5.x

- Token rule definitions moved to `tokens/definitions/`
- Update import paths if using internal APIs

---

## Security Advisories

For security-related updates, see our [Security Policy](../security.md).

Report vulnerabilities to: [security@truveta.com](mailto:security@truveta.com)
