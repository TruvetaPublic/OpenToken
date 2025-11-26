#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$SCRIPT_DIR/../.."

# Create venv at repo root
cd "$REPO_ROOT"
if [ ! -d .venv ]; then
  python -m venv .venv
fi

# shellcheck disable=SC1091
source .venv/bin/activate

# Install OpenToken core library
cd "$REPO_ROOT/lib/python/opentoken"
pip install -r requirements.txt -r dev-requirements.txt -e .

# Install PySpark bridge
cd "$REPO_ROOT/lib/python/opentoken-pyspark"
pip install -r requirements.txt -r dev-requirements.txt -e .
