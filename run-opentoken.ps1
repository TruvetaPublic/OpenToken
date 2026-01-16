# run-opentoken.ps1
# Convenience script to build and run OpenToken via Docker
# Automatically handles Docker image building and container execution

# Disable PowerShell's implicit positional binding (except where explicitly enabled)
# so arguments like "--receiver-public-key" are not accidentally treated as values for
# ValidateSet parameters like OutputType.
[CmdletBinding(PositionalBinding = $false)]
param(
    [Parameter(Mandatory = $false, Position = 0, HelpMessage = "Command to run: tokenize, decrypt, or generate-keypair")]
    [ValidateSet("tokenize", "decrypt", "generate-keypair")]
    [Alias("c")]
    [string]$Command = "tokenize",

    [Parameter(Mandatory = $false, HelpMessage = "Input file path (absolute or relative)")]
    [Alias("i", "input")]
    [string]$InputFile,

    [Parameter(Mandatory = $false, HelpMessage = "Output file path (absolute or relative)")]
    [Alias("o", "output")]
    [string]$OutputFile,

    [Parameter(Mandatory = $false, HelpMessage = "File type: csv or parquet (default: csv)")]
    [Alias("t", "type")]
    [ValidateSet("csv", "parquet")]
    [string]$FileType = "csv",

    [Parameter(Mandatory = $false, HelpMessage = "Output type if different from input: csv or parquet")]
    [Alias("ot", "output-type")]
    [ValidateSet("csv", "parquet")]
    [string]$OutputType,

    [Parameter(Mandatory = $false, HelpMessage = "Path to receiver public key PEM (tokenize)")]
    [Alias("receiver-public-key")]
    [string]$ReceiverPublicKey,

    [Parameter(Mandatory = $false, HelpMessage = "Path to sender keypair PEM (tokenize)")]
    [Alias("sender-keypair-path")]
    [string]$SenderKeypairPath,

    [Parameter(Mandatory = $false, HelpMessage = "Path to receiver keypair PEM (decrypt)")]
    [Alias("receiver-keypair-path")]
    [string]$ReceiverKeypairPath,

    [Parameter(Mandatory = $false, HelpMessage = "Path to sender public key PEM (decrypt; optional if input is .zip)")]
    [Alias("sender-public-key")]
    [string]$SenderPublicKey,

    [Parameter(Mandatory = $false, HelpMessage = "Hash-only mode (tokenize): derive hashing key but do not encrypt")]
    [Alias("hash-only")]
    [switch]$HashOnly,

    [Parameter(Mandatory = $false, HelpMessage = "Elliptic curve name for ECDH (default: P-384)")]
    [Alias("ecdh-curve")]
    [string]$EcdhCurve = "P-384",

    [Parameter(Mandatory = $false, HelpMessage = "Output directory for generate-keypair")]
    [Alias("output-dir")]
    [string]$OutputDir,

    [Parameter(Mandatory = $false, HelpMessage = "Docker image name (default: opentoken:latest)")]
    [Alias("image")]
    [string]$DockerImage = "opentoken:latest",

    [Parameter(Mandatory = $false, HelpMessage = "Skip Docker image build (use existing image)")]
    [Alias("s", "skip-build")]
    [switch]$SkipBuild,

    [Parameter(Mandatory = $false, HelpMessage = "Enable verbose output")]
    [Alias("v", "verbose-output")]
    [switch]$VerboseOutput,

    [Parameter(Mandatory = $false, HelpMessage = "Show help message")]
    [switch]$Help,

    # Allow GNU-style args (e.g. --receiver-public-key) to be passed to this script.
    # PowerShell does not bind "--foo" to parameters by default for scripts, so we
    # capture and parse them manually.
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CliArgs
)

# Capture parameters explicitly bound by PowerShell so we can decide whether
# GNU-style args should override defaults.
$script:BoundParameters = $PSBoundParameters

