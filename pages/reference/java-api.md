---
layout: default
---

# Java API Reference

Document the Java classes and methods for programmatic token generation.

## Core Classes

```java
import com.truveta.opentoken.attributes.PersonAttributes;
import com.truveta.opentoken.tokens.TokenRegistry;
import com.truveta.opentoken.tokens.Token;
import com.truveta.opentoken.tokentransformer.HashTokenTransformer;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;
```

## PersonAttributes

Represents a single person's attributes for token generation.

### Builder Pattern

```java
PersonAttributes person = new PersonAttributes.Builder()
    .recordId("patient_123")
    .firstName("John")
    .lastName("Doe")
    .birthDate("1980-01-15")
    .sex("Male")
    .postalCode("98004")
    .socialSecurityNumber("123-45-6789")
    .build();
```

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getRecordId()` | `String` | Returns the record identifier |
| `getFirstName()` | `String` | Returns normalized first name |
| `getLastName()` | `String` | Returns normalized last name |
| `getBirthDate()` | `String` | Returns birth date (YYYY-MM-DD) |
| `getSex()` | `String` | Returns normalized sex (MALE/FEMALE) |
| `getPostalCode()` | `String` | Returns normalized postal code |
| `getSocialSecurityNumber()` | `String` | Returns normalized SSN |
| `isValid()` | `boolean` | Returns true if all attributes pass validation |
| `getInvalidAttributes()` | `List<String>` | Returns list of invalid attribute names |

### Validation Example

```java
PersonAttributes person = new PersonAttributes.Builder()
    .firstName("John")
    .lastName("D")  // Too short - invalid
    .birthDate("2050-01-01")  // Future date - invalid
    .sex("Male")
    .postalCode("98004")
    .socialSecurityNumber("000-00-0000")  // Invalid SSN
    .build();

if (!person.isValid()) {
    for (String attr : person.getInvalidAttributes()) {
        System.out.println("Invalid: " + attr);
    }
}
// Output:
// Invalid: LastName
// Invalid: BirthDate
// Invalid: SocialSecurityNumber
```

## TokenRegistry

Provides access to all token rules (T1–T5).

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAllTokens()` | `List<Token>` | Returns all 5 token rules |
| `getToken(String ruleId)` | `Token` | Returns specific token rule by ID |

### Example

```java
TokenRegistry registry = new TokenRegistry();

// Get all tokens
List<Token> tokens = registry.getAllTokens();
for (Token token : tokens) {
    System.out.println(token.getRuleId());
}
// Output: T1, T2, T3, T4, T5

// Get specific token
Token t1 = registry.getToken("T1");
```

## Token Interface

Represents a single token rule.

### Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getRuleId()` | `String` | Returns rule identifier (T1-T5) |
| `getSignature(PersonAttributes)` | `String` | Generates token signature from attributes |
| `getRequiredAttributes()` | `List<String>` | Returns attributes used by this rule |

### Example

```java
Token t1 = registry.getToken("T1");

// Get signature for a person
String signature = t1.getSignature(person);
// Example: "DOE|J|MALE|1980-01-15"

// Check required attributes
List<String> required = t1.getRequiredAttributes();
// ["LastName", "FirstName", "Sex", "BirthDate"]
```

## Token Transformers

Transform token signatures into encrypted or hashed tokens.

### HashTokenTransformer

One-way hashing without encryption.

```java
HashTokenTransformer hasher = new HashTokenTransformer("YourHashingSecret");

String signature = "DOE|J|MALE|1980-01-15";
String hashedToken = hasher.transform(signature);
// Returns: Base64-encoded HMAC-SHA256 hash
```

### EncryptTokenTransformer

Full encryption with AES-256-GCM.

```java
EncryptTokenTransformer encryptor = new EncryptTokenTransformer(
    "YourHashingSecret",
    "YourEncryptionKey-32Characters!"  // Exactly 32 chars
);

String signature = "DOE|J|MALE|1980-01-15";
String encryptedToken = encryptor.transform(signature);
// Returns: Base64-encoded encrypted token
```

## Complete Example

```java
import com.truveta.opentoken.attributes.PersonAttributes;
import com.truveta.opentoken.tokens.TokenRegistry;
import com.truveta.opentoken.tokens.Token;
import com.truveta.opentoken.tokentransformer.EncryptTokenTransformer;

public class TokenGenerator {
    public static void main(String[] args) {
        // Create person
        PersonAttributes person = new PersonAttributes.Builder()
            .recordId("patient_001")
            .firstName("John")
            .lastName("Doe")
            .birthDate("1980-01-15")
            .sex("Male")
            .postalCode("98004")
            .socialSecurityNumber("123-45-6789")
            .build();

        // Validate
        if (!person.isValid()) {
            System.err.println("Invalid attributes: " + person.getInvalidAttributes());
            return;
        }

        // Setup transformer
        EncryptTokenTransformer transformer = new EncryptTokenTransformer(
            "HashingSecret",
            "EncryptionKey-32Characters-Here"
        );

        // Generate all tokens
        TokenRegistry registry = new TokenRegistry();
        for (Token token : registry.getAllTokens()) {
            String signature = token.getSignature(person);
            String encrypted = transformer.transform(signature);
            System.out.printf("%s,%s,%s%n", 
                person.getRecordId(), 
                token.getRuleId(), 
                encrypted
            );
        }
    }
}
```

## I/O Classes (CLI Module)

For file processing, use classes from the CLI module:

```java
import com.truveta.opentoken.io.CsvReader;
import com.truveta.opentoken.io.CsvWriter;
import com.truveta.opentoken.io.ParquetReader;
import com.truveta.opentoken.io.ParquetWriter;
```

### CsvReader

```java
CsvReader reader = new CsvReader("input.csv");
while (reader.hasNext()) {
    PersonAttributes person = reader.next();
    // Process person...
}
reader.close();
```

### CsvWriter

```java
CsvWriter writer = new CsvWriter("output.csv");
writer.writeHeader();
writer.writeToken(recordId, ruleId, token);
writer.close();
```

## Thread Safety

All transformer classes are thread-safe and can be shared across threads:

```java
EncryptTokenTransformer transformer = new EncryptTokenTransformer(...);

// Safe to use from multiple threads
ExecutorService executor = Executors.newFixedThreadPool(4);
for (PersonAttributes person : persons) {
    executor.submit(() -> {
        for (Token token : registry.getAllTokens()) {
            String encrypted = transformer.transform(token.getSignature(person));
            // ...
        }
    });
}
```

## Maven Dependency

```xml
<dependency>
    <groupId>com.truveta</groupId>
    <artifactId>opentoken</artifactId>
    <version>1.7.0</version>
</dependency>

<!-- For CLI/IO classes -->
<dependency>
    <groupId>com.truveta</groupId>
    <artifactId>opentoken-cli</artifactId>
    <version>1.7.0</version>
</dependency>
```

## Next Steps

- [Python API Reference](python-api.md) - Cross-language parity
- [Token Registration](token-registration.md) - Add custom tokens
- [CLI Reference](cli.md) - Command-line usage
