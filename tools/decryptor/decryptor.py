"""
OpenToken Decryptor Tool

Decrypts AES-GCM encrypted tokens from CSV files.

Usage: python decryptor.py -e "encryption_key" -i input.csv -o output.csv
Requirements: pycryptodome library, CSV with RuleId,Token,RecordId columns
"""

import argparse
import base64
import csv
from Crypto.Cipher import AES

PROGRAM = 'decryptor.py'
UTF8 = 'utf-8'
BLANK_TOKEN = '0000000000000000000000000000000000000000000000000000000000000000'

def decrypt_tokens(key, input_file, output_file):
    """Decrypt tokens from CSV using AES-GCM. Token format: base64([IV][ciphertext][tag])"""
    with open(output_file, mode='w', encoding=UTF8, newline='') as outfile:
        columns = ['RuleId', 'Token', 'RecordId']
        writer = csv.DictWriter(outfile, fieldnames=columns)
        writer.writeheader()

        with open(input_file) as infile:
            reader = csv.DictReader(infile)
            for row in reader:
                token = row['Token']

                if token != BLANK_TOKEN:
                    # decode the token first
                    decoded_token = base64.b64decode(token)

                    # extract the IV from the decoded token (first 12 bytes)
                    iv = decoded_token[:12]
                    ciphertext = decoded_token[12:]

                    # Decrypt using AES-GCM
                    cipher = AES.new(key.encode(UTF8), AES.MODE_GCM, nonce=iv)
                    try:
                        decrypted_text = cipher.decrypt_and_verify(
                            ciphertext[:-16], ciphertext[-16:])
                        token = decrypted_text.decode(UTF8)
                    except Exception as e:
                        print(token)
                        print(
                            f"Decryption error for RuleId {row['RuleId']}, RecordId {row['RecordId']}: {e}")

                # write the decrypted token back
                writer.writerow({
                    'RuleId': row['RuleId'],
                    'Token': token,
                    'RecordId': row['RecordId']
                })


def parse_args():
    """Parse command-line arguments for encryption key, input file, and output file."""
    parser = argparse.ArgumentParser(
        prog=PROGRAM,
        description='Decrypts tokens from OpenToken CSV files using AES-GCM encryption'
    )
    parser.add_argument('-e', '--encryption-key', required=True, 
                        help='Symmetric encryption key.')
    parser.add_argument('-i', '--input-file', required=True,
                        help='The input file with encrypted tokens.')
    parser.add_argument('-o', '--output-file', required=True,
                        help='The output file with decrypted tokens.')
    return parser.parse_args()

def main():
    """Main entry point - parses args and orchestrates decryption."""
    try:
        args = parse_args()
        print(f'Encryption key: {args.encryption_key}')
        print(f'Input file: {args.input_file}')
        print(f'Output file: {args.output_file}')
        
        decrypt_tokens(args.encryption_key, args.input_file, args.output_file)
        print(
            f'Tokens from {args.input_file} are successfully decrypted and written to {args.output_file}')
    except Exception:
        print('Failed to decrypt tokens')
        raise


if __name__ == "__main__":
    # For direct function testing, uncomment:
    # decrypt_tokens("your-encryption-key", "input.csv", "output.csv")
    main()
