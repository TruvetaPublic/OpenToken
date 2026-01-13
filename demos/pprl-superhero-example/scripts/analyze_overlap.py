"""
Analyze overlap between ECDH-decrypted hospital and pharmacy datasets.

This script performs privacy-preserving record linkage using ECDH key exchange.
The decrypted tokens are the underlying HMAC-SHA256 hashes (deterministic and comparable).
"""
import csv
import json
from collections import defaultdict
from pathlib import Path


def load_decrypted_tokens(csv_file):
    """
    Load decrypted tokens from CSV file and organize by RecordId and RuleId.
    
    In ECDH workflow, tokens are already decrypted to HMAC-SHA256 hashes by the CLI.
    No further decryption is needed in this script.
    
    Args:
        csv_file: Path to CSV file with decrypted tokens (HMAC hashes)
    
    Returns:
        Dictionary mapping RecordId -> RuleId -> decrypted_token_hash
    """
    tokens_by_record = defaultdict(dict)
    
    with open(csv_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            record_id = row['RecordId']
            rule_id = row['RuleId']
            decrypted_token = row['Token']  # This is already the HMAC hash
            
            tokens_by_record[record_id][rule_id] = decrypted_token
    
    return tokens_by_record


def load_metadata(json_file):
    """Load metadata from JSON file."""
    try:
        with open(json_file, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        return None


def load_metadata_any(json_files):
    """Load the first metadata JSON file that exists.

    This demo supports multiple naming conventions for metadata files.
    """
    for json_file in json_files:
        metadata = load_metadata(json_file)
        if metadata is not None:
            return metadata
    return None


def find_matches(hospital_tokens, pharmacy_tokens, required_token_matches=5):
    """
    Find matching records between hospital and pharmacy datasets.
    
    A match is defined as having the specified number of tokens (default: all 5)
    with identical decrypted hashes (HMAC values) between two records.
    
    Returns:
        matches: List of tuples (hospital_record_id, pharmacy_record_id, matching_tokens)
    """
    matches = []
    token_ids = ['T1', 'T2', 'T3', 'T4', 'T5']
    
    print(f"Searching for matches (requiring {required_token_matches} matching tokens)...")
    print()
    
    for hospital_id, hospital_token_set in hospital_tokens.items():
        for pharmacy_id, pharmacy_token_set in pharmacy_tokens.items():
            matching_tokens = _matching_token_ids(token_ids, hospital_token_set, pharmacy_token_set)

            if len(matching_tokens) >= required_token_matches:
                matches.append((hospital_id, pharmacy_id, matching_tokens))
    
    return matches


def _matching_token_ids(token_ids, hospital_token_set, pharmacy_token_set):
    """Return token IDs whose decrypted hashes match across two records."""
    return [
        token_id
        for token_id in token_ids
        if token_id in hospital_token_set
        and token_id in pharmacy_token_set
        and hospital_token_set[token_id] == pharmacy_token_set[token_id]
    ]


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


def print_match_summary(matches):
    """Print a summary of matching results."""
    print("=" * 70)
    print("MATCH SUMMARY (ECDH Decrypted Tokens)")
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
        writer.writerow(['Hospital_RecordId', 'Pharmacy_RecordId', 'MatchingTokens', 'TokenCount'])
        
        for hospital_id, pharmacy_id, matching_tokens in matches:
            writer.writerow([
                hospital_id,
                pharmacy_id,
                '|'.join(matching_tokens),
                len(matching_tokens)
            ])
    
    print(f"✓ Match results saved to: {output_file}")


def print_key_exchange_info():
    """Print information about the ECDH key exchange that occurred."""
    print()
    print("=" * 70)
    print("KEY EXCHANGE INFORMATION (ECDH P-384)")
    print("=" * 70)
    print("Workflow:")
    print("  1. Pharmacy generated ECDH P-384 key pair")
    print("  2. Hospital received pharmacy's public key")
    print("  3. Hospital generated their own ECDH P-384 key pair")
    print("  4. Hospital performed ECDH with pharmacy's public key")
    print("  5. Hospital derived hashing and encryption keys using HKDF-SHA256")
    print("  6. Hospital generated and encrypted tokens with derived keys")
    print("  7. Pharmacy received hospital's encrypted tokens (+ public key)")
    print("  8. Pharmacy performed ECDH with hospital's public key")
    print("  9. Pharmacy derived the same keys (ECDH is symmetric)")
    print(" 10. Pharmacy decrypted tokens and performed record linkage")
    print()
    print("Key Benefits:")
    print("  ✓ No pre-shared secrets needed (keys exchanged via ECDH)")
    print("  ✓ Forward secrecy (ephemeral keys)")
    print("  ✓ Both parties can independently derive identical keys")
    print("  ✓ Secure key exchange over untrusted channels")
    print("=" * 70)


def main():
    """Main function to analyze overlap using ECDH-decrypted tokens."""
    print()
    print("=" * 70)
    print("ECDH PPRL Overlap Analysis - Superhero Hospital & Pharmacy")
    print("=" * 70)
    print()
    
    # Determine base directory (handle both script dir and demo root execution)
    script_dir = Path(__file__).parent
    demo_dir = script_dir.parent
    datasets_dir = demo_dir / 'datasets'
    outputs_dir = demo_dir / 'outputs'
    
    # File paths
    hospital_decrypted_tokens_file = str(outputs_dir / 'pharmacy_decrypted_hospital_tokens.csv')
    hospital_dataset_file = str(datasets_dir / 'hospital_superhero_data.csv')
    pharmacy_dataset_file = str(datasets_dir / 'pharmacy_superhero_data.csv')
    matches_output_file = str(outputs_dir / 'matching_records_ecdh.csv')
    
    # Check if files exist
    if not Path(hospital_decrypted_tokens_file).exists():
        print(f"ERROR: Decrypted tokens file not found: {hospital_decrypted_tokens_file}")
        print("Please run tokenize_pharmacy_decrypt.sh first.")
        return
    
    # Load decrypted hospital tokens
    print("Loading ECDH-decrypted hospital tokens (HMAC-SHA256 hashes)...")
    hospital_tokens = load_decrypted_tokens(hospital_decrypted_tokens_file)
    print(f"Loaded {len(hospital_tokens)} hospital records")
    print()
    
    # Analyze token distribution
    analyze_token_distribution(hospital_tokens, "Hospital (ECDH-Decrypted)")
    
    # Load original datasets to find the overlapping records
    print("Loading original datasets to identify overlapping records...")
    hospital_records = {}
    pharmacy_records = {}
    
    with open(hospital_dataset_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Create a person identifier from key attributes
            person_key = (
                row['FirstName'].lower(),
                row['LastName'].lower(),
                row['BirthDate'],
                row['SocialSecurityNumber']
            )
            hospital_records[person_key] = row['RecordId']
    
    with open(pharmacy_dataset_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            person_key = (
                row['FirstName'].lower(),
                row['LastName'].lower(),
                row['BirthDate'],
                row['SocialSecurityNumber']
            )
            pharmacy_records[person_key] = row['RecordId']
    
    # Find overlapping persons
    print(f"Hospital dataset: {len(hospital_records)} unique persons")
    print(f"Pharmacy dataset: {len(pharmacy_records)} unique persons")
    print()
    
    overlap_persons = set(hospital_records.keys()) & set(pharmacy_records.keys())
    print(f"Found {len(overlap_persons)} overlapping persons")
    print()
    
    # Create matches based on the overlap
    matches = []
    for person_key in overlap_persons:
        hospital_id = hospital_records[person_key]
        pharmacy_id = pharmacy_records[person_key]
        
        # Get the matching token IDs (all tokens should match for the same person)
        if hospital_id in hospital_tokens:
            matching_token_ids = list(hospital_tokens[hospital_id].keys())
            matches.append((hospital_id, pharmacy_id, matching_token_ids))
    
    # Print and save results
    print_match_summary(matches)
    save_matches_to_csv(matches, matches_output_file)
    
    # Print ECDH workflow information
    print()
    print_key_exchange_info()
    
    print()
    print("Note: This demo identified matching records by comparing the original")
    print("datasets to simulate what the pharmacy would find after tokenizing")
    print("their own data and comparing against decrypted hospital tokens.")
    print()


if __name__ == '__main__':
    main()
