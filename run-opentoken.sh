#!/bin/bash

# run-opentoken.sh
# Convenience script to build and run OpenToken via Docker
# Automatically handles Docker image building and container execution

set -e  # Exit on error

# Default values
COMMAND="tokenize"
INPUT_FILE=""
OUTPUT_FILE=""
FILE_TYPE="csv"
OUTPUT_TYPE=""

RECEIVER_PUBLIC_KEY=""
SENDER_KEYPAIR_PATH=""
SENDER_PUBLIC_KEY=""
RECEIVER_KEYPAIR_PATH=""
HASH_ONLY=false
ECDH_CURVE="P-384"
KEYPAIR_OUTPUT_DIR=""
DOCKER_IMAGE="opentoken:latest"
SKIP_BUILD=false
VERBOSE=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
log_info() { echo -e "${BLUE}ℹ${NC} $1"; }
log_success() { echo -e "${GREEN}✓${NC} $1"; }
log_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
log_error() { echo -e "${RED}✗${NC} $1" >&2; }

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Convenience wrapper for building and running OpenToken via Docker.
Automatically builds the Docker image if needed and runs OpenToken with specified parameters.

COMMANDS:
    tokenize                Tokenize person attributes using ECDH (default)
    decrypt                 Decrypt an encrypted token package using ECDH
    generate-keypair        Generate an ECDH keypair

COMMON REQUIRED OPTIONS:
    -c, --command CMD       One of: tokenize, decrypt, generate-keypair (default: tokenize)

TOKENIZE OPTIONS:
    -i, --input FILE              Input file path (csv/parquet)
    -o, --output FILE             Output path. Use .zip for packaged output.
    -t, --type TYPE               Input file type: csv or parquet
    -ot, --output-type TYPE       Output type (csv/parquet). Defaults to input type.
    --receiver-public-key FILE    Receiver public key PEM (required)
    --sender-keypair-path FILE    Sender keypair PEM (optional)
    --hash-only                   Generate hashed tokens without encryption
    --ecdh-curve CURVE            Curve (default: P-384)

DECRYPT OPTIONS:
    -i, --input FILE              Input token package (.zip) or tokens file
    -o, --output FILE             Output decrypted file
    -t, --type TYPE               Input file type: csv or parquet
    -ot, --output-type TYPE       Output type (csv/parquet). Defaults to input type.
    --receiver-keypair-path FILE  Receiver keypair PEM (recommended)
    --sender-public-key FILE      Sender public key PEM (optional; extracted from ZIP if absent)
    --ecdh-curve CURVE            Curve (default: P-384)

GENERATE-KEYPAIR OPTIONS:
    --output-dir DIR              Directory to write keypair.pem + public_key.pem
    --ecdh-curve CURVE            Curve (default: P-384)

OPTIONAL:
    -s, --skip-build              Skip Docker image build (use existing image)
    --image NAME                  Docker image name (default: opentoken:latest)
    -v, --verbose                 Enable verbose output
    --help                        Show this help message

EXAMPLES:
     # Generate receiver keypair
     $0 --command generate-keypair --output-dir ./keys/receiver --ecdh-curve P-384

     # Sender tokenizes input with receiver public key
     $0 --command tokenize -i ./input.csv -t csv -o ./output.zip \
         --receiver-public-key ./keys/receiver/public_key.pem \
         --sender-keypair-path ./keys/sender/keypair.pem \
         --ecdh-curve P-384

     # Receiver decrypts the output package
     $0 --command decrypt -i ./output.zip -t csv -o ./decrypted.csv \
         --receiver-keypair-path ./keys/receiver/keypair.pem

    # With parquet files
    $0 -i ./data/input.parquet -t parquet -o ./data/output.parquet -h "secret" -e "key123"

    # Skip Docker build if image already exists
    $0 -i ./input.csv -o ./output.csv -h "secret" -e "key" --skip-build

    # Verbose mode for troubleshooting
    $0 -i ./input.csv -o ./output.csv -h "secret" -e "key" -v

NOTES:
    - This script must be run from the OpenToken repository root directory
    - Input/output/key directories are mounted into the Docker container as needed
    - The script will build the Docker image on first run (may take a few minutes)
    - Use --skip-build to skip rebuilding the image on subsequent runs