function Apply-CliArgsIfProvided {
    param([string[]]$CliArgsToParse)

    if (-not $CliArgsToParse -or $CliArgsToParse.Count -eq 0) {
        return @{}
    }

    $parsed = @{}

    for ($j = 0; $j -lt $CliArgsToParse.Count; $j++) {
        $rawArg = $CliArgsToParse[$j]
        $arg = $rawArg
        $inlineValue = $null

        if ($rawArg -match '^(--[^=]+)=(.*)$') {
            $arg = $matches[1]
            $inlineValue = $matches[2]
        }

        switch ($arg) {
            "--help" {
                $parsed["Help"] = $true
            }

            "--input" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --input" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["InputFile"] = $val
            }
            "--output" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --output" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["OutputFile"] = $val
            }
            "--type" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --type" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["FileType"] = $val
            }
            "--output-type" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --output-type" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["OutputType"] = $val
            }

            "--receiver-public-key" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --receiver-public-key" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["ReceiverPublicKey"] = $val
            }
            "--sender-keypair-path" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --sender-keypair-path" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["SenderKeypairPath"] = $val
            }
            "--receiver-keypair-path" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --receiver-keypair-path" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["ReceiverKeypairPath"] = $val
            }
            "--sender-public-key" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --sender-public-key" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["SenderPublicKey"] = $val
            }

            "--hash-only" {
                $parsed["HashOnly"] = $true
            }
            "--ecdh-curve" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --ecdh-curve" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["EcdhCurve"] = $val
            }
            "--output-dir" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --output-dir" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["OutputDir"] = $val
            }

            "--skip-build" {
                $parsed["SkipBuild"] = $true
            }
            "--verbose-output" {
                $parsed["VerboseOutput"] = $true
            }
            "--image" {
                $val = $inlineValue
                if ($null -eq $val) {
                    if ($j + 1 -ge $CliArgsToParse.Count) { throw "Missing value for --image" }
                    $j++
                    $val = $CliArgsToParse[$j]
                }
                $parsed["DockerImage"] = $val
            }

            default {
                throw "Unknown option: $arg"
            }
        }
    }

    return $parsed
}

# Function to write script output in a consistent format
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message"
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
    -Command, -c <cmd>          tokenize | decrypt | generate-keypair (default: tokenize)

TOKENIZE:
    -InputFile, -i <file>       Input file path (csv/parquet)
    -OutputFile, -o <file>      Output path (use .zip for packaged output)
    -FileType, -t <type>        csv or parquet
    -ReceiverPublicKey <file>   Receiver public key PEM (required)
    [-SenderKeypairPath <file>] Sender keypair PEM (optional)
    [-HashOnly]                 Hash-only mode (no encryption)
    [-EcdhCurve <curve>]        Curve (default: P-384)

DECRYPT:
    -InputFile, -i <file>       Input token package (.zip) or tokens file
    -OutputFile, -o <file>      Output decrypted file
    -FileType, -t <type>        csv or parquet
    [-ReceiverKeypairPath <file>] Receiver keypair PEM (recommended)
    [-SenderPublicKey <file>]   Sender public key PEM (optional; extracted from ZIP if absent)
    [-EcdhCurve <curve>]        Curve (default: P-384)

GENERATE-KEYPAIR:
    [-OutputDir <dir>]          Directory to write keypair.pem + public_key.pem
    [-EcdhCurve <curve>]        Curve (default: P-384)

OPTIONAL PARAMETERS:
    -FileType, -t <type>        File type: csv or parquet (default: csv)
    -SkipBuild, -s              Skip Docker image build (use existing image)
    -DockerImage <name>         Docker image name (default: opentoken:latest)
    -VerboseOutput, -v          Enable verbose output
    -Help                       Show this help message

