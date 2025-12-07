# PPRL Superhero Example Demo

This demonstration shows privacy-preserving record linkage (PPRL) using OpenToken, simulating two organizations (Superhero Hospital and Pharmacy) with 40% patient overlap.

**Requirements**: Python 3, pandas, Java 11+, Maven  
**Optional**: PySpark (only needed for distributed processing in the Jupyter notebook)

## Overview

Privacy-Preserving Record Linkage (PPRL) enables comparison of encrypted records across organizations without revealing the underlying data. This demo illustrates:

- **Data Generation**: Creates two themed datasets (hospital and pharmacy) with controlled 40% overlap
- **Independent Tokenization**: Each organization tokenizes independently using shared secrets
- **Encryption**: AES-256-GCM encryption with random IVs per record prevents direct token comparison
- **Decryption & Analysis**: Decrypt tokens to extract deterministic hashes for matching
- **Overlap Analysis**: Identifies matching records by comparing all 5 tokens
- **Distributed Processing** (optional): Demonstrates PySpark for large-scale analysis

## Key Challenge Addressed

OpenToken uses **AES-256-GCM with random IVs per encryption**. This means independently tokenized datasets produce different encrypted tokens for identical data, preventing direct comparison. This demo shows how to decrypt tokens to compare the underlying deterministic HMAC-SHA256 hashes.

## Quick Start

### Basic Demo (Default Secrets)

```bash
cd demos/pprl-superhero-example/scripts
./run_complete_demo.sh
```

### Custom Secrets

```bash
cd demos/pprl-superhero-example/scripts
./run_complete_demo.sh "YourHashingSecret" "Your-32-Character-Encryption-Key"
```

### Interactive Jupyter Notebook (Optional - includes optional PySpark support)

```bash
cd demos/pprl-superhero-example
./run_notebook.sh
```

Then open the notebook URL in your browser to explore the demo interactively.

**Note**: The notebook includes optional PySpark code for distributed processing. If PySpark is not installed, those cells will skip gracefully and the core demo will still work.

## Workflow

### 1. Generate Datasets

Creates hospital and pharmacy CSVs with 40% overlap on all person attributes:

```bash
python scripts/generate_superhero_datasets.py hospital.csv pharmacy.csv
```

**Output:**
- `hospital.csv` (100 records, 40 shared with pharmacy)
- `pharmacy.csv` (120 records, 40 shared with hospital)

### 2. Tokenize Datasets

Each organization tokenizes independently using shared secrets (via Java CLI):

```bash
bash scripts/tokenize_datasets.sh "HashingKey" "Secret-Encryption-Key-Goes-Here."
```

**Output:**
- `hospital.tokens.csv` (Hospital records with T1-T5 encrypted tokens)
- `pharmacy.tokens.csv` (Pharmacy records with T1-T5 encrypted tokens)

**Note:** Due to random IVs in AES-GCM, identical data produces different encrypted tokens even with the same secrets.

### 3. Decrypt Tokens & Analyze Overlap

Decrypt tokens to extract hashes and find matches:

```bash
python scripts/analyze_overlap.py "Secret-Encryption-Key-Goes-Here."
```

**Output:**
- `matched_records.csv` (Records matching on all 5 tokens)
- Console output showing match count

**Expected Result:** ~40 matching records (40% of hospital records)

## Scripts

### `generate_superhero_datasets.py`

Creates datasets with controlled overlap using superhero-themed names.

**Usage:**
```bash
python generate_superhero_datasets.py <hospital_path> <pharmacy_path> [overlap_pct] [seed]
```

**Parameters:**
- `hospital_path`: Output path for hospital CSV
- `pharmacy_path`: Output path for pharmacy CSV
- `overlap_pct`: Overlap percentage (default: 0.4)
- `seed`: Random seed for reproducibility (default: 42)

### `tokenize_datasets.sh`

Tokenizes both datasets using OpenToken Java CLI.

**Usage:**
```bash
./tokenize_datasets.sh <hash_key> <encryption_key>
```

**Parameters:**
- `hash_key`: HMAC-SHA256 hashing secret (default: "HashingKey")
- `encryption_key`: AES-256 encryption key, must be exactly 32 chars (default: "Secret-Encryption-Key-Goes-Here.")

### `decrypt_tokens.py`

Decrypts tokens and extracts underlying hashes from a CSV file.

**Usage:**
```bash
python decrypt_tokens.py <encryption_key> <input_csv> <output_csv>
```

**Parameters:**
- `encryption_key`: Must match the key used during tokenization (exactly 32 chars)
- `input_csv`: CSV file containing encrypted tokens (with T1-T5 columns)
- `output_csv`: Output file with decrypted hashes

### `analyze_overlap.py`

Analyzes overlap between two decrypted token datasets.

**Usage:**
```bash
python analyze_overlap.py <encryption_key>
```

**Assumes:**
- `../hospital.tokens.csv` exists
- `../pharmacy.tokens.csv` exists
- Encryption key matches tokenization key

**Output:**
- `../matched_records.csv`: Merged records matching on all 5 tokens
- Console output: Match count and percentage

