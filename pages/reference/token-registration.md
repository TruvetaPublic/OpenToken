---
layout: default
---

# Token and Attribute Registration

This guide explains how to register custom tokens and attributes in both Java and Python implementations to ensure cross-language parity.

## Overview

OpenToken uses different registration mechanisms:

| Language | Mechanism | Location |
|----------|-----------|----------|
| Java | ServiceLoader SPI | `META-INF/services/` |
| Python | Explicit imports | Loader classes |

**Critical**: Both implementations must be updated together to maintain cross-language compatibility.

## Java Registration (ServiceLoader SPI)

Java uses the ServiceLoader pattern for runtime discovery.

### Registering a New Attribute

1. **Create the attribute class** extending `BaseAttribute`:

```java
package com.truveta.opentoken.attributes.person;

import com.truveta.opentoken.attributes.general.StringAttribute;
import com.truveta.opentoken.attributes.validation.RegexValidator;

import java.util.List;

/**
 * Middle name attribute with standard string normalization.
 */
public class MiddleNameAttribute extends StringAttribute {
    public MiddleNameAttribute() {
        super("MiddleName", List.of(
            new RegexValidator("^[A-Za-z\\s\\-']+$", "Invalid middle name format")
        ));
    }
}
```

2. **Register in service file** at `lib/java/opentoken/src/main/resources/META-INF/services/com.truveta.opentoken.attributes.Attribute`:

```
com.truveta.opentoken.attributes.general.DateAttribute
com.truveta.opentoken.attributes.general.RecordIdAttribute
com.truveta.opentoken.attributes.general.StringAttribute
com.truveta.opentoken.attributes.person.BirthDateAttribute
com.truveta.opentoken.attributes.person.FirstNameAttribute
com.truveta.opentoken.attributes.person.LastNameAttribute
com.truveta.opentoken.attributes.person.MiddleNameAttribute
com.truveta.opentoken.attributes.person.PostalCodeAttribute
com.truveta.opentoken.attributes.person.SexAttribute
com.truveta.opentoken.attributes.person.SocialSecurityNumberAttribute
```

**Rules for service files:**
- One fully-qualified class name per line
- Keep entries **alphabetically sorted**
- No blank lines or comments
- No trailing whitespace

### Registering a New Token Rule

1. **Create the token class** implementing `Token`:

```java
package com.truveta.opentoken.tokens.definitions;

import com.truveta.opentoken.tokens.Token;

import java.util.List;

/**
 * Token rule T6 - Example custom token.
 */
public class T6Token implements Token {
    @Override
    public String getRuleId() {
        return "T6";
    }

    @Override
    public List<String> getRequiredAttributes() {
        return List.of("FirstName", "LastName", "BirthDate", "PostalCode");
    }
}
```

2. **Register in service file** at `lib/java/opentoken/src/main/resources/META-INF/services/com.truveta.opentoken.tokens.Token`:

```
com.truveta.opentoken.tokens.definitions.T1Token
com.truveta.opentoken.tokens.definitions.T2Token
com.truveta.opentoken.tokens.definitions.T3Token
com.truveta.opentoken.tokens.definitions.T4Token
com.truveta.opentoken.tokens.definitions.T5Token
com.truveta.opentoken.tokens.definitions.T6Token
```

## Python Registration (Explicit Imports)

Python uses explicit imports in loader classes.

### Registering a New Attribute

1. **Create the attribute class**:

```python
# lib/python/opentoken/src/main/opentoken/attributes/person/middle_name_attribute.py

from opentoken.attributes.general.string_attribute import StringAttribute
from opentoken.attributes.validation.regex_validator import RegexValidator

class MiddleNameAttribute(StringAttribute):
    """Middle name attribute with standard string normalization."""
    
    def __init__(self):
        super().__init__(
            name="MiddleName",
            validators=[
                RegexValidator(r"^[A-Za-z\s\-']+$", "Invalid middle name format")
            ]
        )
```

2. **Register in AttributeLoader**:

Edit `lib/python/opentoken/src/main/opentoken/attributes/attribute_loader.py`:

```python
from opentoken.attributes.person.first_name_attribute import FirstNameAttribute
from opentoken.attributes.person.last_name_attribute import LastNameAttribute
from opentoken.attributes.person.middle_name_attribute import MiddleNameAttribute  # Add import
from opentoken.attributes.person.birth_date_attribute import BirthDateAttribute
# ... other imports

class AttributeLoader:
    @staticmethod
    def load():
        return {
            FirstNameAttribute(),
            LastNameAttribute(),
            MiddleNameAttribute(),  # Add to set
            BirthDateAttribute(),
            # ... other attributes
        }
```