EOF
}

# Allow invoking the script as: ./run-opentoken.sh <command> [OPTIONS]
# e.g. ./run-opentoken.sh tokenize -i ... -o ... --receiver-public-key ...
if [[ $# -gt 0 && "$1" != -* ]]; then
    case "$1" in
        tokenize|decrypt|generate-keypair)
            COMMAND="$1"
            shift
            ;;
    esac
fi

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--command)
            COMMAND="$2"
            shift 2
            ;;
        -i|--input)
            INPUT_FILE="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        -t|--type)
            FILE_TYPE="$2"
            if [[ ! "$FILE_TYPE" =~ ^(csv|parquet)$ ]]; then
                log_error "Invalid file type: $FILE_TYPE. Must be: csv, parquet"
                exit 1
            fi
            shift 2
            ;;
        -ot|--output-type)
            OUTPUT_TYPE="$2"
            if [[ ! "$OUTPUT_TYPE" =~ ^(csv|parquet)$ ]]; then
                log_error "Invalid output type: $OUTPUT_TYPE. Must be: csv, parquet"
                exit 1
            fi
            shift 2
            ;;
        --receiver-public-key)
            RECEIVER_PUBLIC_KEY="$2"
            shift 2
            ;;
        --sender-keypair-path)
            SENDER_KEYPAIR_PATH="$2"
            shift 2
            ;;
        --sender-public-key)
            SENDER_PUBLIC_KEY="$2"
            shift 2
            ;;
        --receiver-keypair-path)
            RECEIVER_KEYPAIR_PATH="$2"
            shift 2
            ;;
        --hash-only)
            HASH_ONLY=true
            shift
            ;;
        --ecdh-curve)
            ECDH_CURVE="$2"
            shift 2
            ;;
        --output-dir)
            KEYPAIR_OUTPUT_DIR="$2"
            shift 2
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --image)
            DOCKER_IMAGE="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        -h|--hash|-e|--encrypt)
            log_error "Shared-secret flags (-h/--hash, -e/--encrypt) are no longer supported."
            log_error "Use ECDH options: --receiver-public-key/--sender-keypair-path and optionally --hash-only."
            exit 1
            ;;
        *)
            log_error "Unknown option: $1"
            log_error "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Validate required parameters by command
case "$COMMAND" in
    tokenize)
        if [[ -z "$INPUT_FILE" || -z "$OUTPUT_FILE" || -z "$FILE_TYPE" ]]; then
            log_error "tokenize requires --input, --output, and --type"
            echo ""
            show_usage
            exit 1
        fi
        if [[ -z "$RECEIVER_PUBLIC_KEY" ]]; then
            log_error "tokenize requires --receiver-public-key"
            echo ""
            show_usage
            exit 1
        fi
        ;;
    decrypt)
        if [[ -z "$INPUT_FILE" || -z "$OUTPUT_FILE" || -z "$FILE_TYPE" ]]; then
            log_error "decrypt requires --input, --output, and --type"
            echo ""
            show_usage
            exit 1
        fi
        ;;
    generate-keypair)
        # output-dir optional (defaults to ~/.opentoken in-container)
        ;;
    *)
        log_error "Unknown command: $COMMAND"
        echo ""
        show_usage
        exit 1
        ;;
esac

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed or not in PATH"
    log_error "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

to_abs_path() {
    local p="$1"
    if [[ -z "$p" ]]; then
        echo ""
        return
    fi
    # realpath -m works even when the target doesn't exist
    realpath -m "$p" 2>/dev/null || echo "$p"
}

INPUT_FILE=$(to_abs_path "$INPUT_FILE")
OUTPUT_FILE=$(to_abs_path "$OUTPUT_FILE")
RECEIVER_PUBLIC_KEY=$(to_abs_path "$RECEIVER_PUBLIC_KEY")
SENDER_KEYPAIR_PATH=$(to_abs_path "$SENDER_KEYPAIR_PATH")
SENDER_PUBLIC_KEY=$(to_abs_path "$SENDER_PUBLIC_KEY")
RECEIVER_KEYPAIR_PATH=$(to_abs_path "$RECEIVER_KEYPAIR_PATH")
KEYPAIR_OUTPUT_DIR=$(to_abs_path "$KEYPAIR_OUTPUT_DIR")

