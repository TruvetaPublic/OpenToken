"""
Analyze overlap between tokenized hospital and pharmacy datasets.

This script performs privacy-preserving record linkage by comparing tokens
from both datasets. Since OpenToken uses random IVs for AES-GCM encryption,
tokens must be decrypted before comparison to get the underlying HMAC-SHA256
hash values, which are deterministic and comparable.
"""
import csv
import json
from collections import defaultdict
import base64
from cryptography.hazmat.primitives.ciphers.aead import AESGCM


def decrypt_token(encrypted_token, encryption_key):
    """
    Decrypt an OpenToken encrypted token to get the underlying hash.
    
    Args:
        encrypted_token: Base64-encoded encrypted token with prepended IV
        encryption_key: 32-character encryption key
    
    Returns:
        Decrypted token (HMAC-SHA256 hash in Base64)
    """
    try:
        # Decode the base64-encoded message
        message_bytes = base64.b64decode(encrypted_token)
        
        # Extract IV (first 12 bytes) and ciphertext+tag (remaining bytes)
        iv = message_bytes[:12]
        ciphertext_and_tag = message_bytes[12:]
        
        # Create AESGCM cipher and decrypt
        aesgcm = AESGCM(encryption_key.encode('utf-8'))
        decrypted_bytes = aesgcm.decrypt(iv, ciphertext_and_tag, None)
        return decrypted_bytes.decode('utf-8')
    except Exception as e:
        raise ValueError(f"Decryption failed for token: {e}")


