#!/usr/bin/env python3
"""
Decrypt tokens for PPRL demo and extract underlying hashes.
Usage: python decrypt_tokens.py <encryption_key> <input_tokens_csv> <output_hashes_csv>
"""
import sys
import pandas as pd
from opentoken.tokentransformer.decrypt_token_transformer import DecryptTokenTransformer

def main():
    if len(sys.argv) < 4:
        print("Usage: python decrypt_tokens.py <encryption_key> <input_tokens_csv> <output_hashes_csv>")
        sys.exit(1)
    encryption_key = sys.argv[1]
    input_csv = sys.argv[2]
    output_csv = sys.argv[3]
    if len(encryption_key) != 32:
        print("Encryption key must be exactly 32 characters.")
        sys.exit(1)
    df = pd.read_csv(input_csv)
    decryptor = DecryptTokenTransformer(encryption_key)
    for col in df.columns:
        if col.startswith("T"):
            df[col] = df[col].apply(lambda x: decryptor.transform(x) if pd.notna(x) else x)
    df.to_csv(output_csv, index=False)
    print(f"Decrypted hashes written to {output_csv}")

if __name__ == "__main__":
    main()
