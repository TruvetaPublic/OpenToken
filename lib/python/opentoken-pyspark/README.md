# OpenToken PySpark Bridge

A PySpark integration for the OpenToken library, enabling distributed privacy-preserving token generation for large-scale person matching workflows.

## Overview

The OpenToken PySpark Bridge provides a seamless interface between PySpark DataFrames and the OpenToken library, allowing you to generate cryptographically secure tokens for person matching in a distributed computing environment.

## Features

- **Distributed Processing**: Leverage PySpark's distributed computing capabilities for large datasets
- **Simple API**: Easy-to-use interface that accepts PySpark DataFrames
- **Compatible**: Works with standard OpenToken secrets for consistent token generation
- **Flexible Column Names**: Supports multiple column name variants (e.g., FirstName/GivenName)
- **Jupyter Ready**: Includes example notebooks for interactive exploration

## Installation

### Quick Install

```bash
# First, install the OpenToken core library
cd lib/python
pip install -e .

# Then install the PySpark bridge
cd ../python-pyspark
pip install -e .
```

For detailed installation instructions, troubleshooting, and alternative installation methods, see [INSTALL.md](INSTALL.md).

### Prerequisites

- Python 3.10 or higher
- PySpark 3.0.0 or higher
- OpenToken core library
- Java 8 or higher (required by PySpark)

## Quick Start

```python
from pyspark.sql import SparkSession
from opentoken_pyspark import OpenTokenProcessor

# Create Spark session
spark = SparkSession.builder \
    .appName("OpenTokenExample") \
    .master("local[*]") \
    .getOrCreate()

# Load your data
df = spark.read.csv("data.csv", header=True)

# Initialize processor with your secrets
processor = OpenTokenProcessor(
    hashing_secret="your-hashing-secret",
    encryption_key="your-encryption-key"
)

# Generate tokens
tokens_df = processor.process_dataframe(df)

# View results
tokens_df.show()
```

## Input DataFrame Requirements

Your input DataFrame must contain the following columns (alternative names are supported):

| Standard Name | Alternative Names | Description |
|--------------|-------------------|-------------|
| RecordId | Id | Unique identifier (optional - auto-generated if not provided) |
| FirstName | GivenName | Person's first name |
| LastName | Surname | Person's last name |
| BirthDate | DateOfBirth | Date of birth in YYYY-MM-DD format |
| Sex | Gender | Sex/Gender (Male, Female, M, F) |
| PostalCode | ZipCode | US ZIP code or Canadian postal code |
| SocialSecurityNumber | NationalIdentificationNumber | SSN or national ID number |

## Output Format

The output DataFrame contains:

- **RecordId**: The original record identifier
- **RuleId**: Token rule identifier (T1, T2, T3, T4, T5)
- **Token**: The generated cryptographic token

Each input record produces multiple output rows (one per token rule).

## Using Custom Token Definitions

You can define custom tokens using the `opentoken.notebook_helpers` module and pass them to the processor:

```python
from opentoken.notebook_helpers import TokenBuilder, CustomTokenDefinition
from opentoken_pyspark import OpenTokenProcessor

# Method 1: Using TokenBuilder
custom_token = TokenBuilder("T6") \
    .add("last_name", "T|U") \
    .add("first_name", "T|U") \
    .add("birth_date", "T|D") \
    .add("postal_code", "T|S(0,3)") \
    .add("sex", "T|U") \
    .build()

custom_definition = CustomTokenDefinition().add_token(custom_token)

# Create processor with custom token definition
processor = OpenTokenProcessor(
    hashing_secret="your-hashing-secret",
    encryption_key="your-encryption-key-32-chars!!",
    token_definition=custom_definition  # Pass custom definition here
)

# Process DataFrame - will use T6 instead of default T1-T5
tokens_df = processor.process_dataframe(df)
```

For more examples and interactive experimentation with custom tokens, see the [Custom Token Definition Guide](notebooks/Custom_Token_Definition_Guide.ipynb).

## Example Notebooks

