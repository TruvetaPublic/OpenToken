# OpenToken

## Introduction

OpenToken is an open-source library for generating privacy-preserving, cryptographically secure tokens derived from deterministic personal data. These tokens enable reliable person matching across disparate datasets without exposing sensitive attributes. OpenToken is designed to standardize token-based matching, improving interoperability, privacy, and data integration across healthcare, research, and other privacy-sensitive domains.

## Token generation strategy

Tokens are cryptographically secure hashes computed from multiple deterministic person attributes. Tokens are created based on a set of `token generation rules`. OpenToken uses multiple distinct token generation rules that define a set of person attributes and which parts of those attributes to use for token generation.

### Sample token generation rules

| Rule ID | Rule Definition                                          |
| ------- | -------------------------------------------------------- |
| T1      | `U(last-name)\|U(first-name-1)\|U(sex)\|birth-date`      |
| T2      | `U(last-name)\|U(first-name)\|birth-date\|postal-code-3` |
| T3      | `U(last-name)\|U(first-name)\|U(sex)\|birth-date`        |
| T4      | `social-security-number\|U(sex)\|birth-date`             |
| T5      | `U(last-name)\|U(first-name-3)\|U(sex)`                  |

> U(X) = uppercase(X)<br>
> attribute-N = take first N characters from the `attribute`

### Rules for token generation

A token signature is generated first for every token generation rule. The token signature is then cryptographically hashed and hex encoded to generate the token.

> $Token(R) = Hex(Sha256(TokenSignature(R)))$ where R is the rule ID.<br>
> The token is then transformed further using the formula below:<br>
> $Base64(AESEncrypt(Base64(HMACSHA256(Token(R)))))$<br>

### Example

Given a person with the following attributes:

```csv
RecordId,FirstName,LastName,PostalCode,Sex,BirthDate,SocialSecurityNumber
891dda6c-961f-4154-8541-b48fe18ee620,John,Doe,98004,Male,2000-01-01,123-45-6789
```

**Note:** No attribute value can be empty to be considered valid.

The token generation rules above generate the following token signatures:

| Rule ID | Token Signature               | Token                                                                                              |
| ------- | ----------------------------- | -------------------------------------------------------------------------------------------------- |
| T1      | `DOE\|J\|MALE\|2000-01-01`    | `Gn7t1Zj16E5Qy+z9iINtczP6fRDYta6C0XFrQtpjnVQSEZ5pQXAzo02Aa9LS9oNMOog6Ssw9GZE6fvJrX2sQ/cThSkB6m91L` |
| T2      | `DOE\|JOHN\|2000-01-01\|980`  | `pUxPgYL9+cMxkA+8928Pil+9W+dm9kISwHYPdkZS+I2nQ/bQ/8HyL3FOVf3NYPW5NKZZO1OZfsz7LfKYpTlaxyzMLqMF2Wk7` |
| T3      | `DOE\|JOHN\|MALE\|2000-01-01` | `rwjfwIo5OcJUItTx8KCoSZMtr7tVGSyXsWv/hhCWmD2pBO5JyfmujsosvwYbYeeQ4Vl1Z3eq0cTwzkvfzJVS/EKaRhtjMZz5` |
| T4      | `123456789\|MALE\|2000-01-01` | `9o7HIYZkhizczFzJL1HFyanlllzSa8hlgQWQ5gHp3Niuo2AvEGcUwtKZXChzHmAa8Jm3183XVoacbL/bFEJyOYYS4EQDppev` |
| T5      | `DOE\|JOH\|MALE`              | `QpBpGBqaMhagfcHGZhVavn23ko03jkyS9Vo4qe78E4sKw+Zq2CIw4MMWG8VXVwInnsFBVk6NSDUI79wECf5DchV5CXQ9AFqR` |

**Note:** The tokens in the example above have been generated using the hash key `HashingKey` and encryption key `Secret-Encryption-Key-Goes-Here.`

