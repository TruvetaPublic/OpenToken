ARG JAVA_VERSION=11
FROM mcr.microsoft.com/devcontainers/java:1-${JAVA_VERSION}-bookworm

# Check for Zscaler certificates and install if found
RUN apt-get update && apt-get install -y ca-certificates openssl python3.11 pip curl && \
    # Check if GitHub API certificate is issued by Zscaler
    echo "===> Checking for Zscaler certificates..." && \
    cert_output=$(echo "" | openssl s_client -showcerts -connect bing.com:443 2>/dev/null) && \
    issuer=$(echo "$cert_output" | openssl x509 -noout -issuer 2>/dev/null || echo "") && \
    echo "===> Certificate issuer: $issuer" && \
    if echo "$issuer" | grep -i "zscaler" > /dev/null; then \
        echo "===> FOUND: Zscaler certificate detected. Extracting root certificate from chain..." && \
        # Extract all certificates from the chain
        echo "$cert_output" | sed -n '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/p' > /tmp/cert_chain.pem && \
        # Split the certificate chain into individual certificates
        csplit -f /tmp/cert_ -b %02d.pem /tmp/cert_chain.pem '/-----BEGIN CERTIFICATE-----/' '{*}' 2>/dev/null || true && \
        # Find the root certificate (self-signed certificate where issuer equals subject)
        root_cert_found=false && \
        for cert_file in /tmp/cert_*.pem; do \
            if [ -s "$cert_file" ]; then \
                # Get issuer and subject for this certificate
                cert_issuer=$(openssl x509 -in "$cert_file" -noout -issuer 2>/dev/null | sed 's/issuer=//') && \
                cert_subject=$(openssl x509 -in "$cert_file" -noout -subject 2>/dev/null | sed 's/subject=//') && \
                echo "===> Checking certificate: $(basename "$cert_file")" && \
                echo "     Subject:  $cert_subject" && \
                echo "     Issuer:   $cert_issuer" && \
                # Check if this is a root certificate (self-signed: issuer == subject)
                if [ "$cert_issuer" = "$cert_subject" ]; then \
                    echo "===> FOUND ROOT: Installing root certificate..." && \
                    # Extract common name for filename
                    cert_cn=$(echo "$cert_subject" | sed -n 's/.*CN=\([^,]*\).*/\1/p' | tr ' ' '_' | tr -cd '[:alnum:]._-') && \
                    root_cert_name="zscaler_root_${cert_cn}.crt" && \
                    cp "$cert_file" "/usr/local/share/ca-certificates/$root_cert_name" && \
                    echo "===> Installed root certificate: $root_cert_name" && \
                    root_cert_found=true; \
                fi; \
            fi; \
        done && \
        # If no self-signed root found, install the last certificate in the chain (likely root)
        if [ "$root_cert_found" = "false" ]; then \
            echo "===> No self-signed root found, installing last certificate in chain as root..." && \
            last_cert=$(ls /tmp/cert_*.pem 2>/dev/null | sort -V | tail -1) && \
            if [ -n "$last_cert" ] && [ -s "$last_cert" ]; then \
                cert_cn=$(openssl x509 -in "$last_cert" -noout -subject 2>/dev/null | sed -n 's/.*CN=\([^,]*\).*/\1/p' | tr ' ' '_' | tr -cd '[:alnum:]._-') && \
                fallback_cert_name="zscaler_chain_root_${cert_cn}.crt" && \
                cp "$last_cert" "/usr/local/share/ca-certificates/$fallback_cert_name" && \
                echo "===> Installed chain root certificate: $fallback_cert_name" && \
                root_cert_found=true; \
            fi; \
        fi && \
        # Update certificate store if we found and installed a root certificate
        if [ "$root_cert_found" = "true" ]; then \
            update-ca-certificates && \
            echo "===> SUCCESS: Zscaler root certificate installed and certificate store updated."; \
        else \
            echo "===> ERROR: No suitable root certificate found in chain."; \
        fi; \
    else \
        echo "===> NOT FOUND: No Zscaler certificate detected. Skipping certificate installation."; \
    fi && \
    # Clean up temporary files
    echo "===> Cleaning up temporary files" && \
    rm -f /tmp/cert_chain.pem /tmp/cert_*.pem && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV REQUESTS_CA_BUNDLE=/etc/ssl/certs/ca-certificates.crt
ENV SSL_CERT_FILE=/etc/ssl/certs/ca-certificates.crt
ENV NODE_OPTIONS=--use-openssl-ca
ENV NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt

USER vscode
RUN pip install bump2version --break-system-packages