EXAMPLES:
    # Generate receiver keypair
    .\run-opentoken.ps1 -c generate-keypair -OutputDir .\keys\receiver -EcdhCurve P-384

    # Sender tokenizes with receiver public key
    .\run-opentoken.ps1 -c tokenize -i .\input.csv -t csv -o .\output.zip \
        -ReceiverPublicKey .\keys\receiver\public_key.pem \
        -SenderKeypairPath .\keys\sender\keypair.pem \
        -EcdhCurve P-384

    # Receiver decrypts the output package
    .\run-opentoken.ps1 -c decrypt -i .\output.zip -t csv -o .\decrypted.csv \
        -ReceiverKeypairPath .\keys\receiver\keypair.pem

    # With parquet files
    .\run-opentoken.ps1 -c tokenize -i .\data\input.parquet -t parquet -o .\data\output.parquet \
        -ReceiverPublicKey .\keys\receiver\public_key.pem

    # Skip Docker build if image already exists
    .\run-opentoken.ps1 -c tokenize -i .\input.csv -t csv -o .\output.zip \
        -ReceiverPublicKey .\keys\receiver\public_key.pem \
        -SkipBuild

    # Verbose mode for troubleshooting
    .\run-opentoken.ps1 -c tokenize -i .\input.csv -t csv -o .\output.zip \
        -ReceiverPublicKey .\keys\receiver\public_key.pem \
        -VerboseOutput

NOTES:
    - This script must be run from the OpenToken repository root directory
    - Input and output files are automatically mounted into the Docker container
    - The script will build the Docker image on first run (may take a few minutes)
    - Use -SkipBuild to skip rebuilding the image on subsequent runs

"@
    Write-Host $usage
}

try {
    $cliParsed = Apply-CliArgsIfProvided -CliArgsToParse $CliArgs
}
catch {
    Write-Info $_.Exception.Message
    Write-Host ""
    Show-Usage
    exit 1
}

if ($cliParsed -and $cliParsed.Count -gt 0) {
    foreach ($k in $cliParsed.Keys) {
        if (-not $PSBoundParameters.ContainsKey($k)) {
            Set-Variable -Name $k -Value $cliParsed[$k]
        }
    }
}

if ($VerboseOutput) {
    $boundKeys = @()
    if ($script:BoundParameters) {
        $boundKeys = $script:BoundParameters.Keys
    }
    Write-Info "Bound parameters: $($boundKeys -join ', ')"
    Write-Info "Remaining args: $($CliArgs -join ' ')"
    if ($cliParsed) {
        Write-Info "Parsed from remaining args: $($cliParsed.Keys -join ', ')"
        Write-Info "Parsed object type: $($cliParsed.GetType().FullName) Count=$($cliParsed.Count)"
    }
    Write-Info "Parsed Command=$Command InputFile=$InputFile OutputFile=$OutputFile FileType=$FileType OutputType=$OutputType"
    Write-Info "Parsed ReceiverPublicKey=$ReceiverPublicKey SenderKeypairPath=$SenderKeypairPath"
}

# Show help if requested
if ($Help) {
    Show-Usage
    exit 0
}

# Validate required parameters by command
switch ($Command) {
    "tokenize" {
        if (-not $InputFile -or -not $OutputFile) {
            Write-Info "tokenize requires -InputFile/-i and -OutputFile/-o"
            Write-Host ""
            Show-Usage
            exit 1
        }
        if (-not $ReceiverPublicKey) {
            Write-Info "tokenize requires -ReceiverPublicKey"
            Write-Host ""
            Show-Usage
            exit 1
        }
    }
    "decrypt" {
        if (-not $InputFile -or -not $OutputFile) {
            Write-Info "decrypt requires -InputFile/-i and -OutputFile/-o"
            Write-Host ""
            Show-Usage
            exit 1
        }
    }
    "generate-keypair" {
        # OutputDir optional (defaults to ~/.opentoken in-container)
    }
    default {
        Write-Info "Unknown command: $Command"
        Write-Host ""
        Show-Usage
        exit 1
    }
}

# Check if Docker is installed
try {
    $dockerVersion = docker --version 2>$null
    if (-not $dockerVersion) {
        throw "Docker not found"
    }
}
catch {
    Write-Info "Docker is not installed or not in PATH"
    Write-Info "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
}

