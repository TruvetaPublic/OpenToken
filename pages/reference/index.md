---
layout: default
---

# Reference

Complete API documentation and attribute reference.

## Library APIs

OpenToken provides programmatic APIs for token generation in Java and Python.

### Java API

#### Core Classes

```java
import com.truveta.opentoken.attributes.PersonAttributes;
import com.truveta.opentoken.tokens.TokenRegistry;
import com.truveta.opentoken.tokens.Token;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
```

#### PersonAttributes

Represents a single person's attributes.

```java
PersonAttributes person = new PersonAttributes.Builder()
    .recordId("patient_123")
    .firstName("John")
    .lastName("Doe")
    .birthDate("1980-01-15")
    .sex("Male")
    .postalCode("98004")
    .socialSecurityNumber("123456789")
    .build();
```

**Methods:**
- `Builder()` – Fluent builder for creating attributes
- `getRecordId()` – Returns record identifier
- `getFirstName()`, `getLastName()`, `getBirthDate()`, etc. – Accessors
- `isValid()` – Returns true if all attributes are valid
- `getInvalidAttributes()` – Returns list of invalid attributes

#### TokenRegistry

Provides access to all token rules.

```java
TokenRegistry registry = new TokenRegistry();
List<Token> tokens = registry.getAllTokens(); // Returns T1–T5
Token t1 = registry.getToken("T1");
String signature = t1.getSignature(person); // Get token signature
```

#### Token Transformers

Transform token signatures into encrypted/hashed tokens.

```java
String hashingSecret = "HashingKey";
String encryptionKey = "EncryptionKey";

// Hash-only transformation
HashTokenTransformer hasher = new HashTokenTransformer(hashingSecret);
String hashedToken = hasher.transform("TOKEN_SIGNATURE");

// Full encryption transformation
EncryptTokenTransformer encryptor = new EncryptTokenTransformer(hashingSecret, encryptionKey);
String encryptedToken = encryptor.transform("TOKEN_SIGNATURE");
```

### Python API

#### Core Classes

```python
from opentoken.attributes.person_attributes import PersonAttributes
from opentoken.tokens.token_registry import TokenRegistry
from opentoken.tokentransformer.hash_token_transformer import HashTokenTransformer
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer
```

#### PersonAttributes

```python
person = PersonAttributes(
    record_id="patient_123",
    first_name="John",
    last_name="Doe",
    birth_date="1980-01-15",
    sex="Male",
    postal_code="98004",
    social_security_number="123456789"
)
```

**Methods:**
- `is_valid()` – Returns True if all attributes are valid
- `get_invalid_attributes()` – Returns list of invalid attributes
- Properties: `first_name`, `last_name`, `birth_date`, `sex`, `postal_code`, `social_security_number`, `record_id`

#### TokenRegistry

```python
registry = TokenRegistry()
tokens = registry.get_all_tokens()  # Returns T1–T5
t1 = registry.get_token("T1")
signature = t1.get_signature(person)  # Get token signature
```

#### Token Transformers

```python
hashing_secret = "HashingKey"
encryption_key = "EncryptionKey"

# Hash-only transformation
hasher = HashTokenTransformer(hashing_secret)
hashed_token = hasher.transform("TOKEN_SIGNATURE")

# Full encryption transformation
encryptor = EncryptTokenTransformer(hashing_secret, encryption_key)
encrypted_token = encryptor.transform("TOKEN_SIGNATURE")
```

---

## Attribute Reference

Complete documentation of person attributes, validation rules, and normalization.

### FirstName

**Description:** Given name or first name of the person.

**Validation Rules:**
- Cannot be null or empty
- Cannot be a placeholder value: "Unknown", "Test", "NotAvailable", "Patient", "Sample", "Anonymous", "Missing", etc.
- Must contain at least one alphabetic character

**Normalization:**
- Removes titles: "Dr. John" → "John", "Mr. John Smith" → "John"
- Removes middle initials: "John J" → "John", "John J." → "John"
- Removes suffixes: "John Jr" → "John", "John Jr." → "John"
- Removes non-alphabetic characters except spaces (initially): "John-Marie" → "John Marie" → "JOHNMARIE"
- Normalizes diacritics: "José" → "JOSE", "Müller" → "MULLER"
- Uppercase for token generation: "john" → "JOHN"

**Example:**
```
Input: "José María"
Normalized: "JOSE MARIA"
Token Use: "JOSE" (or first letter "J" in T1)
```

### LastName

**Description:** Family name or surname of the person.

**Validation Rules:**
- Cannot be null or empty
- Must be at least 2 characters long (with exceptions)
- Exception: Single-character "Ng" is valid (common Vietnamese surname)
- Cannot be a placeholder value
- For 2-character names, must contain at least one vowel OR be "Ng"

**Normalization:**
- Removes suffixes: "Warner IV" → "Warner", "Smith Sr." → "Smith"
- Removes non-alphabetic characters: "O'Brien" → "OBRIEN", "Anne-Marie" → "ANNEMARIE"
- Normalizes diacritics: "García" → "GARCIA"
- Uppercase for token generation

**Example:**
```
Input: "O'Brien"
Normalized: "OBRIEN"
Token Use: "OBRIEN"
```

### BirthDate

**Description:** Date of birth in the format YYYY-MM-DD.

