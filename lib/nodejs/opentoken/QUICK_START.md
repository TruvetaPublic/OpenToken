# OpenToken Node.js Quick Start

## Prerequisites

- Node.js 18.0.0 or higher
- npm 8.0.0 or higher

## Installation

```bash
cd lib/nodejs/opentoken
npm install
```

## Building

```bash
npm run build
```

This compiles the TypeScript source code to JavaScript in the `dist/` directory.

## Testing

```bash
# Run all tests
npm test

# Run tests with coverage
npm run test:coverage

# Run tests in watch mode (for development)
npm test -- --watch
```

## Usage (When Complete)

### Command Line Interface

```bash
opentoken \
  -i /path/to/input.csv \
  -t csv \
  -o /path/to/output.csv \
  -h "HashingKey" \
  -e "Secret-Encryption-Key-Goes-Here."
```

### As a Library

```typescript
import {
  FirstNameAttribute,
  LastNameAttribute,
  HashTokenTransformer,
  EncryptTokenTransformer,
} from '@truveta/opentoken';

// Create attributes
const firstName = new FirstNameAttribute();
const lastName = new LastNameAttribute();

// Validate and normalize
const normalizedFirst = firstName.normalize('John');
const isValid = firstName.validate(normalizedFirst);

// Create token transformers
const hashTransformer = new HashTokenTransformer('your-secret-key');
const encryptTransformer = new EncryptTokenTransformer('your-32-character-encryption-key!');

// Transform tokens (full pipeline not yet implemented)
const hashed = hashTransformer.transform('token-signature');
const encrypted = encryptTransformer.transform(hashed);
```

## Development

### Code Quality

```bash
# Run linter
npm run lint

# Fix linting issues automatically
npm run lint:fix

# Format code with Prettier
npm run format
```

### Project Structure

```
lib/nodejs/opentoken/
├── src/                        # TypeScript source code
│   ├── attributes/            # Person attributes with validation
│   │   ├── person/           # FirstName, LastName, etc.
│   │   ├── general/          # StringAttribute, DateAttribute
│   │   ├── validation/       # Validators
│   │   └── utilities/        # Helper utilities
│   ├── tokens/               # Token definitions (T1-T5)
│   │   └── definitions/      # Individual token rules
│   ├── tokentransformer/     # HMAC-SHA256 + AES-256 encryption
│   ├── io/                   # File I/O (CSV, Parquet) - TODO
│   └── processor/            # Token generation pipeline - TODO
├── test/                      # Unit tests
├── dist/                      # Compiled JavaScript (gitignored)
└── node_modules/             # Dependencies (gitignored)
```

## Current Status

### ✅ Implemented
- Base attribute system with validation
- Person attributes (FirstName, LastName, BirthDate, Sex, SSN)
- Token definitions (T1-T5)
- Token transformers (HMAC-SHA256, AES-256-GCM)
- Unit tests (11 passing tests)

### 🚧 In Progress
- PostalCode attribute
- CSV I/O
- PersonAttributesProcessor
- CLI
- Metadata generation

### ⏳ Planned
- Parquet I/O
- Interoperability tests
- Full test coverage
- CI/CD pipeline
- Docker support

For detailed status, see [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md).

## Contributing

When adding new features:

1. Follow TypeScript best practices
2. Add unit tests for new code
3. Run linter and formatter before committing
4. Ensure all tests pass
5. Update documentation

## Architecture

The Node.js implementation follows the same architecture as Java and Python:

1. **Attributes**: Person data fields with validation and normalization
2. **Validators**: Composable validation rules
3. **Tokens**: Rules defining which attributes form each token (T1-T5)
4. **Transformers**: HMAC-SHA256 hashing + AES-256-GCM encryption
5. **I/O**: CSV and Parquet file processors
6. **Processor**: Token generation pipeline

All three implementations (Java, Python, Node.js) must generate **identical tokens** for the same input data.

## Examples

### Validate a Social Security Number

```typescript
import { SocialSecurityNumberAttribute } from '@truveta/opentoken';

const ssn = new SocialSecurityNumberAttribute();

// Normalize - removes dashes, pads with zeros if needed
const normalized = ssn.normalize('123-45-6789'); // Returns "123-45-6789"
const short = ssn.normalize('1234567'); // Returns "001-23-4567"

// Validate - checks format and rejects invalid patterns
console.log(ssn.validate('123-45-6789')); // true
console.log(ssn.validate('000-12-3456')); // false (area cannot be 000)
console.log(ssn.validate('111-11-1111')); // false (common placeholder)
```

### Validate and Normalize a Name

```typescript
import { FirstNameAttribute } from '@truveta/opentoken';

const firstName = new FirstNameAttribute();

// Normalize - removes titles, middle initials, suffixes, diacritics
console.log(firstName.normalize('Dr. John J.')); // "JOHN"
console.log(firstName.normalize('José')); // "JOSE"
console.log(firstName.normalize('Anne-Marie')); // "ANNEMARIE"

// Validate - rejects placeholder names
console.log(firstName.validate('John')); // true
console.log(firstName.validate('Test')); // false (placeholder)
console.log(firstName.validate('Unknown')); // false (placeholder)
```

### Hash and Encrypt a Token

```typescript
import { HashTokenTransformer, EncryptTokenTransformer } from '@truveta/opentoken';

// Create transformers
const hasher = new HashTokenTransformer('MyHashingSecret');
const encrypter = new EncryptTokenTransformer('My32CharacterEncryptionKey!!!');

// Generate token signature (this would come from PersonAttributesProcessor)
const tokenSignature = 'DOE|J|MALE|2000-01-01';

// Hash it
const hashed = hasher.transform(tokenSignature);
console.log(hashed); // Base64-encoded HMAC-SHA256 hash

// Encrypt it
const encrypted = encrypter.transform(hashed);
console.log(encrypted); // Base64-encoded AES-256-GCM encrypted token
```

## Troubleshooting

### Build Errors

```bash
# Clear everything and rebuild
npm run clean
npm install
npm run build
```

### Test Failures

```bash
# Run tests in verbose mode
npm test -- --verbose

# Run a specific test file
npm test -- FirstNameAttribute.test
```

### Type Errors

Make sure you're using TypeScript 5.0 or higher:

```bash
npm list typescript
```

## Resources

- [Main README](../../../README.md)
- [Implementation Status](./IMPLEMENTATION_STATUS.md)
- [Development Guide](../../../docs/dev-guide-development.md)
- [Java Implementation](../../java/opentoken/)
- [Python Implementation](../../python/opentoken/)

## License

Copyright (c) Truveta. All rights reserved.

Licensed under the Apache License, Version 2.0.