### OpenToken data flow

![open-token-data-flow](./open-token-data-flow.jpg)

### Validation of person attribute values prior to normalization

The person attributes are validated before normalization. The validation rules are as follows:

| Attribute Name         | Validation Rule                                                                                                                                                                                                                                                 |
| ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `FirstName`            | Cannot be a placeholder value (e.g., "Unknown", "Test", "NotAvailable", "Patient", "Sample", "Anonymous", "Missing", etc.). Must not be null or empty.                                                                                                        |
| `LastName`             | Must be at least 2 characters long. For 2-character names, must contain at least one vowel or be "Ng". Cannot be a placeholder value (e.g., "Unknown", "Test", "NotAvailable", "Patient", "Sample", "Anonymous", "Missing", etc.). Must not be null or empty. |
| `BirthDate`            | Must be after January 1, 1910. Cannot be in the future (after today's date). Must be in a valid date format.                                                                                                                                                 |
| `PostalCode`           | Must be a valid US ZIP code (5 or 9 digits) or Canadian postal code. US ZIP codes: `ddddd` or `ddddd-dddd`. Canadian postal codes: `AdA dAd` format (letter-digit-letter space digit-letter-digit). Cannot be common placeholder values like `00000`, `11111`, `12345`, `54321`, `98765` for US or `A1A 1A1`, `K1A 0A6`, `H0H 0H0` for Canadian codes. |
| `SocialSecurityNumber` | Area cannot be `000`, `666` or `900-999`. Group cannot be `00`. Serial cannot be `0000`. Cannot be one of the following invalid sequences: `111-11-1111`, `222-22-2222`, `333-33-3333`, `444-44-4444`, `555-55-5555`, `777-77-7777`, `888-88-8888`.             |

### Normalized person attributes for token generation

All attribute values get normalized as part of their processing. The normalization process includes:

**FirstName normalization:**

- Removes titles (e.g., "Dr. John" → "John")
- Removes middle initials (e.g., "John J" → "John")
- Removes trailing periods (e.g., "John J." → "John")
- Removes generational suffixes (e.g., "Henry IV" → "Henry")
- Removes non-alphabetic characters (e.g., "Anne-Marie" → "AnneMarie")
- Normalizes diacritics (e.g., "José" → "Jose")

**LastName normalization:**

- Removes generational suffixes (e.g., "Warner IV" → "Warner")
- Removes non-alphabetic characters (e.g., "O'Keefe" → "OKeefe")
- Normalizes diacritics (e.g., "García" → "Garcia")

| Attribute Name           | Normalized Format                                   |
| ------------------------ | --------------------------------------------------- |
| `record-id`              | Any unique string identifier                        |
| `first-name`             | Any string (after normalization as described above) |
| `last-name`              | Any string (after normalization as described above) |
| `postal-code`            | US: `ddddd` where `d` is a numeric digit (0-9). Canadian: `AdA dAd` where `A` is a letter and `d` is a digit |
| `sex`                    | `Male\|Female`                                      |
| `birth-date`             | `YYYY-MM-DD` where `MM` is (01-12), `DD` is (01-31) |
| `social-security-number` | `ddddddddd` where `d` is a numeric digit (0-9)      |

## OpenToken overview

This library focuses primarily on token generation. Even though the person matching process is beyond the scope of this library, this document discusses how these tokens work in a person matching system.

As noted above, N distinct tokens are generated for each person using this library. The output of this process is below for three person records r1, r2, and r3:

| RecordId | RuleId | Token(RecordId, RuleId) |
| -------- | ------ | ----------------------- |
| r1       | T1     | Token(r1,T1)            |
| r1       | T2     | Token(r1,T2)            |
| r1       | T3     | Token(r1,T3)            |
| r1       | T4     | Token(r1,T4)            |
| r1       | T5     | Token(r1,T5)            |
| r2       | T1     | Token(r2,T1)            |
| r2       | T2     | Token(r2,T2)            |
| r2       | T3     | Token(r2,T3)            |
| r2       | T4     | Token(r2,T4)            |
| r2       | T5     | Token(r2,T5)            |
| r3       | T1     | Token(r3,T1)            |
| r3       | T2     | Token(r3,T2)            |
| r3       | T3     | Token(r3,T3)            |
| r3       | T4     | Token(r3,T4)            |
| r3       | T5     | Token(r3,T5)            |

If tokens are generated for persons from multiple data sources, person matching systems can identify a person match if the tokens for a person from one data source matches tokens for another person from a different data source. In the picture below, all tokens for **r3** and **r4** match, and as such r3 and r4 are considered a match.

![open-token-system](./open-token-system.jpg)

## Library driver

A driver is provided so that the library code can be executed easily.

### Execution

#### Via shell

The driver code could be invoked using:

```shell
java -jar open-token-<version>.jar -i <input-file> -t <input-type> -o <output-file> -ot <output-type> -h "xb7...98a" -e "b32...q1r"
```

Example:
`java -jar target/open-token-1.8.0.jar -i src/test/resources/sample.csv -t csv -o target/output.csv -ot csv -h "HashingKey" -e "Secret-Encryption-Key-Goes-Here."`

#### Via Docker

Please run the following command in the same folder as the source CSV file:

```shell
docker run -v "$(pwd)":/app open-token -i <input-file> -t <input-type> -o <output-file> -ot <output-type> -h "xb7...98a" -e "b32...q1r"
```

Example:
`docker run -v "$(pwd)":/app open-token -i src/test/resources/sample.csv -t csv -o target/output.csv -ot csv -h "HashingKey" -e "Secret-Encryption-Key-Goes-Here."`

### Arguments

The driver accepts multiple command line arguments:

- `-t | --type`: This argument is used to specify the input file type. You can provide the file type as a string. Types `csv` or `parquet` are supported.

- `-i | --input`: This argument is used to specify the input file path. You can provide the path to an input file containing the sample data for person matching.

- `-o | --output`: This argument is used to specify the output file path. The generated tokens will be written to this file.

- `-ot | --output-type`: Optional. This argument is used to specify the output file type. If not provided, the input type will be used as output type. You can provide the file type as a string. Types `csv` or `parquet` are supported.

- `-h | --hashingsecret`: This argument is used to specify the hashing secret for the `HMAC-SHA256` digest. The generated tokens are hashed using this digest.

- `-e | --encryptionkey`: This argument is used to specify the encryption key for the `AES-256` symmetric encryption. The generated tokes are encrypted using this key.

The encryption logic is: Base64(AES-Encrypt(HMAC-SHA256(Hex(Sha256(token-signature)))))

### Accepted input

The input file (in csv format) must contain at least the following columns and values (one each):

| Accepted Column Names                              | Accepted Values                                                                |
| -------------------------------------------------- | ------------------------------------------------------------------------------ |
| RecordId, Id                                       | Any unique string identifier                                                   |
| FirstName, GivenName                               | Any string value                                                               |
| LastName, Surname                                  | Any string value                                                               |
| PostalCode, ZipCode                                | US: 5 or 9 digit ZIP code `ddddd` or `ddddd-dddd`. Canadian: 6 character postal code `AdAdAd` (with or without space) |
| Sex, Gender                                        | `Male`, `M`, `Female`, `F`                                                     |
| BirthDate, DateOfBirth                             | Dates in either format: `yyyy/MM/dd`, `MM/dd/yyyy`, `MM-dd-yyyy`, `dd.MM.yyyy` |
| SocialSecurityNumber, NationalIdentificationNumber | 9 digit number, with or without dashes, e.g. `ddd-dd-dddd`                     |

**Note 1:** No attribute values can be empty to be considered valid.

**Note 2:** commas are only used for separation of field values, not for within values.

The output file (in csv format) contains the following columns:

- RecordId
- TokenId
- Token

### Building

#### With Maven

Prerequisites:

- Java 11 SDK
- Maven 3.8.7

Run the following:

```shell
mvn clean install
```

The compiled jar can be found under ./target

#### With Docker

Prerequisites:

- Docker

Run the following:

```shell
docker build . -t open-token
```

This will build a local Docker image called `open-token`.

#### Generating `javadoc`

The `javadoc` for the library can be generated as following:

```shell
mvn clean javadoc:javadoc
```

The Java documentation is created in `./target/reports/apidocs`. Invoke by opening `./target/reports/apidocs/index.html` in your favorite browser.

## Overview of the library

This project, `open-token`, provides common utilities, models, and services used across the person matching system. It is designed to support the development of applications and services that require person matching capabilities, ensuring consistency and efficiency.

## Getting started

To use `open-token` in your project, follow these steps:

1. Add it as a dependency in your build configuration file. For Maven, add the following code to your `pom.xml`:

```xml
<dependency>
    <groupId>com.truveta.opentoken</groupId>
    <artifactId>open-token</artifactId>
    <version>1.8.0</version>
</dependency>
```

2. Import `open-token` in your Java code using the following import statement:

```java
import com.truveta.opentoken.tokens.*;
```

3. Start using the utilities, models, and services provided by `open-token` in your project. For example, you can use the `TokenGenerator` class to perform token generation operations:

```java
ArrayList<Map<String, String>> result = new ArrayList<>();

Map<Class<? extends Attribute>, String> row;

/* process person record one by one */
while (reader.hasNext()) {
    row = reader.next();

    /* generate all tokens for one person */
    Map<String, String> tokens = tokenGenerator.getAllTokens(row).getTokens();
    logger.info("Tokens: {}", tokens);

    Set<String> tokenIds = new TreeSet<>(tokens.keySet());

    /* add all the token for the person in result */
    for (String tokenId : tokenIds) {
        var rowResult = new HashMap<String, String>();
        rowResult.put("RecordId", row.get(RecordIdAttribute.class));
        rowResult.put("RuleId", tokenId);
        rowResult.put("Token", tokens.get(tokenId));
        result.add(rowResult);
    }
};

// result has tokens for all persons now.
```

## Test data

In order to test, you have the option to generate mock person data in the expected format.

### Prerequisites

- Python3
- [faker](https://pypi.org/project/Faker/)

### Generating mock data

Under `src/test/resources/mockdata` you can find a python script that allows to generate fake random person data. You can run it as follows with pre-configured defaults:

```shell
./generate.sh 
```

You can modify the parameters when running the script directly. The script will repeat a percentage of the record values using a different record ID.

```shell
# python data_generator.py <number of records> <percentage of repeated records> <output file name>
python data_generator.py 100 0.05 test_data.csv
```

## Contribute

Truveta encourages contributions in the form of features, bug fixes, documentation updates, etc. Some of the areas in key needs of improvements are:

1. The library currently provides `csv` reader and writer. See `com.truveta.opentoken.io`. Readers/writers for `parquet` file is highly desired.
2. More test coverage.

## Development environment

This project includes a [Development Container](https://containers.dev/) configuration that provides a consistent and isolated development environment for working with OpenToken. The Dev Container includes all necessary tools and dependencies pre-configured, making it easy to start contributing right away.

### Getting started with the Dev Container

For detailed instructions on how to use the development container, please refer to the [Dev Container README](./.devcontainer/README.md).

The Dev Container provides:

- Java 11 SDK pre-installed
- Maven 3.8.7 pre-installed
- All necessary dependencies configured
- Git and other development tools
- SSL certificate handling for corporate environments

Using the Dev Container ensures all contributors work with the same environment, avoiding "works on my machine" issues and making the development experience more consistent and reproducible.
