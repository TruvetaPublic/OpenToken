---
layout: default
---

# Token Rules

OpenToken generates five distinct token types (T1-T5), each combining different attributes to balance precision and recall in person matching.

---

## Overview

Each token rule defines:
- **Required attributes**: Must all be present and valid
- **Concatenation order**: Determines the token hash input
- **Use case**: When this rule is most effective

```
Token = HMAC-SHA256(attribute1 + "|" + attribute2 + "|" + ... | key)
```

---

## T1: SSN + Birth Date + Sex

**Purpose:** Highest confidence matching using government ID

### Composition

| Attribute | Required | Example |
|-----------|----------|---------|
| SSN | Yes | `078-05-1120` |
| BirthDate | Yes | `1985-03-15` |
| Sex | Yes | `M` |

### Token Formula

```
T1 = Hash(SSN | BirthDate | Sex)
```

### Characteristics

- **Precision**: Highest (very low false positive rate)
- **Recall**: Lower (SSN often missing)
- **Best for**: Regulatory compliance, insurance matching, billing reconciliation

### Example

```
Input:
  SSN: 078-05-1120
  BirthDate: 1985-03-15
  Sex: M

Normalized:
  SSN: 078-05-1120
  BirthDate: 1985-03-15
  Sex: M

Token Input: "078-05-1120|1985-03-15|M"
Token (hashed): a3f2e8d4c1b5...
```

---

## T2: First Name + Last Name + Birth Date + Sex

**Purpose:** Name-based matching when SSN unavailable

### Composition

| Attribute | Required | Example |
|-----------|----------|---------|
| FirstName | Yes | `JOHN` |
| LastName | Yes | `SMITH` |
| BirthDate | Yes | `1985-03-15` |
| Sex | Yes | `M` |

### Token Formula

```
T2 = Hash(FirstName | LastName | BirthDate | Sex)
```

### Characteristics

- **Precision**: High
- **Recall**: Good (common fields)
- **Best for**: Clinical record linkage, patient matching

### Example

```
Input:
  FirstName: John
  LastName: Smith Jr.
  BirthDate: 03/15/1985
  Sex: Male

Normalized:
  FirstName: JOHN
  LastName: SMITH
  BirthDate: 1985-03-15
  Sex: M

Token Input: "JOHN|SMITH|1985-03-15|M"
Token (hashed): b7c4d9e2f5a1...
```

---

## T3: First Name + Last Name + SSN

**Purpose:** Match when birth date is uncertain or inconsistent

### Composition

| Attribute | Required | Example |
|-----------|----------|---------|
| FirstName | Yes | `JOHN` |
| LastName | Yes | `SMITH` |
| SSN | Yes | `078-05-1120` |

### Token Formula

```
T3 = Hash(FirstName | LastName | SSN)
```

### Characteristics

- **Precision**: High
- **Recall**: Moderate (requires SSN)
- **Best for**: Records with DOB discrepancies

### Example

```
Input:
  FirstName: John
  LastName: Smith
  SSN: 078-05-1120

Normalized:
  FirstName: JOHN
  LastName: SMITH
  SSN: 078-05-1120

Token Input: "JOHN|SMITH|078-05-1120"
Token (hashed): c8d5e1f3a2b4...
```

---

## T4: First Name + Last Name + Birth Date + Postal Code

**Purpose:** Geographic-aware matching

### Composition

| Attribute | Required | Example |
|-----------|----------|---------|
| FirstName | Yes | `JOHN` |
| LastName | Yes | `SMITH` |
| BirthDate | Yes | `1985-03-15` |
| PostalCode | Yes | `98101` |

### Token Formula

```
T4 = Hash(FirstName | LastName | BirthDate | PostalCode)
```

### Characteristics

- **Precision**: Medium-high
- **Recall**: Good (no SSN needed)
- **Best for**: Local/regional matching, address-based studies

### Example

```
Input:
  FirstName: John
  LastName: Smith
  BirthDate: 1985-03-15
  PostalCode: 98101-1234

Normalized:
  FirstName: JOHN
  LastName: SMITH
  BirthDate: 1985-03-15
  PostalCode: 98101

Token Input: "JOHN|SMITH|1985-03-15|98101"
Token (hashed): d9e6f2a4b5c7...
```

---

## T5: First Name (3 chars) + Last Name + Birth Date + Sex

**Purpose:** Fuzzy name matching with partial first name

### Composition

| Attribute | Required | Example |
|-----------|----------|---------|
| FirstName (first 3) | Yes | `JOH` |
| LastName | Yes | `SMITH` |
| BirthDate | Yes | `1985-03-15` |
| Sex | Yes | `M` |

### Token Formula

```
T5 = Hash(FirstName[0:3] | LastName | BirthDate | Sex)
```

### Characteristics

- **Precision**: Lower
- **Recall**: Higher (tolerates name variations)
- **Best for**: Deduplication, finding potential matches

### Example

```
Input:
  FirstName: Jonathan
  LastName: Smith
  BirthDate: 1985-03-15
  Sex: M

Normalized:
  FirstName (3 chars): JON
  LastName: SMITH
  BirthDate: 1985-03-15
  Sex: M

Token Input: "JON|SMITH|1985-03-15|M"
Token (hashed): e1f3a5b7c9d2...
```

Note: "John", "Jonathan", "Johnny" all produce `JOH` → same T5 token.

---

## Token Rule Summary

| Rule | Attributes | Precision | Recall | Primary Use |
|------|------------|-----------|--------|-------------|
| T1 | SSN, DOB, Sex | ★★★★★ | ★★ | Regulatory |
| T2 | Name, DOB, Sex | ★★★★ | ★★★★ | Clinical |
| T3 | Name, SSN | ★★★★ | ★★★ | DOB issues |
| T4 | Name, DOB, ZIP | ★★★ | ★★★★ | Geographic |
| T5 | Name(3), DOB, Sex | ★★ | ★★★★★ | Dedup |

---

## Attribute Fallback

When a required attribute is missing or invalid:

1. **Token not generated** for that rule
2. Other rules still computed if their attributes are valid
3. Metadata records which tokens were generated

```json
{
  "recordId": "12345",
  "tokensGenerated": ["T2", "T4", "T5"],
  "tokensSkipped": {
    "T1": "SSN_MISSING",
    "T3": "SSN_MISSING"
  }
}
```

---

## Custom Token Rules

OpenToken supports defining custom token rules:

### Java

```java
public class CustomToken extends BaseToken {
    @Override
    public String getName() {
        return "T6";
    }
    
    @Override
    public List<String> getRequiredAttributes() {
        return List.of("FirstName", "LastName", "MRN");
    }
}
```

### Python

```python
class CustomToken(BaseToken):
    def get_name(self):
        return "T6"
    
    def get_required_attributes(self):
        return ["FirstName", "LastName", "MRN"]
```

See [Token Registration](../reference/token-registration.md) for registration details.

---

## Further Reading

- [Matching Model](matching-model.md) - Matching strategies
- [Normalization](normalization-and-validation.md) - Attribute standardization
- [Security](../security.md) - Cryptographic details