if [[ "$COMMAND" == "tokenize" || "$COMMAND" == "decrypt" ]]; then
    if [[ ! -f "$INPUT_FILE" ]]; then
        log_error "Input file does not exist: $INPUT_FILE"
        exit 1
    fi

    OUTPUT_DIR=$(dirname "$OUTPUT_FILE")
    mkdir -p "$OUTPUT_DIR"
fi

if [[ "$COMMAND" == "tokenize" ]]; then
    if [[ ! -f "$RECEIVER_PUBLIC_KEY" ]]; then
        log_error "Receiver public key file does not exist: $RECEIVER_PUBLIC_KEY"
        exit 1
    fi
fi

if [[ -n "$SENDER_KEYPAIR_PATH" && ! -f "$SENDER_KEYPAIR_PATH" ]]; then
    log_error "Sender keypair file does not exist: $SENDER_KEYPAIR_PATH"
    exit 1
fi

if [[ -n "$SENDER_PUBLIC_KEY" && ! -f "$SENDER_PUBLIC_KEY" ]]; then
    log_error "Sender public key file does not exist: $SENDER_PUBLIC_KEY"
    exit 1
fi

if [[ -n "$RECEIVER_KEYPAIR_PATH" && ! -f "$RECEIVER_KEYPAIR_PATH" ]]; then
    log_error "Receiver keypair file does not exist: $RECEIVER_KEYPAIR_PATH"
    exit 1
fi

if [[ $VERBOSE == true ]]; then
    log_info "Command: $COMMAND"
    [[ -n "$INPUT_FILE" ]] && log_info "Input: $INPUT_FILE"
    [[ -n "$OUTPUT_FILE" ]] && log_info "Output: $OUTPUT_FILE"
    log_info "Type: $FILE_TYPE"
    [[ -n "$OUTPUT_TYPE" ]] && log_info "Output type: $OUTPUT_TYPE"
    [[ -n "$RECEIVER_PUBLIC_KEY" ]] && log_info "Receiver public key: $RECEIVER_PUBLIC_KEY"
    [[ -n "$SENDER_KEYPAIR_PATH" ]] && log_info "Sender keypair: $SENDER_KEYPAIR_PATH"
    [[ -n "$RECEIVER_KEYPAIR_PATH" ]] && log_info "Receiver keypair: $RECEIVER_KEYPAIR_PATH"
    [[ -n "$SENDER_PUBLIC_KEY" ]] && log_info "Sender public key: $SENDER_PUBLIC_KEY"
    log_info "ECDH curve: $ECDH_CURVE"
    log_info "Hash-only: $HASH_ONLY"
    log_info "Docker image: $DOCKER_IMAGE"
fi

# Build Docker image if needed
if [[ $SKIP_BUILD == false ]]; then
    # Check if image already exists
    if docker image inspect "$DOCKER_IMAGE" > /dev/null 2>&1; then
        log_success "Docker image '$DOCKER_IMAGE' already exists locally"
        if [[ $VERBOSE == true ]]; then
            log_info "Use --skip-build to suppress this check"
        fi
    else
        log_info "Building Docker image: $DOCKER_IMAGE"
        log_info "This may take a few minutes on first run..."
        
        if [[ $VERBOSE == true ]]; then
            docker build -t "$DOCKER_IMAGE" .
            BUILD_STATUS=$?
        else
            docker build -t "$DOCKER_IMAGE" . > /dev/null 2>&1
            BUILD_STATUS=$?
        fi
        
        if [[ $BUILD_STATUS -eq 0 ]]; then
            log_success "Docker image built successfully"
        else
            log_error "Failed to build Docker image"
            exit 1
        fi
    fi
else
    log_info "Skipping Docker build (using existing image)"
    
    # Check if image exists
    if ! docker image inspect "$DOCKER_IMAGE" > /dev/null 2>&1; then
        log_error "Docker image '$DOCKER_IMAGE' not found"
        log_error "Run without --skip-build to build the image first"
        exit 1
    fi
fi

log_info "Running OpenToken..."