**Validation Rules:**
- Must be after January 1, 1910
- Cannot be in the future (after today's date)
- Must be a valid calendar date
- Required

**Accepted Input Formats:**
- `YYYY-MM-DD` (preferred)
- `MM/DD/YYYY`
- `MM-DD-YYYY`
- `DD.MM.YYYY`

**Normalization:**
- Parsed and validated
- Output format: `YYYY-MM-DD`

**Example:**
```
Input: "01/15/1980" or "1980-01-15"
Normalized: "1980-01-15"
Valid Range: 1910-01-01 to today
```

### Sex

**Description:** Biological sex or gender of the person.

**Validation Rules:**
- Must be "Male" or "Female"
- Null or empty values are invalid

**Accepted Input Values:**
- Male: "Male", "M" (case-insensitive)
- Female: "Female", "F" (case-insensitive)

**Normalization:**
- Standardized to "Male" or "Female"
- Uppercase for token generation: "MALE" or "FEMALE"

**Example:**
```
Input: "M" or "m" or "Male"
Normalized: "MALE"
Token Use: "MALE"
```

### PostalCode

**Description:** Postal code for United States or Canada.

**Validation Rules:**

**US ZIP Codes:**
- Valid formats: 5-digit (98004), 9-digit (98004-1234), or 3-digit (980)
- 3-digit codes (ZIP-3) auto-padded to 5-digit: "980" → "98000"
- 4-digit codes auto-padded: "9800" → "98000" (with trailing zero)
- Invalid ZIP-3 codes: "000", "555", "888"
- Must not be placeholder: "00000", "11111", "12345", "54321", "98765"

**Canadian Postal Codes:**
- Valid formats: 3-char (K1A), 4-char (K1A1), 5-char (K1A1A), or 6-char full (K1A 1A1)
- Format: `AdA dAd` where A = letter, d = digit
- Auto-padded and formatted with space
- Invalid examples: "K1A" (reserved capital region), "M7A" (Toronto postal delivery), "H0H" (fictional from Christmas song)
- Must not be placeholder: "A1A 1A1", "X0X 0X0"

**Normalization:**
- Uppercase
- Dashes removed for US ZIP
- Space added/normalized for Canadian postal codes
- Padding applied as needed

**Example:**
```
Input: "98004" or "98004-1234"
Normalized: "98004"
Token Use: "98004" or first 3 "980" (depending on rule)

Input: "K1A1A1" or "K1A 1A1"
Normalized: "K1A 1A1"
Token Use: "K1A" (first 3) or full "K1A1A1" (depending on rule)
```

### SocialSecurityNumber (SSN)

**Description:** US Social Security Number or national identification number.

**Validation Rules:**
- Must be a valid 9-digit number (with or without dashes)
- Area number (first 3 digits) cannot be: 000, 666, or 900–999
- Group number (middle 2 digits) cannot be: 00
- Serial number (last 4 digits) cannot be: 0000
- Cannot be a commonly used invalid sequence:
  - 111-11-1111, 222-22-2222, 333-33-3333, 444-44-4444
  - 555-55-5555, 777-77-7777, 888-88-8888

**Accepted Input Formats:**
- `123456789` (9 digits, no separator)
- `123-45-6789` (with dashes)

**Normalization:**
- Dashes removed
- 9-digit numeric string

**Example:**
```
Input: "123-45-6789" or "123456789"
Normalized: "123456789"
Token Use: "123456789"
Valid Example: 123-45-6789 (area 123, group 45, serial 6789)
Invalid Example: 000-00-0000 (area 000 invalid)
```

### RecordId (Optional)

**Description:** Unique identifier for the record. Auto-generated if not provided.

**Validation Rules:**
- Optional; if omitted, a UUID is generated
- If provided, must be a unique string identifier

**Accepted Input Formats:**
- Any string: "patient_123", "record_abc", UUID format, etc.

**Example:**
```
Input: "patient_12345"
Output: "patient_12345"
Or: (no input)
Output: "550e8400-e29b-41d4-a716-446655440000" (auto-generated UUID)
```

---

## Token Rules Summary

| Rule   | Components                                      | Use Case                     |
| ------ | ----------------------------------------------- | ---------------------------- |
| **T1** | Last name + first letter + sex + birthdate      | Standard matching            |
| **T2** | Last name + full first name + birthdate + ZIP-3 | Geographic variations        |
| **T3** | Last name + full first name + sex + birthdate   | Flexible first name matching |
| **T4** | SSN + sex + birthdate                           | Authoritative identifier     |
| **T5** | Last name + first 3 letters + sex               | Quick search                 |

See [Concepts: Token Rules](../concepts/token-rules.md) for detailed examples.

---

## Encrypted vs. Hashed Tokens

### Encrypted Tokens (Default)

```
Token Signature → SHA-256 Hash → HMAC-SHA256(hash, key) → AES-256 Encrypt → Base64
```

**Length:** ~80–100 characters (base64 encoded)

**Properties:**
- Reversible: Can decrypt with encryption key
- Secure: Prevents re-identification without key
- Slower: Encryption overhead

**Use:** External sharing, production systems with strict privacy requirements

### Hashed Tokens (--hash-only)

```
Token Signature → SHA-256 Hash → HMAC-SHA256(hash, key) → Base64
```

**Length:** ~50–80 characters (base64 encoded)

**Properties:**
- Not reversible: Cannot decrypt (one-way hash)
- Secure: HMAC prevents pre-computed tables
- Faster: No encryption overhead

**Use:** Internal systems, token matching without decryption, cost-sensitive scenarios

---

## CLI Argument Reference

See [Running OpenToken: CLI Guide](../running-opentoken/index.md#cli-guide) for complete command-line documentation.

---

## Next Steps

- **Understand validation**: [Security](../security.md)
- **View token specification**: [Specification](../specification.md)
- **Configure inputs**: [Configuration](../config/configuration.md)
