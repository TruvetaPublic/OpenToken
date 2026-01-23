#!/usr/bin/env bash
#
# Run the Superhero PPRL example end-to-end with ECDH public-key exchange:
#  1) Generate datasets
#  2) Generate pharmacy (receiver) key pair
#  3) Hospital tokenizes with ECDH (sender)
#  4) Pharmacy decrypts and performs overlap analysis
#
set -euo pipefail

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="${SCRIPT_DIR}"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DATASETS_DIR="${DEMO_DIR}/datasets"
OUTPUTS_DIR="${DEMO_DIR}/outputs"
KEYS_DIR="${DEMO_DIR}/keys"
JAVA_DIR="${PROJECT_ROOT}/lib/java/opentoken"
VENV_ACTIVATE="${PROJECT_ROOT}/.venv/bin/activate"

echo "============================================================"
echo "Superhero PPRL - ECDH Public-Key Exchange Demo"
echo "============================================================"
echo "Project root: ${PROJECT_ROOT}"
echo "Demo dir    : ${DEMO_DIR}"
echo ""

# Step 0: Python environment (best-effort)
if [[ -f "${VENV_ACTIVATE}" ]]; then
  echo -e "${BLUE}Activating repo Python venv...${NC}"
  # shellcheck disable=SC1090
  source "${VENV_ACTIVATE}"
else
  echo -e "${YELLOW}Repo venv not found at ${VENV_ACTIVATE}.${NC}"
  echo -e "${YELLOW}Continuing with system Python. Ensure 'cryptography' is installed.${NC}"
fi

# Ensure required Python deps for analysis
if ! python -c "import cryptography" >/dev/null 2>&1; then
  echo -e "${YELLOW}Python package 'cryptography' not found. Installing...${NC}"
  python -m pip install --upgrade pip >/dev/null
  python -m pip install cryptography >/dev/null
fi

# Step 1: Generate datasets
echo -e "${BLUE}Step 1/4: Generating datasets...${NC}"
mkdir -p "${DATASETS_DIR}"
python "${DEMO_DIR}/scripts/generate_superhero_datasets.py"
echo ""

# Step 2: Generate pharmacy (receiver) key pair
echo -e "${BLUE}Step 2/4: Generating pharmacy key pair (ECDH P-384)...${NC}"
chmod +x "${DEMO_DIR}/scripts/tokenize_pharmacy_generate_keys.sh" || true
"${DEMO_DIR}/scripts/tokenize_pharmacy_generate_keys.sh"
echo ""

# Step 3: Hospital tokenizes with ECDH
echo -e "${BLUE}Step 3/4: Hospital tokenizes with ECDH (sender)...${NC}"
chmod +x "${DEMO_DIR}/scripts/tokenize_hospital.sh" || true
"${DEMO_DIR}/scripts/tokenize_hospital.sh"
echo ""

# Step 4: Pharmacy decrypts and analyzes overlap
echo -e "${BLUE}Step 4/4: Pharmacy decrypts tokens and analyzes overlap...${NC}"
chmod +x "${DEMO_DIR}/scripts/tokenize_pharmacy_decrypt.sh" || true
"${DEMO_DIR}/scripts/tokenize_pharmacy_decrypt.sh"
echo ""

# Run overlap analysis with ECDH decrypted tokens
echo "Running overlap analysis on ECDH-decrypted tokens..."
python "${DEMO_DIR}/scripts/analyze_overlap.py"
echo ""

echo "============================================================"
echo -e "${GREEN}ECDH Public-Key Exchange demo complete!${NC}"
echo "Outputs:"
echo "  Key Exchange:"
echo "    - ${KEYS_DIR}/pharmacy_keypair.pem (pharmacy's private key)"
echo "    - ${KEYS_DIR}/pharmacy_public_key.pem (pharmacy's public key)"
echo "  Tokenization:"
echo "    - ${OUTPUTS_DIR}/hospital_tokens_ecdh.zip (contains tokens + hospital's public key)"
echo "  Decryption & Analysis:"
echo "    - ${OUTPUTS_DIR}/pharmacy_decrypted_hospital_tokens.csv (decrypted tokens)"
echo "    - ${OUTPUTS_DIR}/matching_records_ecdh.csv (matching record pairs)"
echo "============================================================"