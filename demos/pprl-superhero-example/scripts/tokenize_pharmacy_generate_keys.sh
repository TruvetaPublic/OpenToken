#!/usr/bin/env bash
#
# Generate a key pair for the pharmacy (receiver) in the ECDH public-key exchange.
#
# This script should be run once by the pharmacy to create their ECDH P-384 key pair.
# The public key is then shared with the hospital (sender).
#
# Output:
#   keys/pharmacy_keypair.pem (private key - KEEP SECRET)
#   keys/pharmacy_public_key.pem (public key - safe to share)
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="${SCRIPT_DIR}/.."
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

echo "============================================================"
echo "Generating Pharmacy Key Pair (ECDH P-384)"
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

mkdir -p "${DEMO_DIR}/keys"

# Check if keys already exist
KEYPAIR_FILE="${DEMO_DIR}/keys/pharmacy_keypair.pem"
PUBLIC_KEY_FILE="${DEMO_DIR}/keys/pharmacy_public_key.pem"
COMPAT_KEYPAIR_FILE="${DEMO_DIR}/keys/keypair.pem"
COMPAT_PUBLIC_FILE="${DEMO_DIR}/keys/public_key.pem"

if [ -f "${KEYPAIR_FILE}" ] && [ -f "${PUBLIC_KEY_FILE}" ]; then
  echo "⚠ Key pair already exists:"
  echo "  - ${KEYPAIR_FILE}"
  echo "  - ${PUBLIC_KEY_FILE}"
  echo ""
  echo "Using existing keys. (Delete the keys directory to regenerate.)"
else
  echo "Generating new ECDH P-384 key pair for pharmacy..."
  echo "  Private Key: ${KEYPAIR_FILE}"
  echo "  Public Key:  ${PUBLIC_KEY_FILE}"
  echo ""

  TEMP_KEY_DIR="${DEMO_DIR}/keys/pharmacy"
  rm -rf "${TEMP_KEY_DIR}"
  mkdir -p "${TEMP_KEY_DIR}"

  java -jar "${JAR_FILE}" generate-keypair \
    --output-dir "${TEMP_KEY_DIR}" \
    --ecdh-curve P-384

  cp "${TEMP_KEY_DIR}/keypair.pem" "${KEYPAIR_FILE}"
  cp "${TEMP_KEY_DIR}/public_key.pem" "${PUBLIC_KEY_FILE}"
fi

# Ensure standard filenames exist for CLI compatibility
cp -f "${KEYPAIR_FILE}" "${COMPAT_KEYPAIR_FILE}"
cp -f "${PUBLIC_KEY_FILE}" "${COMPAT_PUBLIC_FILE}"

echo ""
echo "✓ Pharmacy key pair ready:"
echo "  - Private Key: ${KEYPAIR_FILE} (KEEP SECRET)"
echo "  - Public Key:  ${PUBLIC_KEY_FILE} (share with hospital)"
echo ""
echo "Next step: Share the public key with the hospital:"
echo "  cp ${PUBLIC_KEY_FILE} <path-to-share-with-hospital>/"
echo ""
