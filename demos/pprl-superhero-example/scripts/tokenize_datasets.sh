#!/bin/bash
#
# Tokenize both hospital and pharmacy datasets using OpenToken
# This script generates encrypted tokens for privacy-preserving record linkage

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
DEMO_DIR="$SCRIPT_DIR/.."
DATASETS_DIR="$DEMO_DIR/datasets"
OUTPUTS_DIR="$DEMO_DIR/outputs"
JAVA_DIR="$PROJECT_ROOT/lib/java"

# Secrets for token generation (CHANGE THESE IN PRODUCTION!)
HASHING_SECRET="SuperHeroHashingKey2024"
ENCRYPTION_KEY="SuperHero-Encryption-Key-32chars"  # Must be exactly 32 characters

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "============================================================"
echo "Tokenizing Superhero PPRL Datasets"
echo "============================================================"
echo ""

# Check if OpenToken JAR exists
JAR_FILE=$(ls "$JAVA_DIR/target/opentoken-"*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo -e "${YELLOW}OpenToken JAR not found. Building...${NC}"
    cd "$JAVA_DIR"
    mvn clean install -DskipTests
    JAR_FILE=$(ls "$JAVA_DIR/target/opentoken-"*.jar | head -1)
fi

echo -e "${GREEN}Using JAR: $JAR_FILE${NC}"
echo ""

# Create outputs directory if it doesn't exist
mkdir -p "$OUTPUTS_DIR"

# Tokenize hospital dataset
echo -e "${BLUE}Tokenizing Hospital Dataset...${NC}"
java -jar "$JAR_FILE" \
    -t csv \
    -i "$DATASETS_DIR/hospital_superhero_data.csv" \
    -o "$OUTPUTS_DIR/hospital_tokens.csv" \
    -h "$HASHING_SECRET" \
    -e "$ENCRYPTION_KEY"
echo ""

# Tokenize pharmacy dataset
echo -e "${BLUE}Tokenizing Pharmacy Dataset...${NC}"
java -jar "$JAR_FILE" \
    -t csv \
    -i "$DATASETS_DIR/pharmacy_superhero_data.csv" \
    -o "$OUTPUTS_DIR/pharmacy_tokens.csv" \
    -h "$HASHING_SECRET" \
    -e "$ENCRYPTION_KEY"
echo ""

echo "============================================================"
echo -e "${GREEN}Tokenization Complete!${NC}"
echo "============================================================"
echo "Output files:"
echo "  - $OUTPUTS_DIR/hospital_tokens.csv"
echo "  - $OUTPUTS_DIR/pharmacy_tokens.csv"
echo "  - $OUTPUTS_DIR/hospital_tokens.csv.metadata.json"
echo "  - $OUTPUTS_DIR/pharmacy_tokens.csv.metadata.json"
echo ""
echo "Next step: Run analyze_overlap.py to measure record linkage"