def load_tokens(csv_file, encryption_key):
    """
    Load tokens from CSV file, decrypt them, and organize by RecordId and RuleId.
    
    Args:
        csv_file: Path to CSV file with encrypted tokens
        encryption_key: 32-character encryption key used for tokenization
    
    Returns:
        Dictionary mapping RecordId -> RuleId -> decrypted_token
    """
    tokens_by_record = defaultdict(dict)
    
    with open(csv_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            record_id = row['RecordId']
            rule_id = row['RuleId']
            encrypted_token = row['Token']
            
            # Decrypt the token to get the underlying hash
            decrypted_token = decrypt_token(encrypted_token, encryption_key)
            tokens_by_record[record_id][rule_id] = decrypted_token
    
    return tokens_by_record


def load_metadata(json_file):
    """Load metadata from JSON file."""
    try:
        with open(json_file, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        return None


def find_matches(hospital_tokens, pharmacy_tokens, required_token_matches=5):
    """
    Find matching records between hospital and pharmacy datasets.
    
    A match is defined as having all 5 tokens (T1-T5) identical between two records.
    
    Returns:
        matches: List of tuples (hospital_record_id, pharmacy_record_id, matching_tokens)
    """
    matches = []
    token_ids = ['T1', 'T2', 'T3', 'T4', 'T5']
    
    print(f"Searching for matches (requiring {required_token_matches} matching tokens)...")
    print()
    
    for hospital_id, hospital_token_set in hospital_tokens.items():
        for pharmacy_id, pharmacy_token_set in pharmacy_tokens.items():
            # Count matching tokens
            matching_tokens = []
            for token_id in token_ids:
                if token_id in hospital_token_set and token_id in pharmacy_token_set:
                    if hospital_token_set[token_id] == pharmacy_token_set[token_id]:
                        matching_tokens.append(token_id)
            
            # Check if we have enough matching tokens
            if len(matching_tokens) >= required_token_matches:
                matches.append((hospital_id, pharmacy_id, matching_tokens))
    
    return matches


def analyze_token_distribution(tokens, dataset_name):
    """Analyze the distribution of tokens in a dataset."""
    total_records = len(tokens)
    tokens_per_record = defaultdict(int)
    
    for record_id, token_set in tokens.items():
        tokens_per_record[len(token_set)] += 1
    
    print(f"{dataset_name} Token Distribution:")
    print(f"  Total records: {total_records}")
    for token_count, record_count in sorted(tokens_per_record.items()):
        print(f"  Records with {token_count} tokens: {record_count}")
    print()


def print_match_summary(matches, hospital_tokens, pharmacy_tokens):
    """Print a summary of matching results."""
    print("=" * 70)
    print("MATCH SUMMARY")
    print("=" * 70)
    print(f"Total matching record pairs: {len(matches)}")
    print()
    
    if matches:
        # Group by number of matching tokens
        matches_by_count = defaultdict(list)
        for match in matches:
            count = len(match[2])
            matches_by_count[count].append(match)
        
        for count in sorted(matches_by_count.keys(), reverse=True):
            match_list = matches_by_count[count]
            print(f"Matches with {count} tokens: {len(match_list)}")
        
        print()
        print("Sample matches (first 5):")
        print("-" * 70)
        for i, (hospital_id, pharmacy_id, matching_tokens) in enumerate(matches[:5]):
            print(f"Match #{i+1}:")
            print(f"  Hospital RecordId: {hospital_id}")
            print(f"  Pharmacy RecordId: {pharmacy_id}")
            print(f"  Matching Tokens: {', '.join(matching_tokens)}")
            print()
    else:
        print("No matches found!")
    
    print("=" * 70)


def save_matches_to_csv(matches, output_file):
    """Save matching results to CSV file."""
    with open(output_file, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['HospitalRecordId', 'PharmacyRecordId', 'MatchingTokens', 'TokenCount'])
        
        for hospital_id, pharmacy_id, matching_tokens in matches:
            writer.writerow([
                hospital_id,
                pharmacy_id,
                '|'.join(matching_tokens),
                len(matching_tokens)
            ])
    
    print(f"Match results saved to: {output_file}")


def print_metadata_summary(hospital_metadata, pharmacy_metadata):
    """Print summary of metadata from both datasets."""
    print()
    print("=" * 70)
    print("TOKENIZATION METADATA")
    print("=" * 70)
    
    if hospital_metadata:
        print("Hospital Dataset:")
        print(f"  Total records processed: {hospital_metadata.get('totalRecords', 'N/A')}")
        print(f"  Valid records: {hospital_metadata.get('validRecords', 'N/A')}")
        print(f"  Invalid records: {hospital_metadata.get('invalidRecords', 'N/A')}")
        print(f"  Library version: {hospital_metadata.get('libraryVersion', 'N/A')}")
        print()
    
    if pharmacy_metadata:
        print("Pharmacy Dataset:")
        print(f"  Total records processed: {pharmacy_metadata.get('totalRecords', 'N/A')}")
        print(f"  Valid records: {pharmacy_metadata.get('validRecords', 'N/A')}")
        print(f"  Invalid records: {pharmacy_metadata.get('invalidRecords', 'N/A')}")
        print(f"  Library version: {pharmacy_metadata.get('libraryVersion', 'N/A')}")
        print()
    
    print("=" * 70)
    print()


def main():
    """Main function to analyze overlap between datasets."""
    print()
    print("=" * 70)
    print("PPRL Overlap Analysis - Superhero Hospital & Pharmacy")
    print("=" * 70)
    print()
    
    # Configuration - must match the keys used during tokenization
    encryption_key = "SuperHero-Encryption-Key-32chars"
    
    # File paths
    hospital_tokens_file = '../outputs/hospital_tokens.csv'
    pharmacy_tokens_file = '../outputs/pharmacy_tokens.csv'
    hospital_metadata_file = '../outputs/hospital_tokens.csv.metadata.json'
    pharmacy_metadata_file = '../outputs/pharmacy_tokens.csv.metadata.json'
    matches_output_file = '../outputs/matching_records.csv'
    
    # Load and decrypt tokens
    print("Loading and decrypting tokens...")
    print("(Decryption is needed because OpenToken uses random IVs for encryption)")
    hospital_tokens = load_tokens(hospital_tokens_file, encryption_key)
    pharmacy_tokens = load_tokens(pharmacy_tokens_file, encryption_key)
    print(f"Loaded {len(hospital_tokens)} hospital records")
    print(f"Loaded {len(pharmacy_tokens)} pharmacy records")
    print()
    
    # Analyze token distribution
    analyze_token_distribution(hospital_tokens, "Hospital")
    analyze_token_distribution(pharmacy_tokens, "Pharmacy")
    
    # Find matches (all 5 tokens must match)
    matches = find_matches(hospital_tokens, pharmacy_tokens, required_token_matches=5)
    
    # Print match summary
    print_match_summary(matches, hospital_tokens, pharmacy_tokens)
    
    # Save matches to CSV
    if matches:
        save_matches_to_csv(matches, matches_output_file)
        print()
    
    # Load and print metadata
    hospital_metadata = load_metadata(hospital_metadata_file)
    pharmacy_metadata = load_metadata(pharmacy_metadata_file)
    print_metadata_summary(hospital_metadata, pharmacy_metadata)
    
    # Calculate overlap percentage
    if matches:
        hospital_match_rate = (len(matches) / len(hospital_tokens)) * 100
        pharmacy_match_rate = (len(matches) / len(pharmacy_tokens)) * 100
        
        print("OVERLAP STATISTICS")
        print("=" * 70)
        print(f"Hospital records with matches: {len(matches)} out of {len(hospital_tokens)} ({hospital_match_rate:.1f}%)")
        print(f"Pharmacy records with matches: {len(matches)} out of {len(pharmacy_tokens)} ({pharmacy_match_rate:.1f}%)")
        print("=" * 70)


if __name__ == '__main__':
    main()
