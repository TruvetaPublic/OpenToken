# run-opentoken.ps1
# Convenience script to build and run OpenToken via Docker
# Automatically handles Docker image building and container execution

[CmdletBinding()]
param(
    [Parameter(Mandatory=$false, HelpMessage="Input file path (absolute or relative)")]
    [Alias("i")]
    [string]$InputFile,

    [Parameter(Mandatory=$false, HelpMessage="Output file path (absolute or relative)")]
    [Alias("o")]
    [string]$OutputFile,

    [Parameter(Mandatory=$false, HelpMessage="File type: csv or parquet (default: csv)")]
    [Alias("t")]
    [ValidateSet("csv", "parquet")]
    [string]$FileType = "csv",

    [Parameter(Mandatory=$false, HelpMessage="Hashing secret key")]
    [Alias("h")]
    [string]$HashingSecret,

    [Parameter(Mandatory=$false, HelpMessage="Encryption key")]
    [Alias("e")]
    [string]$EncryptionKey,

    [Parameter(Mandatory=$false, HelpMessage="Docker image name (default: opentoken:latest)")]
    [string]$DockerImage = "opentoken:latest",

    [Parameter(Mandatory=$false, HelpMessage="Skip Docker image build (use existing image)")]
    [Alias("s")]
    [switch]$SkipBuild,

    [Parameter(Mandatory=$false, HelpMessage="Enable verbose output")]
    [Alias("v")]
    [switch]$VerboseOutput,

    [Parameter(Mandatory=$false, HelpMessage="Show help message")]
    [switch]$Help
)

# Function to write colored output
function Write-Info {
    param([string]$Message)
    Write-Host "ℹ " -ForegroundColor Blue -NoNewline
    Write-Host $Message
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ " -ForegroundColor Green -NoNewline
    Write-Host $Message
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "⚠ " -ForegroundColor Yellow -NoNewline
    Write-Host $Message
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ " -ForegroundColor Red -NoNewline
    Write-Host $Message
}

# Function to show usage
function Show-Usage {
    $usage = @"

USAGE:
    run-opentoken.ps1 [OPTIONS]

DESCRIPTION:
    Convenience wrapper for building and running OpenToken via Docker.
    Automatically builds the Docker image if needed and runs OpenToken with specified parameters.

REQUIRED PARAMETERS:
    -InputFile, -i <file>       Input file path (absolute or relative)
    -OutputFile, -o <file>      Output file path (absolute or relative)
    -HashingSecret, -h <key>    Hashing secret key
    -EncryptionKey, -e <key>    Encryption key

OPTIONAL PARAMETERS:
    -FileType, -t <type>        File type: csv or parquet (default: csv)
    -SkipBuild, -s              Skip Docker image build (use existing image)
    -DockerImage <name>         Docker image name (default: opentoken:latest)
    -Verbose, -v                Enable verbose output
    -Help                       Show this help message

EXAMPLES:
    # Basic usage with CSV files
    .\run-opentoken.ps1 -i C:\Data\input.csv -o C:\Data\output.csv -h "MyHashKey" -e "MyEncryptionKey"

    # With parquet files
    .\run-opentoken.ps1 -i .\data\input.parquet -t parquet -o .\data\output.parquet -h "secret" -e "key123"

    # Skip Docker build if image already exists
    .\run-opentoken.ps1 -i .\input.csv -o .\output.csv -h "secret" -e "key" -SkipBuild

    # Verbose mode for troubleshooting
    .\run-opentoken.ps1 -i .\input.csv -o .\output.csv -h "secret" -e "key" -Verbose

NOTES:
    - This script must be run from the OpenToken repository root directory
    - Input and output files are automatically mounted into the Docker container
    - The script will build the Docker image on first run (may take a few minutes)
    - Use -SkipBuild to skip rebuilding the image on subsequent runs

"@
    Write-Host $usage
}

# Show help if requested
if ($Help) {
    Show-Usage
    exit 0
}

# Validate required parameters
if (-not $InputFile) {
    Write-Error-Custom "Input file is required (use -InputFile or -i)"
    Write-Host ""
    Show-Usage
    exit 1
}

if (-not $OutputFile) {
    Write-Error-Custom "Output file is required (use -OutputFile or -o)"
    Write-Host ""
    Show-Usage
    exit 1
}

if (-not $HashingSecret) {
    Write-Error-Custom "Hashing secret is required (use -HashingSecret or -h)"
    Write-Host ""
    Show-Usage
    exit 1
}

if (-not $EncryptionKey) {
    Write-Error-Custom "Encryption key is required (use -EncryptionKey or -e)"
    Write-Host ""
    Show-Usage
    exit 1
}

