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

| Tier | Confidence  | Use Case                       |
| ---- | ----------- | ------------------------------ |
| T1   | Highest     | Regulatory compliance, billing |
| T2   | High        | Clinical record linkage        |
| T3   | Medium-High | Research cohorts               |
| T4   | Medium      | Population studies             |
| T5   | Lower       | Broad matching, deduplication  |

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

## Worked Example: From Person Data to Tokens and Matches

This section walks through a complete example: raw input data → normalization → token generation → matching decisions.

### Sample Dataset

Consider four fictional patient records from two different healthcare systems:

| RecordId | FirstName | LastName   | BirthDate  | Sex    | PostalCode | SSN         |
| -------- | --------- | ---------- | ---------- | ------ | ---------- | ----------- |
| HOS-101  | María     | García Jr. | 03/22/1988 | Female | 90210      | 452-38-7291 |
| HOS-102  | tom       | O'Reilly   | 1995-11-03 | M      | 30301-4455 | 671-82-9134 |
| CLN-201  | Maria     | Garcia     | 1988-03-22 | F      | 90210      | 452-38-7291 |
| CLN-202  | Thomas    | O'Reilly   | 11/03/1995 | Male   | 30301      | —           |

**HOS-** records come from a hospital system; **CLN-** records come from a clinic.

### Step 1: Normalization

OpenToken normalizes each field before token generation. For full rules, see [Normalization and Validation](normalization-and-validation.md).

| RecordId | FirstName | LastName | BirthDate  | Sex | PostalCode | SSN         |
| -------- | --------- | -------- | ---------- | --- | ---------- | ----------- |
| HOS-101  | MARIA     | GARCIA   | 1988-03-22 | F   | 90210      | 452-38-7291 |
| HOS-102  | TOM       | O'REILLY | 1995-11-03 | M   | 30301      | 671-82-9134 |
| CLN-201  | MARIA     | GARCIA   | 1988-03-22 | F   | 90210      | 452-38-7291 |
| CLN-202  | THOMAS    | O'REILLY | 1995-11-03 | M   | 30301      | —           |

**What changed:**

- **María → MARIA**: Diacritic removed, uppercased
- **García Jr. → GARCIA**: Suffix removed, diacritic removed, uppercased
- **tom → TOM**: Uppercased
- **03/22/1988 → 1988-03-22**: Date reformatted to ISO 8601
- **30301-4455 → 30301**: ZIP+4 truncated to 5 digits
- **SSN missing (CLN-202)**: Noted; T1 and T3 will be skipped for this record

### Step 2: Token Generation

Each record produces up to five tokens (T1–T5). Tokens are Base64-encoded hashes; the examples below are illustrative placeholders with realistic lengths (~88 characters for encrypted tokens).

For detailed rule compositions, see [Token Rules](token-rules.md).

**HOS-101 (María García, 1988-03-22):**

| Rule | Token Signature                    | Illustrative Token       |
| ---- | ---------------------------------- | ------------------------ |
| T1   | `452-38-7291\|1988-03-22\|F`       | `Xk9mT2pLc1VhR3dNZUZ...` |
| T2   | `MARIA\|GARCIA\|1988-03-22\|F`     | `bHdRa0VuWXBCdkxhTnI...` |
| T3   | `MARIA\|GARCIA\|452-38-7291`       | `cTdYc1pNdkpUa2JQeHo...` |
| T4   | `MARIA\|GARCIA\|1988-03-22\|90210` | `ZnBOdFdtS2haQWdWcko...` |
| T5   | `MAR\|GARCIA\|1988-03-22\|F`       | `RWtqVXhMY0dTcldmbVk...` |

**CLN-201 (Maria Garcia, 1988-03-22):**

| Rule | Token Signature                    | Illustrative Token       |
| ---- | ---------------------------------- | ------------------------ |
| T1   | `452-38-7291\|1988-03-22\|F`       | `Xk9mT2pLc1VhR3dNZUZ...` |
| T2   | `MARIA\|GARCIA\|1988-03-22\|F`     | `bHdRa0VuWXBCdkxhTnI...` |
| T3   | `MARIA\|GARCIA\|452-38-7291`       | `cTdYc1pNdkpUa2JQeHo...` |
| T4   | `MARIA\|GARCIA\|1988-03-22\|90210` | `ZnBOdFdtS2haQWdWcko...` |
| T5   | `MAR\|GARCIA\|1988-03-22\|F`       | `RWtqVXhMY0dTcldmbVk...` |