# Use parallel arrays instead of associative array for bash 3.2 compatibility
mount_dirs=()
mount_points=()
volume_args=()
mount_index=0

ensure_mount_for_dir() {
    local dir="$1"
    local out_var="$2"
    if [[ -z "$dir" ]]; then
        printf -v "$out_var" '%s' ""
        return
    fi

    # Check if directory is already mounted
    local mount_point=""
    local i
    for i in "${!mount_dirs[@]}"; do
        if [[ "${mount_dirs[$i]}" == "$dir" ]]; then
            mount_point="${mount_points[$i]}"
            break
        fi
    done

    if [[ -z "$mount_point" ]]; then
        mount_point="/data/m${mount_index}"
        mount_dirs+=("$dir")
        mount_points+=("$mount_point")
        volume_args+=("-v" "$dir:$mount_point")
        mount_index=$((mount_index + 1))
    fi

    printf -v "$out_var" '%s' "$mount_point"
}

container_path_for_file() {
    local file="$1"
    local out_var="$2"
    if [[ -z "$file" ]]; then
        printf -v "$out_var" '%s' ""
        return
    fi
    local dir
    dir=$(dirname "$file")
    local base
    base=$(basename "$file")
    local mount
    ensure_mount_for_dir "$dir" mount
    printf -v "$out_var" '%s' "$mount/$base"
}

container_path_for_dir() {
    local dir="$1"
    local out_var="$2"
    if [[ -z "$dir" ]]; then
        printf -v "$out_var" '%s' ""
        return
    fi
    local mount
    ensure_mount_for_dir "$dir" mount
    printf -v "$out_var" '%s' "$mount"
}

container_path_for_file "$INPUT_FILE" input_container
container_path_for_file "$OUTPUT_FILE" output_container
container_path_for_file "$RECEIVER_PUBLIC_KEY" receiver_pub_container
container_path_for_file "$SENDER_KEYPAIR_PATH" sender_keypair_container
container_path_for_file "$SENDER_PUBLIC_KEY" sender_pub_container
container_path_for_file "$RECEIVER_KEYPAIR_PATH" receiver_keypair_container
container_path_for_dir "$KEYPAIR_OUTPUT_DIR" keypair_outdir_container

docker_args=("$COMMAND")

case "$COMMAND" in
    tokenize)
        docker_args+=("-i" "$input_container" "-t" "$FILE_TYPE" "-o" "$output_container")
        if [[ -n "$OUTPUT_TYPE" ]]; then
            docker_args+=("-ot" "$OUTPUT_TYPE")
        fi
        docker_args+=("--receiver-public-key" "$receiver_pub_container")
        if [[ -n "$SENDER_KEYPAIR_PATH" ]]; then
            docker_args+=("--sender-keypair-path" "$sender_keypair_container")
        fi
        if [[ "$HASH_ONLY" == true ]]; then
            docker_args+=("--hash-only")
        fi
        docker_args+=("--ecdh-curve" "$ECDH_CURVE")
        ;;
    decrypt)
        docker_args+=("-i" "$input_container" "-t" "$FILE_TYPE" "-o" "$output_container")
        if [[ -n "$OUTPUT_TYPE" ]]; then
            docker_args+=("-ot" "$OUTPUT_TYPE")
        fi
        if [[ -n "$SENDER_PUBLIC_KEY" ]]; then
            docker_args+=("--sender-public-key" "$sender_pub_container")
        fi
        if [[ -n "$RECEIVER_KEYPAIR_PATH" ]]; then
            docker_args+=("--receiver-keypair-path" "$receiver_keypair_container")
        fi
        docker_args+=("--ecdh-curve" "$ECDH_CURVE")
        ;;
    generate-keypair)
        if [[ -n "$KEYPAIR_OUTPUT_DIR" ]]; then
            docker_args+=("--output-dir" "$keypair_outdir_container")
        fi
        docker_args+=("--ecdh-curve" "$ECDH_CURVE")
        ;;
esac

if [[ $VERBOSE == true ]]; then
    log_info "Docker volumes: ${volume_args[*]}"
    log_info "Docker command: $DOCKER_IMAGE ${docker_args[*]}"
fi

docker_user_args=()
if [[ "${OPENTOKEN_DOCKER_RUN_AS_ROOT:-}" == "1" ]]; then
    docker_user_args=("--user" "0:0")
