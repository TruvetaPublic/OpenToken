# Zscaler Certificate Management

This directory contains scripts to automatically handle Zscaler certificate issues in development environments.

## For Developers (Before Building Container)

If your organization uses Zscaler for SSL inspection, you'll need to export the Zscaler Root CA certificate from your system to enable secure connections inside the container.

### Step 1: Export your Zscaler certificate

#### Option 1: Using Bash (macOS, Git Bash, or WSL on Windows)

Run the following command from the project root directory:

```bash
./.devcontainer/scripts/export_host_zscaler_cert.sh
```

#### Option 2: Using PowerShell (Windows)

Run the following command from the project root directory in PowerShell:

```powershell
.\.devcontainer\scripts\Export-ZscalerCert.ps1
```

Either script will extract the Zscaler Root CA certificate from your system and save it to `.devcontainer/scripts/zscaler_root.pem`.

### Step 2: Build or rebuild your container

The container will automatically use the exported certificate.

## How It Works

1. One of the export scripts extracts the Zscaler Root CA from your host system:
   - `export_host_zscaler_cert.sh` for macOS/Linux/Git Bash users
   - `Export-ZscalerCert.ps1` for Windows PowerShell users
2. The Dockerfile copies this certificate into the container during build
3. The `detect_zscaler.sh` script installs the certificate in the container's certificate store

If no host certificate is found, the container will attempt to auto-detect Zscaler by making SSL connections and analyzing certificate chains.

## Manual Certificate Installation

If automatic detection fails, you can manually add a certificate by:

1. Obtaining the Zscaler Root CA certificate (from your browser or system certificate store)
2. Saving it as `.devcontainer/scripts/zscaler_root.pem`
3. Rebuilding the container
