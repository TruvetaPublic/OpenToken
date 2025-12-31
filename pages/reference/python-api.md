---
layout: default
---

# Python API Reference

Document the Python modules and functions for programmatic token generation.

## Core Modules

```python
from opentoken.attributes.person_attributes import PersonAttributes
from opentoken.tokens.token_registry import TokenRegistry
from opentoken.tokentransformer.hash_token_transformer import HashTokenTransformer
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer
```

## PersonAttributes

Represents a single person's attributes for token generation.

### Constructor

```python
person = PersonAttributes(
    record_id="patient_123",
    first_name="John",
    last_name="Doe",
    birth_date="1980-01-15",
    sex="Male",
    postal_code="98004",
    social_security_number="123-45-6789"
)
```

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `record_id` | `str` | Record identifier |
| `first_name` | `str` | Normalized first name |
| `last_name` | `str` | Normalized last name |
| `birth_date` | `str` | Birth date (YYYY-MM-DD) |
| `sex` | `str` | Normalized sex (MALE/FEMALE) |
| `postal_code` | `str` | Normalized postal code |
| `social_security_number` | `str` | Normalized SSN |

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `is_valid()` | `bool` | Returns True if all attributes pass validation |
| `get_invalid_attributes()` | `List[str]` | Returns list of invalid attribute names |

### Validation Example

```python
person = PersonAttributes(
    first_name="John",
    last_name="D",  # Too short - invalid
    birth_date="2050-01-01",  # Future date - invalid
    sex="Male",
    postal_code="98004",
    social_security_number="000-00-0000"  # Invalid SSN
)

if not person.is_valid():
    for attr in person.get_invalid_attributes():
        print(f"Invalid: {attr}")

# Output:
# Invalid: LastName
# Invalid: BirthDate
# Invalid: SocialSecurityNumber
```

## TokenRegistry

Provides access to all token rules (T1–T5).

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `get_all_tokens()` | `List[Token]` | Returns all 5 token rules |
| `get_token(rule_id)` | `Token` | Returns specific token rule by ID |

### Example

```python
registry = TokenRegistry()

# Get all tokens
tokens = registry.get_all_tokens()
for token in tokens:
    print(token.rule_id)
# Output: T1, T2, T3, T4, T5

# Get specific token
t1 = registry.get_token("T1")
```

## Token Class

Represents a single token rule.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `rule_id` | `str` | Rule identifier (T1-T5) |

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `get_signature(person)` | `str` | Generates token signature from attributes |
| `get_required_attributes()` | `List[str]` | Returns attributes used by this rule |

### Example

```python
t1 = registry.get_token("T1")

# Get signature for a person
signature = t1.get_signature(person)
# Example: "DOE|J|MALE|1980-01-15"

# Check required attributes
required = t1.get_required_attributes()
# ["LastName", "FirstName", "Sex", "BirthDate"]
```

## Token Transformers

Transform token signatures into encrypted or hashed tokens.

### HashTokenTransformer

One-way hashing without encryption.

```python
hasher = HashTokenTransformer("YourHashingSecret")

signature = "DOE|J|MALE|1980-01-15"
hashed_token = hasher.transform(signature)
# Returns: Base64-encoded HMAC-SHA256 hash
```

### EncryptTokenTransformer

Full encryption with AES-256-GCM.

```python
encryptor = EncryptTokenTransformer(
    hashing_secret="YourHashingSecret",
    encryption_key="YourEncryptionKey-32Characters!"  # Exactly 32 chars
)

signature = "DOE|J|MALE|1980-01-15"
encrypted_token = encryptor.transform(signature)
# Returns: Base64-encoded encrypted token
```

## Complete Example

