#!/usr/bin/env python3
"""
Example demonstrating ECDH key exchange with OpenToken PySpark bridge.

This shows how to use public key cryptography for secure token generation
without sharing secrets between sender and receiver.
"""

import tempfile
import os
import shutil
from pyspark.sql import SparkSession
from opentoken_pyspark import OpenTokenProcessor, KeyPairManager


def main():
    # Create Spark session
    spark = SparkSession.builder \
        .appName("OpenToken ECDH Example") \
        .master("local[*]") \
        .getOrCreate()

    print("=== OpenToken ECDH Key Exchange Example ===\n")

    # Step 1: Generate keypairs (in production, done once offline)
    print("Step 1: Generating keypairs...")
    receiver_key_dir = tempfile.mkdtemp(prefix="receiver_keys_")
    sender_key_dir = tempfile.mkdtemp(prefix="sender_keys_")

    try:
        # Receiver generates keypair
        receiver_mgr = KeyPairManager(key_directory=receiver_key_dir, curve_name="P-384")
        receiver_mgr.generate_and_save_key_pair()
        print(f"✓ Receiver keypair saved to: {receiver_key_dir}")

        # Sender generates keypair
        sender_mgr = KeyPairManager(key_directory=sender_key_dir, curve_name="P-384")
        sender_mgr.generate_and_save_key_pair()
        print(f"✓ Sender keypair saved to: {sender_key_dir}\n")

        # Step 2: Create sample data
        print("Step 2: Creating sample data...")
        data = [
            {
                "RecordId": "001",
                "FirstName": "John",
                "LastName": "Doe",
                "BirthDate": "1990-01-15",
                "Sex": "Male",
                "PostalCode": "98052",
                "SocialSecurityNumber": "123-45-6789"
            },
            {
                "RecordId": "002",
                "FirstName": "Jane",
                "LastName": "Smith",
                "BirthDate": "1985-06-20",
                "Sex": "Female",
                "PostalCode": "10001",
                "SocialSecurityNumber": "987-65-4321"
            }
        ]
        df = spark.createDataFrame(data)
        print(f"✓ Created DataFrame with {df.count()} records\n")

        # Step 3: Initialize processor with ECDH
        print("Step 3: Performing ECDH key exchange...")
        processor = OpenTokenProcessor.from_ecdh(
            receiver_public_key_path=os.path.join(receiver_key_dir, "public_key.pem"),
            sender_keypair_path=os.path.join(sender_key_dir, "keypair.pem"),
            ecdh_curve="P-384"
        )
        print("✓ Keys derived successfully from ECDH\n")

        # Step 4: Generate tokens
        print("Step 4: Generating tokens...")
        tokens_df = processor.process_dataframe(df)
        total_tokens = tokens_df.count()
        print(f"✓ Generated {total_tokens} tokens\n")

        # Step 5: Display results
        print("Step 5: Sample tokens:")
        tokens_df.show(10, truncate=False)

        # Show token distribution by rule
        print("\nToken distribution by rule:")
        tokens_df.groupBy("RuleId").count().orderBy("RuleId").show()

        print("\n=== Success! ECDH key exchange working correctly ===")

    finally:
        # Cleanup
        shutil.rmtree(receiver_key_dir, ignore_errors=True)
        shutil.rmtree(sender_key_dir, ignore_errors=True)
        spark.stop()
        print("\n✓ Cleanup completed")


if __name__ == "__main__":
    main()