function Get-FullPathIfProvided {
    param([string]$Path)
    if (-not $Path) { return $null }

    # PowerShell does not reliably expand '~' when it's passed as a parameter value
    # and later treated as a plain string. Expand it here so paths like
    # ~/.opentoken/public_key.pem work.
    if ($Path -eq "~") {
        $Path = [Environment]::GetFolderPath("UserProfile")
    }
    elseif ($Path.StartsWith("~/") -or $Path.StartsWith("~\\")) {
        $homeDir = [Environment]::GetFolderPath("UserProfile")
        $suffix = $Path.Substring(2)
        $Path = Join-Path $homeDir $suffix
    }

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return [System.IO.Path]::GetFullPath($Path)
    }
    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Path))
}

$InputFile = Get-FullPathIfProvided $InputFile
$OutputFile = Get-FullPathIfProvided $OutputFile
$ReceiverPublicKey = Get-FullPathIfProvided $ReceiverPublicKey
$SenderKeypairPath = Get-FullPathIfProvided $SenderKeypairPath
$ReceiverKeypairPath = Get-FullPathIfProvided $ReceiverKeypairPath
$SenderPublicKey = Get-FullPathIfProvided $SenderPublicKey
$OutputDir = Get-FullPathIfProvided $OutputDir

if (($Command -eq "tokenize") -or ($Command -eq "decrypt")) {
    if (-not (Test-Path $InputFile)) {
        Write-Info "Input file does not exist: $InputFile"
        exit 1
    }
    $OutputFileParent = Split-Path -Parent $OutputFile
    if ($OutputFileParent -and -not (Test-Path $OutputFileParent)) {
        New-Item -ItemType Directory -Path $OutputFileParent -Force | Out-Null
    }
}

if ($Command -eq "tokenize") {
    if (-not (Test-Path $ReceiverPublicKey)) {
        Write-Info "Receiver public key file does not exist: $ReceiverPublicKey"
        exit 1
    }
}

foreach ($p in @($SenderKeypairPath, $ReceiverKeypairPath, $SenderPublicKey)) {
    if ($p -and -not (Test-Path $p)) {
        Write-Info "Key file does not exist: $p"
        exit 1
    }
}

function Ensure-Mount {
    param(
        [hashtable]$DirToMount,
        [ref]$Index,
        [string]$HostDir,
        [ref]$VolumeArgs
    )
    if (-not $HostDir) { return $null }
    if (-not $DirToMount.ContainsKey($HostDir)) {
        $mountPoint = "/data/m$($Index.Value)"
        $DirToMount[$HostDir] = $mountPoint
        $VolumeArgs.Value += @("-v", "${HostDir}:$mountPoint")
        $Index.Value++
    }
    return $DirToMount[$HostDir]
}

function Container-Path-For-File {
    param(
        [hashtable]$DirToMount,
        [ref]$Index,
        [ref]$VolumeArgs,
        [string]$HostPath
    )
    if (-not $HostPath) { return $null }
    $hostDir = Split-Path -Parent $HostPath
    $base = Split-Path -Leaf $HostPath
    $mountPoint = Ensure-Mount -DirToMount $DirToMount -Index $Index -HostDir $hostDir -VolumeArgs $VolumeArgs
    return "$mountPoint/$base"
}

function Container-Path-For-Dir {
    param(
        [hashtable]$DirToMount,
        [ref]$Index,
        [ref]$VolumeArgs,
        [string]$HostDir
    )
    if (-not $HostDir) { return $null }
    return (Ensure-Mount -DirToMount $DirToMount -Index $Index -HostDir $HostDir -VolumeArgs $VolumeArgs)
}

if ($VerboseOutput) {
    Write-Info "Command: $Command"
    if ($InputFile) { Write-Info "Input: $InputFile" }
    if ($OutputFile) { Write-Info "Output: $OutputFile" }
    Write-Info "Type: $FileType"
    if ($OutputType) { Write-Info "Output type: $OutputType" }
    if ($ReceiverPublicKey) { Write-Info "Receiver public key: $ReceiverPublicKey" }
    if ($SenderKeypairPath) { Write-Info "Sender keypair: $SenderKeypairPath" }
    if ($ReceiverKeypairPath) { Write-Info "Receiver keypair: $ReceiverKeypairPath" }
    if ($SenderPublicKey) { Write-Info "Sender public key: $SenderPublicKey" }
    Write-Info "ECDH curve: $EcdhCurve"
    Write-Info "Hash-only: $($HashOnly.IsPresent)"
    Write-Info "Docker image: $DockerImage"
}