# Check if Docker is installed
try {
    $dockerVersion = docker --version 2>$null
    if (-not $dockerVersion) {
        throw "Docker not found"
    }
}
catch {
    Write-Error-Custom "Docker is not installed or not in PATH"
    Write-Error-Custom "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
}

# Convert to absolute paths
$InputFile = Resolve-Path -Path $InputFile -ErrorAction SilentlyContinue
if (-not $InputFile) {
    Write-Error-Custom "Input file does not exist: $InputFile"
    exit 1
}

# For output file, create parent directory if it doesn't exist
$OutputFileParent = Split-Path -Parent $OutputFile
if ($OutputFileParent -and -not (Test-Path $OutputFileParent)) {
    New-Item -ItemType Directory -Path $OutputFileParent -Force | Out-Null
}

# Convert output path to absolute (may not exist yet)
if ([System.IO.Path]::IsPathRooted($OutputFile)) {
    $OutputFile = $OutputFile
} else {
    $OutputFile = Join-Path (Get-Location) $OutputFile
}
$OutputFile = [System.IO.Path]::GetFullPath($OutputFile)

# Verify input file exists
if (-not (Test-Path $InputFile)) {
    Write-Error-Custom "Input file does not exist: $InputFile"
    exit 1
}

# Get directory paths for volume mounting
$InputDir = Split-Path -Parent $InputFile
$InputFilename = Split-Path -Leaf $InputFile
$OutputDir = Split-Path -Parent $OutputFile
$OutputFilename = Split-Path -Leaf $OutputFile

# Create output directory if it doesn't exist
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

if ($VerboseOutput) {
    Write-Info "Input file: $InputFile"
    Write-Info "Output file: $OutputFile"
    Write-Info "File type: $FileType"
    Write-Info "Docker image: $DockerImage"
}

# Build Docker image if needed
if (-not $SkipBuild) {
    # Check if image already exists
    $imageExists = docker image inspect $DockerImage 2>$null
    if ($imageExists) {
        Write-Success "Docker image '$DockerImage' already exists locally"
        if ($VerboseOutput) {
            Write-Info "Use -SkipBuild to suppress this check"
        }
    } else {
        Write-Info "Building Docker image: $DockerImage"
        Write-Info "This may take a few minutes on first run..."
        
        if ($VerboseOutput) {
            docker build -t $DockerImage .
        } else {
            docker build -t $DockerImage . 2>&1 | Out-Null
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Docker image built successfully"
        } else {
            Write-Error-Custom "Failed to build Docker image"
            exit 1
        }
    }
} else {
    Write-Info "Skipping Docker build (using existing image)"
    
    # Check if image exists
    $imageExists = docker image inspect $DockerImage 2>$null
    if (-not $imageExists) {
        Write-Error-Custom "Docker image '$DockerImage' not found"
        Write-Error-Custom "Run without -SkipBuild to build the image first"
        exit 1
    }
}

# Run OpenToken via Docker
Write-Info "Running OpenToken..."

# Convert Windows paths to Docker-compatible format (with forward slashes)
$InputDirDocker = $InputDir -replace '\\', '/'
$OutputDirDocker = $OutputDir -replace '\\', '/'

# Handle drive letter for Windows (C:\ becomes /c/)
$InputDirDocker = $InputDirDocker -replace '^([A-Z]):', '/$1'
$OutputDirDocker = $OutputDirDocker -replace '^([A-Z]):', '/$1'

# If input and output are in the same directory, mount once
if ($InputDir -eq $OutputDir) {
    if ($VerboseOutput) {
        Write-Info "Mounting directory: $InputDir"
    }
    
    docker run --rm `
        -v "${InputDir}:/data" `
        $DockerImage `
        -i "/data/$InputFilename" `
        -t $FileType `
        -o "/data/$OutputFilename" `
        -h $HashingSecret `
        -e $EncryptionKey
} else {
    # Mount input and output directories separately
    if ($VerboseOutput) {
        Write-Info "Mounting input directory: $InputDir"
        Write-Info "Mounting output directory: $OutputDir"
    }
    
    docker run --rm `
        -v "${InputDir}:/data/input" `
        -v "${OutputDir}:/data/output" `
        $DockerImage `
        -i "/data/input/$InputFilename" `
        -t $FileType `
        -o "/data/output/$OutputFilename" `
        -h $HashingSecret `
        -e $EncryptionKey
}

if ($LASTEXITCODE -eq 0) {
    Write-Success "OpenToken completed successfully!"
    Write-Success "Output file: $OutputFile"
} else {
    Write-Error-Custom "OpenToken execution failed"
    exit 1
}
