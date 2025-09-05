ARG JAVA_VERSION=11
FROM mcr.microsoft.com/devcontainers/java:1-${JAVA_VERSION}-bookworm

# Check for Zscaler certificates and install if found using external script
COPY .devcontainer/scripts/detect_zscaler.sh /tmp/

# Copy host-provided certificate if it exists (will be skipped if file doesn't exist)
COPY .devcontainer/scripts/zscaler_root.pem* /tmp/host_zscaler_cert.pem

RUN apt-get update && apt-get install -y ca-certificates openssl python3 python3-pip pipx curl && \
    # Make the script executable and run it
    chmod +x /tmp/detect_zscaler.sh && \
    /tmp/detect_zscaler.sh && \
    # Register python as a synonym for python3
    ln -s /usr/bin/python3 /usr/bin/python && \
    # Clean up temporary files
    echo "===> Cleaning up temporary files" && \
    rm -rf /tmp/detect_zscaler.sh /tmp/zscaler_certs /tmp/host_zscaler_cert.pem && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt
ENV SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt
ENV NODE_OPTIONS=--use-openssl-ca
ENV NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt

USER vscode

COPY lib/python/dev-requirements.txt /tmp/dev-requirements.txt
COPY lib/python/requirements.txt /tmp/requirements.txt

RUN pip install -r /tmp/dev-requirements.txt --break-system-packages