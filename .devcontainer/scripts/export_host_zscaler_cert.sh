#!/bin/bash

# Script to export Zscaler Root CA certificate from system certificate store
# Works on both macOS and Windows (using Git Bash or WSL)

# Output location within the .devcontainer directory
OUTPUT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_FILE="$OUTPUT_DIR/zscaler_root.pem"
CERT_NAME="Zscaler Root CA"

echo "Attempting to export Zscaler Root CA certificate..."
echo "Output will be saved to: $OUTPUT_FILE"

# Function to check if we're on macOS
is_macos() {
  [[ "$(uname -s)" == "Darwin" ]]
}

# Function to check if we're on Windows
is_windows() {
  [[ "$(uname -s)" == *"MINGW"* ]] || [[ "$(uname -s)" == *"MSYS"* ]] || [[ -n "$WINDIR" ]] || [[ -n "$windir" ]]
}

# Function to export certificate on macOS
export_cert_macos() {
  echo "Detected macOS system..."
  echo "Attempting to export Zscaler Root CA from Keychain..."

  # Check if certificate exists in keychain
  if security find-certificate -a -c "$CERT_NAME" -Z | grep -q "SHA-1"; then
    echo "Certificate found in keychain. Exporting..."
    
    # Export the certificate
    security find-certificate -a -c "$CERT_NAME" -p > "$OUTPUT_FILE"
    
    if [ $? -eq 0 ] && [ -s "$OUTPUT_FILE" ]; then
      echo "Certificate successfully exported to $OUTPUT_FILE"
      return 0
    else
      echo "Failed to export certificate or export was empty."
      return 1
    fi
  else
    echo "Certificate not found in keychain."
    return 1
  fi
}

# Function to export certificate on Windows
export_cert_windows() {
  echo "Detected Windows system..."
  echo "Attempting to export Zscaler Root CA from Windows certificate store..."
  
  # First, check if PowerShell is available
  if command -v powershell.exe &> /dev/null; then
    echo "Using PowerShell to export certificate..."
    
    # Use PowerShell to find and export the certificate
    powershell.exe -Command "
      \$cert = Get-ChildItem -Path Cert:\LocalMachine\Root | Where-Object {\$_.Subject -match 'CN=$CERT_NAME'} | Select-Object -First 1
      if (\$cert) {
        \$certBytes = \$cert.Export('Cert')
        [System.IO.File]::WriteAllBytes('$OUTPUT_FILE', \$certBytes)
        Write-Host 'Certificate found and exported.'
        exit 0
      } else {
        Write-Host 'Certificate not found in the Root store.'
        # Try the CurrentUser store as well
        \$cert = Get-ChildItem -Path Cert:\CurrentUser\Root | Where-Object {\$_.Subject -match 'CN=$CERT_NAME'} | Select-Object -First 1
        if (\$cert) {
          \$certBytes = \$cert.Export('Cert')
          [System.IO.File]::WriteAllBytes('$OUTPUT_FILE', \$certBytes)
          Write-Host 'Certificate found in CurrentUser store and exported.'
          exit 0
        } else {
          Write-Host 'Certificate not found in CurrentUser store either.'
          exit 1
        }
      }
    "
    
    if [ $? -eq 0 ] && [ -s "$OUTPUT_FILE" ]; then
      echo "Certificate successfully exported to $OUTPUT_FILE"
      # Convert DER format to PEM if needed
      if ! grep -q "BEGIN CERTIFICATE" "$OUTPUT_FILE"; then
        echo "Converting certificate from DER to PEM format..."
        DER_FILE="${OUTPUT_FILE}.der"
        mv "$OUTPUT_FILE" "$DER_FILE"
        openssl x509 -inform DER -in "$DER_FILE" -out "$OUTPUT_FILE"
        rm "$DER_FILE"
      fi
      return 0
    else
      echo "Failed to export certificate or export was empty."
      return 1
    fi
  else
    echo "PowerShell not available. Cannot export certificate on Windows without PowerShell."
    return 1
  fi
}

# Main script execution
if is_macos; then
  export_cert_macos
elif is_windows; then
  export_cert_windows
else
  echo "This script only supports macOS and Windows. Detected OS: $(uname -s)"
  exit 1
fi

# Check if we successfully exported a certificate
if [ $? -ne 0 ] || [ ! -s "$OUTPUT_FILE" ]; then
  echo "Failed to export Zscaler Root CA certificate."
  # Remove empty output file if it exists
  [ -f "$OUTPUT_FILE" ] && rm "$OUTPUT_FILE"
  exit 1
fi

echo "Zscaler certificate successfully exported and ready for container use."
exit 0
