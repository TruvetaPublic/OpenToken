#!/usr/bin/env bash
set -euo pipefail

echo "=== Setting up Python environment ==="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$SCRIPT_DIR/../.."

# Create venv at repo root
cd "$REPO_ROOT"
if [ ! -d .venv ]; then
  echo "Creating virtual environment..."
  python -m venv .venv
else
  echo "Virtual environment already exists"
fi

# shellcheck disable=SC1091
source .venv/bin/activate

# Upgrade pip and install wheel to avoid build issues
echo "Upgrading pip, wheel, and setuptools..."
pip install --upgrade pip wheel setuptools

# Install all requirements in a single pip call to reduce file handle usage
echo "Installing Python packages..."
cd "$REPO_ROOT/lib/python"
pip install --no-cache-dir \
  -r opentoken/requirements.txt \
  -r opentoken-pyspark/requirements.txt \
  -r dev-requirements.txt \
  -e opentoken \
  -e opentoken-pyspark

echo "✓ Python environment setup complete"