**Observation:** HOS-101 and CLN-201 produce **identical tokens** for all five rules because their normalized attributes are identical.

**HOS-102 (tom O'Reilly, 1995-11-03):**

| Rule | Token Signature                    | Illustrative Token       |
| ---- | ---------------------------------- | ------------------------ |
| T1   | `671-82-9134\|1995-11-03\|M`       | `UXdlcnR5VWlPcEFzRGZ...` |
| T2   | `TOM\|O'REILLY\|1995-11-03\|M`     | `WnhjdmJubUtMbUpIR2d...` |
| T3   | `TOM\|O'REILLY\|671-82-9134`       | `QWxza2RqZmhHa0xQb1p...` |
| T4   | `TOM\|O'REILLY\|1995-11-03\|30301` | `TW5iVmN4WmFRd0VyVHl...` |
| T5   | `TOM\|O'REILLY\|1995-11-03\|M`     | `SWp1aHlHdEZyRGVTd1d...` |

**CLN-202 (Thomas O'Reilly, 1995-11-03, no SSN):**

| Rule | Token Signature                       | Illustrative Token       |
| ---- | ------------------------------------- | ------------------------ |
| T1   | — (SSN missing)                       | *Not generated*          |
| T2   | `THOMAS\|O'REILLY\|1995-11-03\|M`     | `RHZiTmNYemFRd0VyWnR...` |
| T3   | — (SSN missing)                       | *Not generated*          |
| T4   | `THOMAS\|O'REILLY\|1995-11-03\|30301` | `S2p1aHlHdEZyRGVWd1h...` |
| T5   | `THO\|O'REILLY\|1995-11-03\|M`        | `VXl0ckVXcUFzRGZHaEp...` |

**Observation:** HOS-102 and CLN-202 do **not** match on any rule:

- T1, T3: CLN-202 missing SSN
- T2, T4, T5: First name differs (TOM vs THOMAS) → different token signatures

### Step 3: Matching Decisions

When comparing tokens across the two systems:

| Record Pair       | T1  | T2  | T3  | T4  | T5  | Match?              |
| ----------------- | --- | --- | --- | --- | --- | ------------------- |
| HOS-101 ↔ CLN-201 | ✓   | ✓   | ✓   | ✓   | ✓   | **Yes** (all rules) |
| HOS-102 ↔ CLN-202 | —   | ✗   | —   | ✗   | ✗   | **No**              |
| HOS-101 ↔ HOS-102 | ✗   | ✗   | ✗   | ✗   | ✗   | No                  |
| CLN-201 ↔ CLN-202 | ✗   | ✗   | ✗   | ✗   | ✗   | No                  |

**Interpretation:**

- **HOS-101 and CLN-201 are the same person.** Despite surface differences ("María" vs "Maria", suffix "Jr."), normalization produces identical attributes, so all tokens match.
- **HOS-102 and CLN-202 are likely the same person but do not match.** "Tom" and "Thomas" normalize differently; without an SSN on CLN-202, there's no high-confidence link. T5 might have matched if both used "THO" (first 3 characters), but "TOM" ≠ "THO".

### Key Takeaways

1. **Normalization is critical.** Two records with superficially different inputs (accents, suffixes, date formats) can match perfectly after normalization.
2. **Missing attributes reduce matching opportunities.** CLN-202 couldn't use T1 or T3 because SSN was missing.
3. **Name variations may prevent matches.** "Tom" vs "Thomas" is a common real-world issue; T5's 3-character prefix helps only if the first 3 letters are identical.
4. **Multiple rules provide fallback.** Even if T1 fails (SSN missing), T2/T4/T5 may still match if other attributes align.

### Related Pages

- [Token Rules](token-rules.md) — Detailed composition of T1–T5
- [Normalization and Validation](normalization-and-validation.md) — Full normalization and validation rules

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
