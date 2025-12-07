#!/usr/bin/env python3
"""
Analyze overlap between two decrypted token datasets for PPRL demo.
Usage: python analyze_overlap.py <encryption_key>
"""
import sys
import pandas as pd
import os
from opentoken.tokentransformer.decrypt_token_transformer import DecryptTokenTransformer

def main():
    if len(sys.argv) < 2:
        print("Usage: python analyze_overlap.py <encryption_key>")
        sys.exit(1)
    encryption_key = sys.argv[1]
    if len(encryption_key) != 32:
        print("Encryption key must be exactly 32 characters.")
        sys.exit(1)
    # Decrypt hospital tokens
    hospital_tokens = pd.read_csv("../hospital.tokens.csv")
    pharmacy_tokens = pd.read_csv("../pharmacy.tokens.csv")
    decryptor = DecryptTokenTransformer(encryption_key)
    for col in hospital_tokens.columns:
        if col.startswith("T"):
            hospital_tokens[col] = hospital_tokens[col].apply(lambda x: decryptor.transform(x) if pd.notna(x) else x)
    for col in pharmacy_tokens.columns:
        if col.startswith("T"):
            pharmacy_tokens[col] = pharmacy_tokens[col].apply(lambda x: decryptor.transform(x) if pd.notna(x) else x)
    # Find matches (all 5 tokens must match)
    merged = pd.merge(hospital_tokens, pharmacy_tokens, on=["T1","T2","T3","T4","T5"])
    print(f"Found {len(merged)} matching records.")
    merged.to_csv("../matched_records.csv", index=False)
    print("Matched records written to ../matched_records.csv")

if __name__ == "__main__":
    main()
