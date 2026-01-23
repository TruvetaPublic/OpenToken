#!/usr/bin/env pwsh

param(
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"

$REPO_ROOT = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $REPO_ROOT

$SAMPLE = Join-Path $REPO_ROOT "resources" "sample.csv"
if (-not (Test-Path $SAMPLE)) {
    Write-Error "Sample CSV not found: $SAMPLE"
    exit 1
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker not found in PATH"
    exit 1
}

$SKIP_BUILD_FLAG = ""
if ($SkipBuild -or $env:OPENTOKEN_SKIP_BUILD -eq "1") {
    $SKIP_BUILD_FLAG = "-SkipBuild"
}

$target_dir = Join-Path $REPO_ROOT "target"
if (-not (Test-Path $target_dir)) {
    New-Item -ItemType Directory -Path $target_dir -Force | Out-Null
}

# Create temp directory with random suffix
$random_suffix = -join ((65..90) + (97..122) | Get-Random -Count 6 | ForEach-Object {[char]$_})
$TMP_DIR = Join-Path $target_dir "pwsh-wrapper-test-$random_suffix"
New-Item -ItemType Directory -Path $TMP_DIR -Force | Out-Null

# Cleanup function
$cleanup_script = {
    if (Test-Path $TMP_DIR) {
        Remove-Item -Path $TMP_DIR -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# Register cleanup on exit
$null = Register-EngineEvent -SourceIdentifier PowerShell.Exiting -Action $cleanup_script

Write-Host "==> Testing run-opentoken.ps1 (PowerShell Wrapper)"
$env:OPENTOKEN_DOCKER_RUN_AS_ROOT = "1"

$receiver_dir = Join-Path $TMP_DIR "receiver"
$sender_dir = Join-Path $TMP_DIR "sender"
$output_zip = Join-Path $TMP_DIR "output.zip"
$output_csv = Join-Path $TMP_DIR "decrypted.csv"

New-Item -ItemType Directory -Path $receiver_dir -Force | Out-Null
New-Item -ItemType Directory -Path $sender_dir -Force | Out-Null

Write-Host "Step 1: Generate receiver keypair..."
& ".\run-opentoken.ps1" -c generate-keypair -OutputDir $receiver_dir -EcdhCurve P-384 $SKIP_BUILD_FLAG

Write-Host "Step 2: Generate sender keypair..."
& ".\run-opentoken.ps1" -c generate-keypair -OutputDir $sender_dir -EcdhCurve P-384 $SKIP_BUILD_FLAG

Write-Host "Step 3: Tokenize (encrypt with ECDH)..."
& ".\run-opentoken.ps1" -c tokenize -i $SAMPLE -t csv -o $output_zip `
    -ReceiverPublicKey (Join-Path $receiver_dir "public_key.pem") `
    -SenderKeypairPath (Join-Path $sender_dir "keypair.pem") `
    -EcdhCurve P-384 $SKIP_BUILD_FLAG

Write-Host "Step 4: Decrypt..."
& ".\run-opentoken.ps1" -c decrypt -i $output_zip -t csv -o $output_csv `
    -ReceiverKeypairPath (Join-Path $receiver_dir "keypair.pem") `
    -EcdhCurve P-384 $SKIP_BUILD_FLAG

# Verify outputs exist
if (-not (Test-Path $output_zip) -or (Get-Item $output_zip).Length -eq 0) {
    Write-Error "Missing or empty output zip: $output_zip"
    exit 1
}

if (-not (Test-Path $output_csv) -or (Get-Item $output_csv).Length -eq 0) {
    Write-Error "Missing or empty decrypted CSV: $output_csv"
    exit 1
}

Write-Host ""
Write-Host "✓ All PowerShell wrapper tests passed!" -ForegroundColor Green
Write-Host "  Output ZIP: $output_zip"
Write-Host "  Decrypted CSV: $output_csv"

# Cleanup
& $cleanup_script
