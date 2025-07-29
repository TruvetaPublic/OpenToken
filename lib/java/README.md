# OpenToken Java Implementation

This is the Java implementation of the OpenToken library

## Prerequisites

- Java 11 SDK or higher
- Maven 3.8.7 or higher

## Installation

### Building from Source

```shell
# From the java directory
mvn clean install
```

The compiled JAR will be available in `target/open-token-<version>.jar`

### Maven Dependency

Add this to your `pom.xml`:

```xml
<dependency>
    <groupId>com.truveta.opentoken</groupId>
    <artifactId>open-token</artifactId>
    <version>1.9.2</version>
</dependency>
```

## Usage

### Command Line Interface

```shell
java -jar target/open-token-<version>.jar [OPTIONS]
```

**Arguments:**
- `-i, --input <path>`: Input file path
- `-t, --type <type>`: Input file type (`csv` or `parquet`)
- `-o, --output <path>`: Output file path
- `-ot, --output-type <type>`: Output file type (optional, defaults to input type)
- `-h, --hashingsecret <secret>`: Hashing secret for HMAC-SHA256
- `-e, --encryptionkey <key>`: Encryption key for AES-256

**Example:**
```shell
java -jar target/open-token-1.9.2.jar \
  -i src/test/resources/sample.csv \
  -t csv \
  -o target/output.csv \
  -h "HashingKey" \
  -e "Secret-Encryption-Key-Goes-Here."
```

### Programmatic API

```java
import com.truveta.opentoken.tokens.*;
import com.truveta.opentoken.io.*;
import com.truveta.opentoken.processor.*;

// Initialize token transformers
List<TokenTransformer> transformers = Arrays.asList(
    new HashTokenTransformer("your-hashing-secret"),
    new EncryptTokenTransformer("your-encryption-key")
);

// Process data
try (PersonAttributesReader reader = new PersonAttributesCSVReader("input.csv");
     PersonAttributesWriter writer = new PersonAttributesCSVWriter("output.csv")) {
    
    PersonAttributesProcessor.process(reader, writer, transformers, metadata);
}
```

## Testing

Run all tests:
```shell
mvn test
```

Run with coverage:
```shell
mvn clean test jacoco:report
```

Coverage reports will be generated in `target/site/jacoco/index.html`.

## Development

### Project Structure

```
src/
├── main/java/com/truveta/opentoken/
│   ├── attributes/          # Person attribute definitions
│   ├── io/                  # Input/output handlers
│   ├── processor/           # Core processing logic
│   ├── tokens/              # Token generation
│   └── tokentransformer/    # Token transformation
└── test/
    ├── java/                # Unit tests
    └── resources/           # Test data
```

### Code Style

This project uses Checkstyle for code formatting. Run:
```shell
mvn checkstyle:check
```

### Generating Documentation

Generate Javadoc:
```shell
mvn clean javadoc:javadoc
```

Documentation will be available in `target/site/apidocs/index.html`.

## Supported Input Formats

- **CSV**: Comma-separated values with required columns
- **Parquet**: Apache Parquet format

## Output

The library generates:
- **Token file**: Contains RecordId, TokenId, and Token columns
- **Metadata file**: JSON file with processing statistics and system information

## Known Issues

- Large files may require increased JVM heap size: `-Xmx4g`
- Unicode characters in names are normalized to ASCII equivalents

## Contributing

1. Follow Java coding standards and Checkstyle rules
2. Add unit tests for new features
3. Update Javadoc for public APIs
4. Ensure backward compatibility

## Support

For Java-specific issues, please check:
1. Java version compatibility (Java 11+)
2. Maven dependency conflicts
3. Memory settings for large datasets

For general questions, see the main project README.
