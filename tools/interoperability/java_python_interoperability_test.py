"""
Interoperability tests for OpenToken Java and Python implementations.

These tests validate that both implementations produce identical results
and can work with each other's outputs using ECDH key exchange.
"""

import subprocess
import tempfile
import json
import os
import sys
import csv
from pathlib import Path
from typing import Dict, Any

# Add Python library to path
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "lib/python/opentoken/src/main"))
sys.path.insert(0, str(Path(__file__).parent.parent.parent / "tools"))


class OpenTokenCLI:
    """Base class for OpenToken CLI operations."""
    
    def __init__(self):
        self.project_root = Path(__file__).parent.parent.parent
        java_jar_dir = self.project_root / "lib/java/opentoken-cli/target"
        jar_files = list(java_jar_dir.glob("opentoken-cli-*.jar"))
        if not jar_files:
            raise FileNotFoundError(f"No OpenToken CLI JAR found in {java_jar_dir}")
        self.java_jar_path = jar_files[0]
        self.python_main = self.project_root / "lib/python/opentoken-cli/src/main/opentoken_cli/main.py"
        self.sample_csv = self.project_root / "resources/sample.csv"
        
        # Default ECDH curve
        self.ecdh_curve = "P-384"


class JavaCLI(OpenTokenCLI):
    """Command Line wrapper for OpenToken-Java."""
    
    def generate_keypair(self, output_dir: Path) -> subprocess.CompletedProcess:
        """Generate ECDH keypair using Java CLI."""
        cmd = [
            "java", "-jar", str(self.java_jar_path),
            "generate-keypair",
            "--output-dir", str(output_dir),
            "--ecdh-curve", self.ecdh_curve
        ]
        
        print(f"Generating Java keypair in {output_dir}")
        result = subprocess.run(cmd, capture_output=True, text=True, cwd=self.project_root)
        
        if result.returncode != 0:
            print(f"Java keypair generation stderr: {result.stderr}")
            print(f"Java keypair generation stdout: {result.stdout}")
            raise RuntimeError(f"Java keypair generation failed: {result.stderr}")
        
        return result
    
    def generate_tokens(self, input_file: Path, output_file: Path, 
                       receiver_public_key: Path, sender_keypair_path: Path = None,
                       hash_only: bool = False) -> subprocess.CompletedProcess:
        """OpenToken-Java -- Generate tokens using ECDH."""
        cmd = [
            "java", "-jar", str(self.java_jar_path),
            "tokenize",
            "-i", str(input_file),
            "-t", "csv",
            "-o", str(output_file),
            "-ot", "csv",
            "--receiver-public-key", str(receiver_public_key),
            "--ecdh-curve", self.ecdh_curve
        ]
        
        if sender_keypair_path:
            cmd.extend(["--sender-keypair-path", str(sender_keypair_path)])
        
        if hash_only:
            cmd.append("--hash-only")
        
        print("Running Java tokenize\n")
        result = subprocess.run(cmd, capture_output=True, text=True, cwd=self.project_root)
        
        if result.returncode != 0:
            print(f"Java CLI stderr: {result.stderr}")
            print(f"Java CLI stdout: {result.stdout}")
            raise RuntimeError(f"Java CLI failed with return code {result.returncode}: {result.stderr}")
        
        return result
    
    def decrypt_tokens(self, input_file: Path, output_file: Path,
                      sender_public_key: Path, receiver_keypair_path: Path = None) -> subprocess.CompletedProcess:
        """OpenToken-Java -- Decrypt tokens using ECDH."""
        cmd = [
            "java", "-jar", str(self.java_jar_path),
            "decrypt",
            "-i", str(input_file),
            "-t", "csv",
            "-o", str(output_file),
            "-ot", "csv",
            "--sender-public-key", str(sender_public_key),
            "--ecdh-curve", self.ecdh_curve
        ]
        
        if receiver_keypair_path:
            cmd.extend(["--receiver-keypair-path", str(receiver_keypair_path)])
        
        print("Running Java decrypt\n")
        result = subprocess.run(cmd, capture_output=True, text=True, cwd=self.project_root)
        
        if result.returncode != 0:
            print(f"Java decrypt stderr: {result.stderr}")
            print(f"Java decrypt stdout: {result.stdout}")
            raise RuntimeError(f"Java decrypt failed: {result.stderr}")
        
        return result