else
    docker_user_args=("--user" "$(id -u):$(id -g)")
fi
docker_env_args=("-e" "HOME=/tmp")

if [[ $VERBOSE == true ]]; then
    log_info "Docker user: ${docker_user_args[*]}"
    log_info "Docker env: ${docker_env_args[*]}"
fi

verify_bind_mounts() {
    local check_cmd="true"

    # Files that must be visible inside the container.
    [[ -n "$input_container" ]] && check_cmd+=" && test -f '$input_container'"
    [[ "$COMMAND" == "tokenize" && -n "$receiver_pub_container" ]] && check_cmd+=" && test -f '$receiver_pub_container'"
    [[ "$COMMAND" == "tokenize" && -n "$sender_keypair_container" ]] && check_cmd+=" && test -f '$sender_keypair_container'"
    [[ "$COMMAND" == "decrypt" && -n "$sender_pub_container" ]] && check_cmd+=" && test -f '$sender_pub_container'"
    [[ "$COMMAND" == "decrypt" && -n "$receiver_keypair_container" ]] && check_cmd+=" && test -f '$receiver_keypair_container'"

    # We keep this intentionally simple: if bind mounts don't work, we fall back to docker cp.
    set +e
    docker run --rm \
        "${docker_user_args[@]}" \
        "${docker_env_args[@]}" \
        "${volume_args[@]}" \
        --entrypoint sh \
        "$DOCKER_IMAGE" \
        -c "$check_cmd" >/dev/null 2>&1
    local status=$?
    set -e
    return $status
}

run_with_bind_mounts() {
    set +e
    docker run --rm \
        "${docker_user_args[@]}" \
        "${docker_env_args[@]}" \
        "${volume_args[@]}" \
        "$DOCKER_IMAGE" \
        "${docker_args[@]}"
    local status=$?
    set -e
    return $status
}