### Registering a New Token Rule

1. **Create the token class**:

```python
# lib/python/opentoken/src/main/opentoken/tokens/definitions/t6_token.py

from opentoken.tokens.token import Token

class T6Token(Token):
    """Token rule T6 - Example custom token."""
    
    @property
    def rule_id(self) -> str:
        return "T6"
    
    def get_required_attributes(self) -> list:
        return ["FirstName", "LastName", "BirthDate", "PostalCode"]
```

2. **Register in TokenRegistry**:

Edit `lib/python/opentoken/src/main/opentoken/tokens/token_registry.py`:

```python
from opentoken.tokens.definitions.t1_token import T1Token
from opentoken.tokens.definitions.t2_token import T2Token
from opentoken.tokens.definitions.t3_token import T3Token
from opentoken.tokens.definitions.t4_token import T4Token
from opentoken.tokens.definitions.t5_token import T5Token
from opentoken.tokens.definitions.t6_token import T6Token  # Add import

class TokenRegistry:
    def __init__(self):
        self._tokens = [
            T1Token(),
            T2Token(),
            T3Token(),
            T4Token(),
            T5Token(),
            T6Token(),  # Add to list
        ]
```

## Cross-Language Sync Verification

### Using the Sync Tool

After making changes in both languages, verify parity:

```bash
cd /workspaces/OpenToken/tools
python java_language_syncer.py
```

This tool checks:
- Attribute names match between Java and Python
- Token rules are identical
- Normalization logic produces same outputs
- Registration files are complete

### Interoperability Testing

Run the interoperability test suite:

```bash
cd /workspaces/OpenToken/tools/interoperability
python java_python_interoperability_test.py
```

This verifies that identical inputs produce identical token outputs in both languages.

## File Locations Summary

### Java Files

| Type | Location |
|------|----------|
| Attribute classes | `lib/java/opentoken/src/main/java/com/truveta/opentoken/attributes/` |
| Token classes | `lib/java/opentoken/src/main/java/com/truveta/opentoken/tokens/definitions/` |
| Attribute service file | `lib/java/opentoken/src/main/resources/META-INF/services/com.truveta.opentoken.attributes.Attribute` |
| Token service file | `lib/java/opentoken/src/main/resources/META-INF/services/com.truveta.opentoken.tokens.Token` |

### Python Files

| Type | Location |
|------|----------|
| Attribute classes | `lib/python/opentoken/src/main/opentoken/attributes/` |
| Token classes | `lib/python/opentoken/src/main/opentoken/tokens/definitions/` |
| Attribute loader | `lib/python/opentoken/src/main/opentoken/attributes/attribute_loader.py` |
| Token registry | `lib/python/opentoken/src/main/opentoken/tokens/token_registry.py` |

## Common Mistakes

### Java

❌ **Forgetting service file entry**
```
# Attribute won't be discovered at runtime!
```

❌ **Unsorted service file**
```
com.truveta.opentoken.attributes.person.SexAttribute
com.truveta.opentoken.attributes.person.BirthDateAttribute  # Should be sorted
```

❌ **Blank lines or comments in service file**
```
com.truveta.opentoken.attributes.person.BirthDateAttribute

# This comment breaks ServiceLoader
com.truveta.opentoken.attributes.person.FirstNameAttribute
```

### Python

❌ **Forgetting loader import**
```python
# AttributeLoader.load() won't include the new attribute!
```

❌ **Wrong import path**
```python
from opentoken.attributes.middle_name_attribute import MiddleNameAttribute  # Wrong!
from opentoken.attributes.person.middle_name_attribute import MiddleNameAttribute  # Correct
```

### Both Languages

❌ **Updating only one language**
```
# Cross-language parity broken!
# Java has MiddleNameAttribute, Python doesn't
```

❌ **Different attribute names**
```java
// Java: "MiddleName"
// Python: "middle_name"
// These will NOT match!
```

## Checklist for New Attributes/Tokens

Before submitting a PR with new attributes or tokens:

- [ ] Java class created with proper inheritance
- [ ] Java service file updated (sorted alphabetically)
- [ ] Python class created with matching logic
- [ ] Python loader/registry updated
- [ ] Unit tests added for both languages
- [ ] Sync tool passes: `python tools/java_language_syncer.py`
- [ ] Interoperability tests pass
- [ ] Documentation updated

## Next Steps

- [Java API Reference](java-api.md) - Java class details
- [Python API Reference](python-api.md) - Python module details
- [CLI Reference](cli.md) - Command-line options
