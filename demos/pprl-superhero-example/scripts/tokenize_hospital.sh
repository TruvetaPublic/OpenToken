#!/usr/bin/env bash
#
# Tokenize the hospital dataset using OpenToken (Java CLI) with ECDH public-key exchange.
#
# This script demonstrates the public-key workflow:
# 1. Sender (hospital) receives the receiver's (pharmacy) public key
# 2. Sender generates their own key pair (if needed)
# 3. OpenToken performs ECDH key exchange to derive hashing and encryption keys
# 4. Tokens are generated and encrypted
# 5. Output is packaged in a ZIP file with the sender's public key included
#
# Output:
#   outputs/hospital_tokens_ecdh.zip (contains tokens.csv, metadata.json, sender_public_key.pem)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="${SCRIPT_DIR}/.."
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

echo "============================================================"
echo "Tokenizing Hospital Dataset (ECDH Public-Key Exchange)"
echo "============================================================"
echo ""

# Check if OpenToken JAR exists (in opentoken-cli module)
JAR_FILE=$(ls "${PROJECT_ROOT}/lib/java/opentoken-cli/target/opentoken-cli-"*.jar 2>/dev/null | head -1)
if [ -z "${JAR_FILE}" ]; then
  echo "OpenToken JAR not found. Building..."
  (cd "${PROJECT_ROOT}/lib/java" && mvn clean install -DskipTests)
  JAR_FILE=$(ls "${PROJECT_ROOT}/lib/java/opentoken-cli/target/opentoken-cli-"*.jar | head -1)
fi

echo "Using JAR: ${JAR_FILE}"
echo ""

mkdir -p "${DEMO_DIR}/outputs"
mkdir -p "${DEMO_DIR}/keys"

# Check for pharmacy's public key
RECEIVER_PUBLIC_KEY="${DEMO_DIR}/keys/pharmacy_public_key.pem"
if [ ! -f "${RECEIVER_PUBLIC_KEY}" ]; then
  echo "ERROR: Receiver's (pharmacy) public key not found at: ${RECEIVER_PUBLIC_KEY}"
  echo "Please run tokenize_pharmacy_ecdh_generate_keys.sh first to generate the pharmacy's key pair."
  exit 1
fi

echo "Found receiver's public key: ${RECEIVER_PUBLIC_KEY}"
echo ""

# Ensure hospital (sender) key pair exists (keep it in the demo folder)
HOSPITAL_KEYPAIR_FILE="${DEMO_DIR}/keys/hospital_keypair.pem"
HOSPITAL_PUBLIC_KEY_FILE="${DEMO_DIR}/keys/hospital_public_key.pem"
if [ ! -f "${HOSPITAL_KEYPAIR_FILE}" ] || [ ! -f "${HOSPITAL_PUBLIC_KEY_FILE}" ]; then
  echo "Hospital sender key pair not found; generating (ECDH P-384)..."
  TEMP_HOSPITAL_KEY_DIR="${DEMO_DIR}/keys/hospital"
  rm -rf "${TEMP_HOSPITAL_KEY_DIR}"
  mkdir -p "${TEMP_HOSPITAL_KEY_DIR}"

  java -jar "${JAR_FILE}" generate-keypair \
    --output-dir "${TEMP_HOSPITAL_KEY_DIR}" \
    --ecdh-curve P-384

  cp "${TEMP_HOSPITAL_KEY_DIR}/keypair.pem" "${HOSPITAL_KEYPAIR_FILE}"
  cp "${TEMP_HOSPITAL_KEY_DIR}/public_key.pem" "${HOSPITAL_PUBLIC_KEY_FILE}"
fi

echo "Running OpenToken CLI with ECDH key exchange (hospital)..."
echo "  Input: ${DEMO_DIR}/datasets/hospital_superhero_data.csv"
echo "  Receiver's Public Key: ${RECEIVER_PUBLIC_KEY}"
echo "  Sender Key Pair: ${HOSPITAL_KEYPAIR_FILE}"
echo "  Output: ${DEMO_DIR}/outputs/hospital_tokens_ecdh.zip"
echo ""

java -jar "${JAR_FILE}" tokenize \
  -t csv \
  -i "${DEMO_DIR}/datasets/hospital_superhero_data.csv" \
  -o "${DEMO_DIR}/outputs/hospital_tokens_ecdh.zip" \
  --receiver-public-key "${RECEIVER_PUBLIC_KEY}" \
  --sender-keypair-path "${HOSPITAL_KEYPAIR_FILE}" \
  --ecdh-curve P-384

echo ""
echo "Done."
echo "  - ${DEMO_DIR}/outputs/hospital_tokens_ecdh.zip"
echo "    Contains:"
echo "      - tokens.csv (encrypted tokens)"
echo "      - tokens.metadata.json (with key exchange details)"
echo "      - hospital_public_key.pem (hospital's public key for pharmacy to decrypt)"
echo ""
echo "Next step: Share hospital_tokens_ecdh.zip with the pharmacy for decryption."
