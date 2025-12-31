---
layout: default
---

# Matching Model

OpenToken uses a multi-rule tokenization strategy to enable privacy-preserving person matching across healthcare datasets.

---

## Overview

The matching model generates cryptographically secure tokens from personal identifiers (PII) without exposing the underlying data. Different token rules balance **precision** (fewer false positives) against **recall** (fewer missed matches).

```
┌─────────────────────────────────────────────────────────────────┐
│                    Person Record (PII)                          │
│  Name, DOB, SSN, Sex, Postal Code                              │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Normalization                                │
│  - Remove titles/suffixes                                       │
│  - Strip diacritics                                             │
│  - Standardize formats                                          │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Validation                                   │
│  - Date ranges                                                  │
│  - SSN patterns                                                 │
│  - Required fields                                              │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                Token Generation (T1-T5)                         │
│  - Concatenate attributes per rule                              │
│  - HMAC-SHA256 hash                                             │
│  - Optional AES-256 encryption                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Why Multiple Token Rules?

Real-world healthcare data is messy:
- Names may have typos or variations
- Dates may be recorded differently
- SSNs may be missing or partially known
- Addresses change over time

Using **five distinct rules** allows matching at different confidence levels:

| Tier | Confidence | Use Case |
|------|------------|----------|
| T1 | Highest | Regulatory compliance, billing |
| T2 | High | Clinical record linkage |
| T3 | Medium-High | Research cohorts |
| T4 | Medium | Population studies |
| T5 | Lower | Broad matching, deduplication |

---

## Token Rules Summary

### T1: SSN-Based (Highest Confidence)

```
SSN + BirthDate + Sex
```

The most restrictive rule. Requires SSN to be present and valid.

**Strengths**: Very low false positive rate
**Limitations**: SSN often missing in healthcare data

---

### T2: Full Name + Demographics

```
FirstName + LastName + BirthDate + Sex
```

Uses full name without SSN requirement.

**Strengths**: Good precision with common data elements
**Limitations**: Name variations cause misses

---

### T3: Name + SSN (No DOB)

```
FirstName + LastName + SSN
```

Useful when birth date is uncertain.

**Strengths**: Handles DOB discrepancies
**Limitations**: Requires SSN

---

### T4: Name + DOB + Location

```
FirstName + LastName + BirthDate + PostalCode
```

Incorporates geographic information.

**Strengths**: Good for local matching
**Limitations**: Address changes over time

---

### T5: Partial Name + Demographics

```
FirstName (first 3 chars) + LastName + BirthDate + Sex
```

Uses only first 3 characters of first name.

**Strengths**: Robust to first name variations
**Limitations**: Lower precision

---

## Matching Strategies

### Union Matching (High Recall)

Match if **any** token rule matches:

```
Match = T1 ∨ T2 ∨ T3 ∨ T4 ∨ T5
```

Use for: Research studies, broad population analysis

### Intersection Matching (High Precision)

Match only if **multiple** rules match:

```
Match = (T1 ∨ T2) ∧ (T3 ∨ T4)
```

Use for: Clinical record linkage, regulatory requirements

### Tiered Matching

Apply rules in sequence, stopping at first match:

```python
for rule in [T1, T2, T3, T4, T5]:
    if rule.matches(record_a, record_b):
        return (True, rule.confidence)
return (False, None)
```

Use for: Adaptive matching based on data quality

---

## Collision Resistance

Token security relies on:

1. **HMAC-SHA256**: Cryptographically secure hash function
2. **Secret keys**: Organization-specific hashing keys
3. **Normalization**: Consistent input formatting
4. **AES-256**: Optional encryption layer

Two different people producing the same token (collision) is statistically negligible when:
- Keys are properly managed
- Input data is correctly normalized

---

## Cross-Organization Matching

For records from different organizations to match:

1. **Same keys**: Both organizations must use identical hashing/encryption keys
2. **Same normalization**: Attribute processing must be identical
3. **Same rules**: Token generation logic must match exactly

OpenToken ensures this through:
- Dual Java/Python implementations with byte-identical outputs
- Comprehensive normalization documentation
- Interoperability testing

---

## Further Reading

- [Token Rules](token-rules.md) - Detailed rule specifications
- [Normalization](normalization-and-validation.md) - How attributes are standardized
- [Security Model](../security.md) - Cryptographic guarantees