class PythonCLI(OpenTokenCLI):
    """Command Line wrapper for OpenToken-Python."""

    def generate_keypair(self, output_dir: Path) -> subprocess.CompletedProcess:
        """Generate ECDH keypair using Python CLI."""
        cmd = [
            "python3", str(self.python_main),
            "generate-keypair",
            "--output-dir", str(output_dir),
            "--ecdh-curve", self.ecdh_curve
        ]
        
        env = {**os.environ, "PYTHONPATH": str(self.project_root / "lib/python/opentoken/src/main") + ":" + str(self.project_root / "lib/python/opentoken-cli/src/main")}
        
        print(f"Generating Python keypair in {output_dir}")
        result = subprocess.run(cmd, capture_output=True, text=True, 
                              cwd=self.project_root, env=env)
        
        if result.returncode != 0:
            print(f"Python keypair generation stderr: {result.stderr}")
            print(f"Python keypair generation stdout: {result.stdout}")
            raise RuntimeError(f"Python keypair generation failed: {result.stderr}")
        
        return result

    def generate_tokens(self, input_file: Path, output_file: Path,
                       receiver_public_key: Path, sender_keypair_path: Path = None,
                       hash_only: bool = False) -> subprocess.CompletedProcess:
        """OpenToken-Python -- Generate tokens using ECDH"""
        cmd = [
            "python3", str(self.python_main),
            "tokenize",
            "-i", str(input_file),
            "-t", "csv",
            "-o", str(output_file),
            "-ot", "csv",
            "--receiver-public-key", str(receiver_public_key),
            "--ecdh-curve", self.ecdh_curve
        ]
        
        if sender_keypair_path:
            cmd.extend(["--sender-keypair-path", str(sender_keypair_path)])
        
        if hash_only:
            cmd.append("--hash-only")
        
        env = {**os.environ, "PYTHONPATH": str(self.project_root / "lib/python/opentoken/src/main") + ":" + str(self.project_root / "lib/python/opentoken-cli/src/main")}
        
        print("Running Python tokenize\n")
        result = subprocess.run(cmd, capture_output=True, text=True, 
                              cwd=self.project_root, env=env)
        
        if result.returncode != 0:
            print(f"OpenToken-Python stderr: {result.stderr}")
            print(f"OpenToken-Python stdout: {result.stdout}")
            raise RuntimeError(f"OpenToken-Python failed with return code {result.returncode}: {result.stderr}")
        
        return result
    
    def decrypt_tokens(self, input_file: Path, output_file: Path,
                      sender_public_key: Path, receiver_keypair_path: Path = None) -> subprocess.CompletedProcess:
        """OpenToken-Python -- Decrypt tokens using ECDH."""
        cmd = [
            "python3", str(self.python_main),
            "decrypt",
            "-i", str(input_file),
            "-t", "csv",
            "-o", str(output_file),
            "-ot", "csv",
            "--sender-public-key", str(sender_public_key),
            "--ecdh-curve", self.ecdh_curve
        ]
        
        if receiver_keypair_path:
            cmd.extend(["--receiver-keypair-path", str(receiver_keypair_path)])
        
        env = {**os.environ, "PYTHONPATH": str(self.project_root / "lib/python/opentoken/src/main") + ":" + str(self.project_root / "lib/python/opentoken-cli/src/main")}
        
        print("Running Python decrypt\n")
        result = subprocess.run(cmd, capture_output=True, text=True,
                              cwd=self.project_root, env=env)
        
        if result.returncode != 0:
            print(f"Python decrypt stderr: {result.stderr}")
            print(f"Python decrypt stdout: {result.stdout}")
            raise RuntimeError(f"Python decrypt failed: {result.stderr}")
        
        return result


