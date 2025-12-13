#!/usr/bin/env bash
#
# Backward-compatible wrapper.
#
# This demo now has two separate scripts:
#   - tokenize_hospital.sh
#   - tokenize_pharmacy.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${SCRIPT_DIR}/tokenize_hospital.sh"
echo ""
"${SCRIPT_DIR}/tokenize_pharmacy.sh"