See the included Jupyter notebooks for complete examples:

**Basic Usage:**
```bash
cd notebooks
jupyter notebook OpenToken_PySpark_Example.ipynb
```

**Custom Token Definitions:**
```bash
cd notebooks
jupyter notebook Custom_Token_Definition_Guide.ipynb
```

## Dataset Overlap Analysis

The `OpenTokenOverlapAnalyzer` class helps identify matching records between two tokenized datasets based on encrypted tokens.

### Basic Usage

```python
from opentoken_pyspark import OpenTokenOverlapAnalyzer

# Initialize with encryption key (same key used for token generation)
analyzer = OpenTokenOverlapAnalyzer("encryption-key-32-characters!!")

# Analyze overlap between two tokenized datasets
# Match on tokens T1 and T2 (both must match)
results = analyzer.analyze_overlap(
    tokens_df1,
    tokens_df2,
    matching_rules=["T1", "T2"],
    dataset1_name="Hospital_A",
    dataset2_name="Hospital_B"
)

# Print summary
analyzer.print_summary(results)

# Access detailed results
print(f"Total records in dataset 1: {results['total_records_dataset1']}")
print(f"Matching records: {results['matching_records_dataset1']}")
print(f"Overlap percentage: {results['overlap_percentage']:.2f}%")

# Get DataFrame of matched record pairs
matches_df = results['matches']
matches_df.show()
```

### Compare with Multiple Rule Sets

```python
# Compare overlap using different matching criteria
rule_sets = [
    ["T1"],              # Match on T1 only
    ["T1", "T2"],        # Match on T1 AND T2
    ["T1", "T2", "T3"]   # Match on T1 AND T2 AND T3
]

results = analyzer.compare_with_multiple_rules(
    tokens_df1, tokens_df2, rule_sets
)

# See how overlap changes with stricter rules
for result in results:
    print(f"Rules {result['matching_rules']}: "
          f"{result['overlap_percentage']:.2f}% overlap")
```

### Use Cases

- **Data Quality Assessment**: Identify duplicate records across datasets
- **Patient Matching**: Find matching patients between healthcare systems
- **Research Cohort Overlap**: Analyze overlap between research study populations
- **Data Sharing Analysis**: Assess data overlap before establishing data sharing agreements

### How It Works

1. Both datasets must contain tokenized records (RecordId, RuleId, Token columns)
2. Matching rules specify which token types must match (e.g., ["T1", "T2"])
3. Records are considered matching only if ALL specified token types match
4. The analyzer provides statistics and a DataFrame of matched record pairs
5. Uses the same encryption key that was used to generate the tokens

## Testing

Run the test suite:

```bash
# From the python-pyspark directory
pytest
```

## Performance Considerations

- **Partitioning**: PySpark processes data in parallel across partitions. Adjust `spark.sql.shuffle.partitions` for your cluster size.
- **Memory**: Token generation is memory-efficient but ensure adequate executor memory for your data volume.
- **Secrets**: Secrets are serialized to worker nodes - ensure secure cluster configuration.

## Architecture

The PySpark bridge uses Pandas UDFs (User Defined Functions) to efficiently process batches of records:

1. Data is partitioned across the Spark cluster
2. Each partition is processed by a Pandas UDF
3. Within each batch, the OpenToken library generates tokens
4. Results are collected back into a PySpark DataFrame

This architecture balances the benefits of distributed computing with the cryptographic requirements of token generation.

## Security Notes

- **Secrets Management**: Use secure secrets management systems in production (e.g., AWS Secrets Manager, Azure Key Vault)
- **Network Security**: Ensure secure communication between Spark nodes
- **Data Privacy**: Generated tokens are cryptographically secure and cannot be reversed to original values

## Related Documentation

- [OpenToken Core Library](../python/README.md)
- [Main OpenToken Documentation](../../README.md)
- [Development Guide](../../docs/dev-guide-development.md)

## Contributing

Contributions are welcome! Please see the main OpenToken contributing guidelines.

## License

Copyright (c) Truveta. All rights reserved.