### `run_complete_demo.sh`

Runs the complete workflow in one command.

**Usage:**
```bash
./run_complete_demo.sh [hash_key] [encryption_key]
```

**What it does:**
1. Generates datasets
2. Tokenizes both datasets
3. Analyzes overlap
4. Reports results

## Jupyter Notebook

### `PPRL_Superhero_PySpark_Demo.ipynb`

Interactive notebook demonstrating:

1. **Setup & Dependencies**: Environment configuration
2. **Library Imports**: PySpark, pandas, OpenToken modules
3. **PySpark Configuration**: SparkSession initialization
4. **Data Loading**: Generate and load datasets into Spark DataFrames
5. **PR #44 Integration**: Tokenize using new scripts
6. **Script Validation**: Run and validate integration
7. **PySpark Transformations**: Distributed decryption and overlap analysis
8. **Results Display**: Summary statistics and matched records

**Run:**
```bash
./run_notebook.sh
# Then open the URL in your browser
```

## Output Files

| File | Description |
|------|-------------|
| `hospital.csv` | Hospital dataset (100 records) |
| `pharmacy.csv` | Pharmacy dataset (120 records) |
| `hospital.tokens.csv` | Encrypted hospital tokens |
| `pharmacy.tokens.csv` | Encrypted pharmacy tokens |
| `matched_records.csv` | Records matching on all 5 tokens (~40 records) |

## Key Findings

**Expected Results:**
- Hospital records: 100
- Pharmacy records: 120
- Matched records: ~40 (40% overlap on hospital records)

**What this demonstrates:**
- Independent organizations can tokenize securely
- Decryption is needed to compare encrypted tokens
- Deterministic hashes enable accurate matching
- PySpark is optional for distributed processing at scale

## Security Considerations

### Privacy Implications

- **Decryption reveals hashes**: Hashes are deterministic HMAC-SHA256 values, not raw person data
- **No raw data exposure**: Original attributes (names, SSNs, dates) are never decrypted
- **Match-only decryption**: Only decrypt for comparison purposes in controlled environments

### Real-World Deployment Patterns

1. **Trusted Third Party (TTP)**: A neutral organization handles decryption and matching
2. **Secure Multi-Party Computation (SMPC)**: Distributed computation without centralized decryption
3. **Hardware Security Module (HSM)**: Encryption/decryption in secure hardware
4. **Homomorphic Encryption**: Compute on encrypted data without decryption

### Key Management Best Practices

- **Separate secrets**: Use different encryption keys for different organizations
- **Secure exchange**: Use TLS or secure key exchange protocols
- **Audit logging**: Log all decryption operations
- **Key rotation**: Implement periodic key rotation policies
- **Access control**: Restrict decryption to authorized personnel

## Customization

### Modify Overlap Percentage

```python
# In generate_superhero_datasets.py
# Change overlap_pct parameter:
generate_superhero_datasets("hospital.csv", "pharmacy.csv", overlap_pct=0.5)
```

### Modify Dataset Size

```python
# In generate_superhero_datasets.py, change n_hospital and n_pharmacy:
n_hospital = 500
n_pharmacy = 600
```

### Use Different Secrets

All scripts accept command-line secrets:

```bash
./run_complete_demo.sh "MyHashKey123" "MyEncryptionKey-32chars-long!!!"
```

## Troubleshooting

### Encryption Key Invalid

Error: "Encryption key must be exactly 32 characters."

**Solution:** Ensure encryption key is exactly 32 characters:
```bash
# Count characters
echo -n "Secret-Encryption-Key-Goes-Here." | wc -c
# Output: 32
```

### Import Errors

Error: "ModuleNotFoundError: No module named 'opentoken'"

**Solution:** Install OpenToken CLI:
```bash
pip install -e /workspaces/OpenToken/lib/python/opentoken-cli
```

### Tokenization Fails

Error: "Invalid Argument. Key must be 32 characters long"

**Solution:** Check key length:
```bash
# Use default keys if unsure
./run_complete_demo.sh
```

## Next Steps

- **Scale to larger datasets**: Use PySpark on distributed cluster
- **Add more token rules**: Extend T1-T5 with domain-specific rules
- **Integrate with real data**: Adapt scripts for your person attributes
- **Implement SMPC**: Use secure computation for decentralized matching

## PySpark Integration

The included notebook demonstrates:

1. **Loading encrypted tokens into Spark DataFrames**
2. **Creating UDFs for distributed decryption**
3. **PySpark joins for overlap analysis**
4. **Performance metrics on distributed clusters**

This enables PPRL at scale with:
- Parallel decryption across cluster nodes
- Efficient distributed joins
- Built-in fault tolerance
- Support for terabytes of encrypted tokens

## References

- [OpenToken README](../../README.md)
- [Development Guide](../../docs/dev-guide-development.md)
- [OpenToken PySpark Module](../../lib/python/opentoken-pyspark)
- [Token Encryption Process](../../README.md#token-encryption-process)

## License

This demonstration is part of the OpenToken project. See [LICENSE](../../LICENSE) for details.