```python
from opentoken.attributes.person_attributes import PersonAttributes
from opentoken.tokens.token_registry import TokenRegistry
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer

def generate_tokens():
    # Create person
    person = PersonAttributes(
        record_id="patient_001",
        first_name="John",
        last_name="Doe",
        birth_date="1980-01-15",
        sex="Male",
        postal_code="98004",
        social_security_number="123-45-6789"
    )

    # Validate
    if not person.is_valid():
        print(f"Invalid attributes: {person.get_invalid_attributes()}")
        return

    # Setup transformer
    transformer = EncryptTokenTransformer(
        hashing_secret="HashingSecret",
        encryption_key="EncryptionKey-32Characters-Here"
    )

    # Generate all tokens
    registry = TokenRegistry()
    for token in registry.get_all_tokens():
        signature = token.get_signature(person)
        encrypted = transformer.transform(signature)
        print(f"{person.record_id},{token.rule_id},{encrypted}")

if __name__ == "__main__":
    generate_tokens()
```

## Batch Processing

For processing multiple records:

```python
import csv
from opentoken.attributes.person_attributes import PersonAttributes
from opentoken.tokens.token_registry import TokenRegistry
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer

def process_csv(input_path, output_path, hashing_secret, encryption_key):
    registry = TokenRegistry()
    transformer = EncryptTokenTransformer(hashing_secret, encryption_key)
    
    with open(input_path, 'r') as infile, open(output_path, 'w', newline='') as outfile:
        reader = csv.DictReader(infile)
        writer = csv.writer(outfile)
        writer.writerow(['RecordId', 'RuleId', 'Token'])
        
        for row in reader:
            person = PersonAttributes(
                record_id=row.get('RecordId', ''),
                first_name=row['FirstName'],
                last_name=row['LastName'],
                birth_date=row['BirthDate'],
                sex=row['Sex'],
                postal_code=row['PostalCode'],
                social_security_number=row['SSN']
            )
            
            if person.is_valid():
                for token in registry.get_all_tokens():
                    signature = token.get_signature(person)
                    encrypted = transformer.transform(signature)
                    writer.writerow([person.record_id, token.rule_id, encrypted])
```

## PySpark Integration

For distributed processing:

```python
from opentoken_pyspark import OpenTokenUDF

# Create UDF
opentoken_udf = OpenTokenUDF(
    hashing_secret="HashingSecret",
    encryption_key="EncryptionKey-32Characters-Here"
)

# Apply to DataFrame
df_tokens = df.withColumn(
    "tokens",
    opentoken_udf.generate_tokens(
        df.FirstName, df.LastName, df.BirthDate,
        df.Sex, df.PostalCode, df.SSN
    )
)
```

See [Spark or Databricks](../operations/spark-or-databricks.md) for details.

## Cross-Language Parity

OpenToken guarantees identical output between Java and Python:

```python
# This Python code produces the exact same tokens as equivalent Java code
person = PersonAttributes(
    first_name="John",
    last_name="Doe",
    birth_date="1980-01-15",
    sex="Male",
    postal_code="98004",
    social_security_number="123-45-6789"
)

# Token will match Java output byte-for-byte
```

Verify parity with:
```bash
cd tools/interoperability
python java_python_interoperability_test.py
```

## Error Handling

```python
try:
    person = PersonAttributes(
        first_name="",  # Empty - will be invalid
        last_name="Doe",
        birth_date="invalid-date",  # Bad format
        sex="Unknown",  # Not Male/Female
        postal_code="98004",
        social_security_number="123-45-6789"
    )
    
    if not person.is_valid():
        errors = person.get_invalid_attributes()
        raise ValueError(f"Invalid attributes: {errors}")
        
except ValueError as e:
    print(f"Validation error: {e}")
```

## Installation

```bash
# From repository
cd lib/python/opentoken
pip install -e .

# Development dependencies
pip install -r dev-requirements.txt
```

## Next Steps

- [Java API Reference](java-api.md) - Cross-language reference
- [Token Registration](token-registration.md) - Add custom tokens
- [CLI Reference](cli.md) - Command-line usage
- [Spark Integration](../operations/spark-or-databricks.md) - Distributed processing
