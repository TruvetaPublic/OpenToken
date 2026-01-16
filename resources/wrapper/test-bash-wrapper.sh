#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$REPO_ROOT"

SAMPLE="$REPO_ROOT/resources/sample.csv"
if [[ ! -f "$SAMPLE" ]]; then
  echo "Sample CSV not found: $SAMPLE" >&2
  exit 1
fi

if ! command -v docker >/dev/null; then
  echo "Docker not found in PATH" >&2
  exit 1
fi

SKIP_BUILD=""
if [[ "${OPENTOKEN_SKIP_BUILD:-}" == "1" ]]; then
  SKIP_BUILD="--skip-build"
fi

mkdir -p "$REPO_ROOT/target"
TMP_DIR="$(mktemp -d "$REPO_ROOT/target/bash-wrapper-test-XXXXXX")"
chmod -R 777 "$TMP_DIR"
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

echo "==> Testing run-opentoken.sh (Bash Wrapper)"
export OPENTOKEN_DOCKER_RUN_AS_ROOT=1
receiver_dir="$TMP_DIR/receiver"
sender_dir="$TMP_DIR/sender"
output_zip="$TMP_DIR/output.zip"
output_csv="$TMP_DIR/decrypted.csv"
mkdir -p "$receiver_dir" "$sender_dir"

echo "Step 1: Generate receiver keypair..."
./run-opentoken.sh generate-keypair --output-dir "$receiver_dir" --ecdh-curve P-384 $SKIP_BUILD

echo "Step 2: Generate sender keypair..."
./run-opentoken.sh generate-keypair --output-dir "$sender_dir" --ecdh-curve P-384 $SKIP_BUILD

echo "Step 3: Tokenize (encrypt with ECDH)..."
./run-opentoken.sh tokenize -i "$SAMPLE" -t csv -o "$output_zip" \
  --receiver-public-key "$receiver_dir/public_key.pem" \
  --sender-keypair-path "$sender_dir/keypair.pem" \
  --ecdh-curve P-384 $SKIP_BUILD

echo "Step 4: Decrypt..."
./run-opentoken.sh decrypt -i "$output_zip" -t csv -o "$output_csv" \
  --receiver-keypair-path "$receiver_dir/keypair.pem" \
  --ecdh-curve P-384 $SKIP_BUILD

unset OPENTOKEN_DOCKER_RUN_AS_ROOT

# Verify outputs exist
[[ -s "$output_zip" ]] || { echo "Missing output zip: $output_zip" >&2; exit 1; }
[[ -s "$output_csv" ]] || { echo "Missing decrypted CSV: $output_csv" >&2; exit 1; }

echo ""
echo "✓ All bash wrapper tests passed!"
echo "  Output ZIP: $output_zip"
echo "  Decrypted CSV: $output_csv"