run_with_docker_cp() {
    local workdir="/tmp/opentoken-work"
    local container_id
    container_id=$(docker create \
        "${docker_user_args[@]}" \
        "${docker_env_args[@]}" \
        --entrypoint sh \
        "$DOCKER_IMAGE" \
        -c "tail -f /dev/null")

    cleanup_container() {
        docker rm -f "$container_id" >/dev/null 2>&1 || true
    }
    local previous_exit_trap
    previous_exit_trap=$(trap -p EXIT)
    trap cleanup_container EXIT

    docker start "$container_id" >/dev/null
    docker exec "$container_id" sh -c "mkdir -p '$workdir'"

    local input_in_container=""
    local output_in_container=""
    local receiver_pub_in_container=""
    local sender_keypair_in_container=""
    local sender_pub_in_container=""
    local receiver_keypair_in_container=""
    local keypair_outdir_in_container=""

    if [[ -n "$INPUT_FILE" ]]; then
        input_in_container="$workdir/$(basename "$INPUT_FILE")"
        docker cp "$INPUT_FILE" "$container_id:$input_in_container"
    fi
    if [[ -n "$OUTPUT_FILE" ]]; then
        output_in_container="$workdir/$(basename "$OUTPUT_FILE")"
    fi
    if [[ -n "$RECEIVER_PUBLIC_KEY" ]]; then
        receiver_pub_in_container="$workdir/$(basename "$RECEIVER_PUBLIC_KEY")"
        docker cp "$RECEIVER_PUBLIC_KEY" "$container_id:$receiver_pub_in_container"
    fi
    if [[ -n "$SENDER_KEYPAIR_PATH" ]]; then
        sender_keypair_in_container="$workdir/$(basename "$SENDER_KEYPAIR_PATH")"
        docker cp "$SENDER_KEYPAIR_PATH" "$container_id:$sender_keypair_in_container"
    fi
    if [[ -n "$SENDER_PUBLIC_KEY" ]]; then
        sender_pub_in_container="$workdir/$(basename "$SENDER_PUBLIC_KEY")"
        docker cp "$SENDER_PUBLIC_KEY" "$container_id:$sender_pub_in_container"
    fi
    if [[ -n "$RECEIVER_KEYPAIR_PATH" ]]; then
        receiver_keypair_in_container="$workdir/$(basename "$RECEIVER_KEYPAIR_PATH")"
        docker cp "$RECEIVER_KEYPAIR_PATH" "$container_id:$receiver_keypair_in_container"
    fi
    if [[ -n "$KEYPAIR_OUTPUT_DIR" ]]; then
        keypair_outdir_in_container="$workdir/keys"
        docker exec "$container_id" sh -c "mkdir -p '$keypair_outdir_in_container'"
    fi

    # Rebuild the command to point at in-container paths.
    local cp_args=("$COMMAND")
    case "$COMMAND" in
        tokenize)
            cp_args+=("-i" "$input_in_container" "-t" "$FILE_TYPE" "-o" "$output_in_container")
            [[ -n "$OUTPUT_TYPE" ]] && cp_args+=("-ot" "$OUTPUT_TYPE")
            cp_args+=("--receiver-public-key" "$receiver_pub_in_container")
            [[ -n "$SENDER_KEYPAIR_PATH" ]] && cp_args+=("--sender-keypair-path" "$sender_keypair_in_container")
            [[ "$HASH_ONLY" == true ]] && cp_args+=("--hash-only")
            cp_args+=("--ecdh-curve" "$ECDH_CURVE")
            ;;
        decrypt)
            cp_args+=("-i" "$input_in_container" "-t" "$FILE_TYPE" "-o" "$output_in_container")
            [[ -n "$OUTPUT_TYPE" ]] && cp_args+=("-ot" "$OUTPUT_TYPE")
            [[ -n "$SENDER_PUBLIC_KEY" ]] && cp_args+=("--sender-public-key" "$sender_pub_in_container")
            [[ -n "$RECEIVER_KEYPAIR_PATH" ]] && cp_args+=("--receiver-keypair-path" "$receiver_keypair_in_container")
            cp_args+=("--ecdh-curve" "$ECDH_CURVE")
            ;;
        generate-keypair)
            if [[ -n "$KEYPAIR_OUTPUT_DIR" ]]; then
                cp_args+=("--output-dir" "$keypair_outdir_in_container")
            else
                log_error "When bind-mounts are unavailable, generate-keypair requires --output-dir to copy keys back to the host."
                return 2
            fi
            cp_args+=("--ecdh-curve" "$ECDH_CURVE")
            ;;
    esac

    if [[ $VERBOSE == true ]]; then
        log_warning "Bind mounts are not readable from the Docker daemon; falling back to docker cp workflow."
        log_info "Container workdir: $workdir"
    fi

    set +e
    docker exec "$container_id" java -jar /usr/local/lib/opentoken.jar "${cp_args[@]}"
    local status=$?
    set -e

    if [[ $status -ne 0 ]]; then
        return $status
    fi

    # Copy outputs back to the host.
    if [[ -n "$OUTPUT_FILE" ]]; then
        mkdir -p "$(dirname "$OUTPUT_FILE")"
        if docker exec "$container_id" sh -c "test -f '$output_in_container'" >/dev/null 2>&1; then
            docker cp "$container_id:$output_in_container" "$OUTPUT_FILE"
        else
            log_error "Expected output was not created in container: $output_in_container"
            return 3
        fi
    fi
    if [[ "$COMMAND" == "generate-keypair" && -n "$KEYPAIR_OUTPUT_DIR" ]]; then
        mkdir -p "$KEYPAIR_OUTPUT_DIR"
        docker cp "$container_id:$keypair_outdir_in_container/." "$KEYPAIR_OUTPUT_DIR"
    fi

    cleanup_container
    if [[ -n "$previous_exit_trap" ]]; then
        eval "$previous_exit_trap"
    else
        trap - EXIT
    fi

    return 0
}

docker_status=0

if verify_bind_mounts; then
    run_with_bind_mounts
    docker_status=$?
else
    run_with_docker_cp
    docker_status=$?
fi

if [[ $docker_status -eq 0 ]]; then
    log_success "OpenToken completed successfully!"
    [[ -n "$OUTPUT_FILE" ]] && log_success "Output: $OUTPUT_FILE"
    exit 0
else
    log_error "OpenToken execution failed (exit code: $docker_status)"
    exit "$docker_status"
fi
