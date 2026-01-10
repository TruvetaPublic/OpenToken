#!/usr/bin/env bash
#
# Tokenize the pharmacy dataset using OpenToken (Java CLI) with ECDH public-key exchange.
#
# This script demonstrates the receiver (pharmacy) side of the public-key workflow:
# 1. Pharmacy receives the encrypted token ZIP from the hospital
# 2. Pharmacy extracts the hospital's public key from the ZIP
# 3. OpenToken performs ECDH key exchange with the hospital's public key
# 4. Tokens are decrypted to get the underlying HMAC-SHA256 hashes
# 5. Pharmacy performs record linkage by comparing decrypted hashes
#
# Prerequisites:
#   - hospital_tokens_ecdh.zip (received from hospital)
#   - pharmacy private key at: keys/pharmacy_keypair.pem
#
# Output:
#   outputs/pharmacy_decrypted_tokens.csv (decrypted hashes for comparison)
#   outputs/matching_records_ecdh.csv (matching record pairs)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="${SCRIPT_DIR}/.."
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

echo "============================================================"
echo "Pharmacy: Decrypt Hospital Tokens (ECDH Public-Key Exchange)"
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

# Check for hospital's encrypted token package
HOSPITAL_TOKENS_ZIP="${DEMO_DIR}/outputs/hospital_tokens_ecdh.zip"
if [ ! -f "${HOSPITAL_TOKENS_ZIP}" ]; then
  echo "ERROR: Hospital token package not found at: ${HOSPITAL_TOKENS_ZIP}"
  echo "Please run tokenize_hospital_ecdh.sh first."
  exit 1
fi

echo "Found hospital token package: ${HOSPITAL_TOKENS_ZIP}"
echo ""

echo "Running OpenToken CLI to decrypt hospital tokens (pharmacy side)..."
echo "  Input: ${HOSPITAL_TOKENS_ZIP}"
echo "  Receiver Private Key: ${DEMO_DIR}/keys/pharmacy_keypair.pem"
echo "  Output: ${DEMO_DIR}/outputs/pharmacy_decrypted_hospital_tokens.csv"
echo ""

java -jar "${JAR_FILE}" --decrypt-with-ecdh \
  -t csv \
  -i "${HOSPITAL_TOKENS_ZIP}" \
  -o "${DEMO_DIR}/outputs/pharmacy_decrypted_hospital_tokens.csv" \
  --receiver-keypair-path "${DEMO_DIR}/keys/pharmacy_keypair.pem"

echo ""
echo "Done."
echo "  - ${DEMO_DIR}/outputs/pharmacy_decrypted_hospital_tokens.csv"
echo ""
echo "Note: Decrypted hashes can now be compared for record linkage."
