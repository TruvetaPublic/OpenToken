#!/bin/bash
# Tokenize datasets for PPRL demo using Java CLI
# Usage: ./tokenize_datasets.sh <hash_key> <encryption_key>
set -e
HASH_KEY=${1:-"HashingKey"}
ENCRYPTION_KEY=${2:-"Secret-Encryption-Key-Goes-Here."}
if [ ${#ENCRYPTION_KEY} -ne 32 ]; then
  echo "Encryption key must be exactly 32 characters." >&2
  exit 1
fi
# Get the Java CLI JAR location
OPENTOKEN_JAR="/workspaces/OpenToken/lib/java/opentoken-cli/target/opentoken-cli-1.11.0-jar-with-dependencies.jar"
if [ ! -f "$OPENTOKEN_JAR" ]; then
  echo "OpenToken CLI JAR not found. Building Java module..."
  cd /workspaces/OpenToken/lib/java
  mvn clean install -q
  cd - > /dev/null
fi
# Tokenize hospital
java -jar "$OPENTOKEN_JAR" \
  --input ../hospital.csv --type csv --output ../hospital.tokens.csv \
  -h "$HASH_KEY" -e "$ENCRYPTION_KEY"
# Tokenize pharmacy
java -jar "$OPENTOKEN_JAR" \
  --input ../pharmacy.csv --type csv --output ../pharmacy.tokens.csv \
  -h "$HASH_KEY" -e "$ENCRYPTION_KEY"
echo "Tokenization complete."
