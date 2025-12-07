#!/bin/bash
# Run complete PPRL demo workflow
# Usage: ./run_complete_demo.sh <hash_key> <encryption_key>
set -e
HASH_KEY=${1:-"HashingKey"}
ENCRYPTION_KEY=${2:-"Secret-Encryption-Key-Goes-Here."}
if [ ${#ENCRYPTION_KEY} -ne 32 ]; then
  echo "Encryption key must be exactly 32 characters." >&2
  exit 1
fi
# Step 1: Generate datasets
python ./generate_superhero_datasets.py ../hospital.csv ../pharmacy.csv
# Step 2: Tokenize datasets
./tokenize_datasets.sh "$HASH_KEY" "$ENCRYPTION_KEY"
# Step 3: Analyze overlap
python ./analyze_overlap.py "$ENCRYPTION_KEY"
echo "Demo complete. See ../matched_records.csv for results."
