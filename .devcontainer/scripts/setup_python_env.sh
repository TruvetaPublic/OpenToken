#!/usr/bin/env bash
set -euo pipefail

cd /workspaces/OpenToken/lib/python/opentoken

if [ ! -d .venv ]; then
  python -m venv .venv
fi

# shellcheck disable=SC1091
source .venv/bin/activate

pip install -r requirements.txt -r dev-requirements.txt -e .