# Build Docker image if needed
if (-not $SkipBuild) {
    # Check if image already exists
    docker image inspect $DockerImage > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Docker image '$DockerImage' already exists locally"
        if ($VerboseOutput) {
            Write-Info "Use -SkipBuild to suppress this check"
        }
    }
    else {
        Write-Info "Building Docker image: $DockerImage"
        Write-Info "This may take a few minutes on first run..."

        if ($VerboseOutput) {
            docker build -t $DockerImage .
        }
        else {
            docker build -t $DockerImage . 2>&1 | Out-Null
        }

        if ($LASTEXITCODE -eq 0) {
            Write-Info "Docker image built successfully"
        }
        else {
            Write-Info "Failed to build Docker image"
            exit 1
        }
    }
}
else {
    Write-Info "Skipping Docker build (using existing image)"
    
    # Check if image exists
    docker image inspect $DockerImage > $null 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Info "Docker image '$DockerImage' not found"
        Write-Info "Run without -SkipBuild to build the image first"
        exit 1
    }
}

Write-Info "Running OpenToken..."

$dirToMount = @{}
$idx = 0
$volumeArgs = @()

$inputContainer = Container-Path-For-File -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostPath $InputFile
$outputContainer = Container-Path-For-File -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostPath $OutputFile
$receiverPubContainer = Container-Path-For-File -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostPath $ReceiverPublicKey
$senderKeypairContainer = Container-Path-For-File -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostPath $SenderKeypairPath
$receiverKeypairContainer = Container-Path-For-File -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostPath $ReceiverKeypairPath
$senderPubContainer = Container-Path-For-File -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostPath $SenderPublicKey
$outputDirContainer = Container-Path-For-Dir -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostDir $OutputDir

$dockerArgs = @($Command)

switch ($Command) {
    "tokenize" {
        $dockerArgs += @("-i", $inputContainer, "-t", $FileType, "-o", $outputContainer)
        if ($OutputType) { $dockerArgs += @("-ot", $OutputType) }
        $dockerArgs += @("--receiver-public-key", $receiverPubContainer)
        if ($SenderKeypairPath) { $dockerArgs += @("--sender-keypair-path", $senderKeypairContainer) }
        if ($HashOnly.IsPresent) { $dockerArgs += "--hash-only" }
        $dockerArgs += @("--ecdh-curve", $EcdhCurve)
    }
    "decrypt" {
        $dockerArgs += @("-i", $inputContainer, "-t", $FileType, "-o", $outputContainer)
        if ($OutputType) { $dockerArgs += @("-ot", $OutputType) }
        if ($SenderPublicKey) { $dockerArgs += @("--sender-public-key", $senderPubContainer) }
        if ($ReceiverKeypairPath) { $dockerArgs += @("--receiver-keypair-path", $receiverKeypairContainer) }
        $dockerArgs += @("--ecdh-curve", $EcdhCurve)
    }
    "generate-keypair" {
        if ($OutputDir) {
            $outDirMount = Container-Path-For-Dir -DirToMount $dirToMount -Index ([ref]$idx) -VolumeArgs ([ref]$volumeArgs) -HostDir $OutputDir
            $dockerArgs += @("--output-dir", $outDirMount)
        }
        $dockerArgs += @("--ecdh-curve", $EcdhCurve)
    }
}

if ($VerboseOutput) {
    Write-Info ("Docker volumes: " + ($volumeArgs -join ' '))
    Write-Info ("Docker command: $DockerImage " + ($dockerArgs -join ' '))
}

docker run --rm @volumeArgs $DockerImage @dockerArgs

if ($LASTEXITCODE -eq 0) {
    Write-Info "OpenToken completed successfully!"
    if ($OutputFile) { Write-Info "Output: $OutputFile" }
}
else {
    Write-Info "OpenToken execution failed"
    exit 1
}