class TokenValidator:
    """Utility class for validating and comparing tokens."""
    
    @staticmethod
    def load_csv_tokens(file_path: Path) -> Dict[str, Dict[str, str]]:
        """Load tokens from CSV file, indexed by RecordId and RuleId."""
        tokens = {}
        
        with open(file_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                record_id = row.get('RecordId', '')
                rule_id = row.get('RuleId', '')
                token = row.get('Token', '')
                
                if record_id not in tokens:
                    tokens[record_id] = {}
                tokens[record_id][rule_id] = token
        
        return tokens
    
    @staticmethod
    def compare_token_files(file1: Path, file2: Path) -> Dict[str, Any]:
        """Compare two token CSV files and return detailed comparison results."""
        tokens1 = TokenValidator.load_csv_tokens(file1)
        tokens2 = TokenValidator.load_csv_tokens(file2)
        
        all_record_ids = set(tokens1.keys()) | set(tokens2.keys())
        
        comparison_results = {
            'total_records': len(all_record_ids),
            'matching_records': 0,
            'mismatched_records': [],
            'missing_in_file1': [],
            'missing_in_file2': [],
            'detailed_mismatches': {}
        }
        
        for record_id in all_record_ids:
            if record_id not in tokens1:
                comparison_results['missing_in_file1'].append(record_id)
                continue
            if record_id not in tokens2:
                comparison_results['missing_in_file2'].append(record_id)
                continue
            
            # Compare all rule IDs for this record
            all_rule_ids = set(tokens1[record_id].keys()) | set(tokens2[record_id].keys())
            record_matches = True
            record_mismatches = {}
            
            for rule_id in all_rule_ids:
                token1 = tokens1[record_id].get(rule_id, '')
                token2 = tokens2[record_id].get(rule_id, '')
                
                if token1 != token2:
                    record_matches = False
                    record_mismatches[rule_id] = {
                        'file1_token': token1,
                        'file2_token': token2
                    }
            
            if record_matches:
                comparison_results['matching_records'] += 1
            else:
                comparison_results['mismatched_records'].append(record_id)
                comparison_results['detailed_mismatches'][record_id] = record_mismatches
        
        return comparison_results


class TestTokenCompatibility:
    """Test token compatibility between OpenToken-Java and OpenToken-Python using ECDH."""
    
    def setup_method(self):
        """Set up environment for each method."""
        self.java_cli = JavaCLI()
        self.python_cli = PythonCLI()
        self.validator = TokenValidator()
    
    def test_full_interoperability_pipeline(self):
        """
        Complete interoperability test with ECDH:
        1. Generate keypairs for sender (hospital) and receiver (pharmacy)
        2. Run Java implementation on sample.csv (as sender)
        3. Run Python implementation on sample.csv (as sender)
        4. Decrypt Java output using Java CLI (as receiver)
        5. Decrypt Python output using Python CLI (as receiver)
        6. Compare decrypted tokens to ensure they're identical
        """
        print("Running Java/Python Interoperability Test (ECDH)")
        print("-" * 50)
        
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            
            # Create key directories
            hospital_key_dir = temp_path / "hospital_keys"
            pharmacy_key_dir = temp_path / "pharmacy_keys"
            hospital_key_dir.mkdir()
            pharmacy_key_dir.mkdir()
            
            print("1. Generating keypairs...")
            
            # Generate keypairs for hospital (sender) and pharmacy (receiver)
            self.java_cli.generate_keypair(hospital_key_dir)
            self.python_cli.generate_keypair(pharmacy_key_dir)
            
            # Key paths
            hospital_private_key = hospital_key_dir / "keypair.pem"
            hospital_public_key = hospital_key_dir / "public_key.pem"
            pharmacy_private_key = pharmacy_key_dir / "keypair.pem"
            pharmacy_public_key = pharmacy_key_dir / "public_key.pem"
            
            # Verify keys were created
            for key_path in [hospital_private_key, hospital_public_key, pharmacy_private_key, pharmacy_public_key]:
                assert key_path.exists(), f"Key file {key_path} was not created"
            
            # Output files
            java_output = temp_path / "java_output.csv"
            python_output = temp_path / "python_output.csv"
            java_decrypted = temp_path / "java_decrypted.csv"
            python_decrypted = temp_path / "python_decrypted.csv"
            
            print("2. Running Java implementation (as hospital/sender)...")
            
            # Generate tokens with Java (hospital sends to pharmacy)
            self.java_cli.generate_tokens(
                input_file=self.java_cli.sample_csv,
                output_file=java_output,
                receiver_public_key=pharmacy_public_key,
                sender_keypair_path=hospital_private_key
            )
            
            # Verify Java output exists
            assert java_output.exists(), f"Java output file {java_output} was not created"
            
            print("3. Running Python implementation (as hospital/sender)...")
            
            # Generate tokens with Python (hospital sends to pharmacy)
            self.python_cli.generate_tokens(
                input_file=self.python_cli.sample_csv,
                output_file=python_output,
                receiver_public_key=pharmacy_public_key,
                sender_keypair_path=hospital_private_key
            )
            
            # Verify Python output exists
            assert python_output.exists(), f"Python output file {python_output} was not created"
            
            print("4. Decrypting Java output (as pharmacy/receiver)...")
            
            # Decrypt Java output with Java CLI
            self.java_cli.decrypt_tokens(
                input_file=java_output,
                output_file=java_decrypted,
                sender_public_key=hospital_public_key,
                receiver_keypair_path=pharmacy_private_key
            )
            assert java_decrypted.exists(), f"Java decrypted file {java_decrypted} was not created"
            
            print("5. Decrypting Python output (as pharmacy/receiver)...")
            
            # Decrypt Python output with Python CLI
            self.python_cli.decrypt_tokens(
                input_file=python_output,
                output_file=python_decrypted,
                sender_public_key=hospital_public_key,
                receiver_keypair_path=pharmacy_private_key
            )
            assert python_decrypted.exists(), f"Python decrypted file {python_decrypted} was not created"
            
            print("6. Comparing results...")
            
            # Compare decrypted outputs
            comparison = self.validator.compare_token_files(java_decrypted, python_decrypted)
            
            print(f"\nResults: {comparison['matching_records']}/{comparison['total_records']} records match")
            
            if comparison['mismatched_records']:
                print(f"⚠️  {len(comparison['mismatched_records'])} mismatched records found")
                for record_id in comparison['mismatched_records'][:3]:  # Show first 3
                    print(f"  • Record {record_id} differs")
                    if record_id in comparison['detailed_mismatches']:
                        for rule_id, details in comparison['detailed_mismatches'][record_id].items():
                            print(f"    Rule {rule_id}:")
                            print(f"      Java:   {details['file1_token'][:50]}...")
                            print(f"      Python: {details['file2_token'][:50]}...")
            
            if comparison['missing_in_file1'] or comparison['missing_in_file2']:
                print(f"⚠️  Missing records - Java: {len(comparison['missing_in_file1'])}, Python: {len(comparison['missing_in_file2'])}")
            
            # Assert that all tokens match
            assert comparison['matching_records'] == comparison['total_records'], \
                f"Token mismatch found! {len(comparison['mismatched_records'])} records don't match"
            
            assert len(comparison['missing_in_file1']) == 0, \
                f"Records missing in Java output: {comparison['missing_in_file1']}"
            
            assert len(comparison['missing_in_file2']) == 0, \
                f"Records missing in Python output: {comparison['missing_in_file2']}"
            
            print(f"✅ SUCCESS: All {comparison['total_records']} records have identical tokens!")
            print("-" * 50)
    
    def test_metadata_consistency(self):
        """Test that metadata files are consistent between implementations."""
        print("\nTesting Metadata Consistency (ECDH)")
        print("-" * 30)
        
        with tempfile.TemporaryDirectory() as temp_dir:
            temp_path = Path(temp_dir)
            
            # Create key directories
            sender_key_dir = temp_path / "sender_keys"
            receiver_key_dir = temp_path / "receiver_keys"
            sender_key_dir.mkdir()
            receiver_key_dir.mkdir()
            
            # Generate keypairs
            self.java_cli.generate_keypair(sender_key_dir)
            self.python_cli.generate_keypair(receiver_key_dir)
            
            sender_private_key = sender_key_dir / "keypair.pem"
            receiver_public_key = receiver_key_dir / "public_key.pem"
            
            java_output = temp_path / "java_metadata_test.csv"
            python_output = temp_path / "python_metadata_test.csv"
            
            # Generate tokens with both implementations
            self.java_cli.generate_tokens(
                self.java_cli.sample_csv, 
                java_output,
                receiver_public_key,
                sender_private_key
            )
            self.python_cli.generate_tokens(
                self.python_cli.sample_csv, 
                python_output,
                receiver_public_key,
                sender_private_key
            )
            
            # Check for metadata files
            java_metadata = java_output.with_suffix('.metadata.json')
            python_metadata = python_output.with_suffix('.metadata.json')
            
            assert java_metadata.exists(), f"Java metadata file {java_metadata} not found"
            assert python_metadata.exists(), f"Python metadata file {python_metadata} not found"
            
            # Load and compare metadata
            with open(java_metadata, 'r') as f:
                java_meta = json.load(f)
            
            with open(python_metadata, 'r') as f:  
                python_meta = json.load(f)
            
            # Compare essential fields (platform-specific fields will differ)
            essential_fields = ['OutputFormat', 'KeyExchangeMethod']
            
            for field in essential_fields:
                if field in java_meta and field in python_meta:
                    assert java_meta[field] == python_meta[field], \
                        f"Metadata field '{field}' differs: Java={java_meta[field]}, Python={python_meta[field]}"
            
            # Verify ECDH-specific fields exist
            for meta, impl_name in [(java_meta, "Java"), (python_meta, "Python")]:
                assert 'KeyExchangeMethod' in meta, f"{impl_name} metadata missing KeyExchangeMethod"
                assert meta['KeyExchangeMethod'] == 'ECDH', f"{impl_name} should use ECDH key exchange"
                assert 'Curve' in meta, f"{impl_name} metadata missing Curve"
                assert 'SenderPublicKeyHash' in meta, f"{impl_name} metadata missing SenderPublicKeyHash"
                assert 'ReceiverPublicKeyHash' in meta, f"{impl_name} metadata missing ReceiverPublicKeyHash"
            
            print("✅ Metadata consistency verified!")
            print("-" * 30)

if __name__ == "__main__":
    # Run the test manually
    test = TestTokenCompatibility()
    test.setup_method()
    
    try:
        test.test_full_interoperability_pipeline()
        test.test_metadata_consistency()
                
        print("\nALL TESTS PASSED!")
        
    except Exception as e:
        print(f"\nTEST FAILED: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
