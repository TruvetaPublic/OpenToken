"""
Copyright (c) Truveta. All rights reserved.

Dataset overlap analyzer for comparing tokenized datasets.
"""

import logging
from typing import List, Dict, Optional, Tuple
from pyspark.sql import DataFrame
from pyspark.sql.functions import col, lit, when, count, sum as spark_sum
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad
import base64


logger = logging.getLogger(__name__)


class OpenTokenOverlapAnalyzer:
    """
    Analyze overlap between two tokenized datasets based on matching rules.

    This class helps identify records that match between two datasets using
    encrypted tokens. It supports flexible matching rules based on specific
    token types (T1-T5 or custom tokens).
    """

    def __init__(self, encryption_key: str):
        """
        Initialize the overlap analyzer with encryption key.

        Args:
            encryption_key: The same AES-256 encryption key used to encrypt tokens.
                           Required to decrypt tokens for comparison.

        Raises:
            ValueError: If encryption key is empty or invalid length

        Example:
            >>> analyzer = OpenTokenOverlapAnalyzer("encryption-key-32-characters!!")
        """
        if not encryption_key or not encryption_key.strip():
            raise ValueError("Encryption key cannot be empty")
        if len(encryption_key.encode('utf-8')) != 32:
            raise ValueError(
                "Encryption key must be exactly 32 bytes (characters) for AES-256. "
                f"Got {len(encryption_key.encode('utf-8'))} bytes"
            )
        self.encryption_key = encryption_key.encode('utf-8')

    def _decrypt_token(self, encrypted_token: str) -> str:
        """
        Decrypt an encrypted token.

        Args:
            encrypted_token: Base64-encoded encrypted token

        Returns:
            Decrypted token string
        """
        try:
            encrypted_data = base64.b64decode(encrypted_token)
            iv = encrypted_data[:16]
            ciphertext = encrypted_data[16:]

            cipher = AES.new(self.encryption_key, AES.MODE_CBC, iv)
            decrypted = unpad(cipher.decrypt(ciphertext), AES.block_size)
            return decrypted.decode('utf-8')
        except Exception as e:
            logger.warning(f"Failed to decrypt token: {e}")
            return None

    def analyze_overlap(
        self,
        dataset1: DataFrame,
        dataset2: DataFrame,
        matching_rules: List[str],
        dataset1_name: str = "Dataset1",
        dataset2_name: str = "Dataset2"
    ) -> Dict[str, any]:
        """
        Analyze overlap between two tokenized datasets based on matching rules.

        Args:
            dataset1: First tokenized DataFrame (must have columns: RecordId, RuleId, Token)
            dataset2: Second tokenized DataFrame (must have columns: RecordId, RuleId, Token)
            matching_rules: List of token rule IDs that define a match (e.g., ["T1", "T2", "T3"])
                           A record is considered matching if it has matching tokens for ALL specified rules.
            dataset1_name: Optional name for first dataset (for reporting)
            dataset2_name: Optional name for second dataset (for reporting)

        Returns:
            Dictionary containing overlap analysis results:
            - 'total_records_dataset1': Total unique records in dataset 1
            - 'total_records_dataset2': Total unique records in dataset 2
            - 'matching_records': Number of records with matches in both datasets
            - 'unique_to_dataset1': Records only in dataset 1
            - 'unique_to_dataset2': Records only in dataset 2
            - 'overlap_percentage': Percentage of overlap
            - 'matches': DataFrame with matched record pairs

        Raises:
            ValueError: If datasets don't have required columns or matching_rules is empty

        Example:
            >>> analyzer = OpenTokenOverlapAnalyzer("encryption-key-32-characters!!")
            >>> # Match on T1 and T2 tokens (both must match)
            >>> results = analyzer.analyze_overlap(tokens_df1, tokens_df2, ["T1", "T2"])
            >>> print(f"Matching records: {results['matching_records']}")
            >>> print(f"Overlap: {results['overlap_percentage']:.2f}%")
        """
        # Validate inputs
        required_cols = {"RecordId", "RuleId", "Token"}
        for df, name in [(dataset1, dataset1_name), (dataset2, dataset2_name)]:
            if not required_cols.issubset(set(df.columns)):
                raise ValueError(
                    f"{name} must have columns: {required_cols}. "
                    f"Got: {set(df.columns)}"
                )

        if not matching_rules:
            raise ValueError("matching_rules cannot be empty")

        # Filter datasets to only include specified matching rules
        df1_filtered = dataset1.filter(col("RuleId").isin(matching_rules))
        df2_filtered = dataset2.filter(col("RuleId").isin(matching_rules))

        # Get unique records in each dataset
        total_records_df1 = dataset1.select("RecordId").distinct().count()
        total_records_df2 = dataset2.select("RecordId").distinct().count()

        # Join on Token and RuleId to find matches
        matches = df1_filtered.alias("df1").join(
            df2_filtered.alias("df2"),
            (col("df1.Token") == col("df2.Token")) & (col("df1.RuleId") == col("df2.RuleId")),
            "inner"
        ).select(
            col("df1.RecordId").alias(f"{dataset1_name}_RecordId"),
            col("df2.RecordId").alias(f"{dataset2_name}_RecordId"),
            col("df1.RuleId").alias("RuleId"),
            col("df1.Token").alias("Token")
        )

        # Count matches per record pair
        # A valid match requires matching tokens for ALL specified rules
        match_counts = matches.groupBy(
            f"{dataset1_name}_RecordId",
            f"{dataset2_name}_RecordId"
        ).agg(
            count("*").alias("matched_rules_count")
        ).filter(
            col("matched_rules_count") == len(matching_rules)
        )

        # Get unique matching records
        matching_records_df1 = match_counts.select(
            f"{dataset1_name}_RecordId"
        ).distinct().count()
        matching_records_df2 = match_counts.select(
            f"{dataset2_name}_RecordId"
        ).distinct().count()

        # Calculate unique records
        unique_to_df1 = total_records_df1 - matching_records_df1
        unique_to_df2 = total_records_df2 - matching_records_df2

        # Calculate overlap percentage (based on smaller dataset)
        smaller_dataset_size = min(total_records_df1, total_records_df2)
        overlap_percentage = (
            (min(matching_records_df1, matching_records_df2) / smaller_dataset_size * 100)
            if smaller_dataset_size > 0 else 0
        )

        # Get detailed matches
        detailed_matches = matches.groupBy(
            f"{dataset1_name}_RecordId",
            f"{dataset2_name}_RecordId"
        ).agg(
            count("*").alias("matched_rules_count")
        ).filter(
            col("matched_rules_count") == len(matching_rules)
        ).drop("matched_rules_count")

        return {
            'total_records_dataset1': total_records_df1,
            'total_records_dataset2': total_records_df2,
            'matching_records_dataset1': matching_records_df1,
            'matching_records_dataset2': matching_records_df2,
            'unique_to_dataset1': unique_to_df1,
            'unique_to_dataset2': unique_to_df2,
            'overlap_percentage': overlap_percentage,
            'matches': detailed_matches,
            'dataset1_name': dataset1_name,
            'dataset2_name': dataset2_name,
            'matching_rules': matching_rules
        }

    def compare_with_multiple_rules(
        self,
        dataset1: DataFrame,
        dataset2: DataFrame,
        rule_sets: List[List[str]],
        dataset1_name: str = "Dataset1",
        dataset2_name: str = "Dataset2"
    ) -> List[Dict[str, any]]:
        """
        Compare datasets using multiple different matching rule sets.

        This allows you to see how overlap changes with different matching criteria.
        For example, compare overlap when requiring T1+T2 vs T1+T2+T3.

        Args:
            dataset1: First tokenized DataFrame
            dataset2: Second tokenized DataFrame
            rule_sets: List of rule sets to try (e.g., [["T1"], ["T1", "T2"], ["T1", "T2", "T3"]])
            dataset1_name: Optional name for first dataset
            dataset2_name: Optional name for second dataset

        Returns:
            List of analysis results, one for each rule set

        Example:
            >>> analyzer = OpenTokenOverlapAnalyzer("encryption-key-32-characters!!")
            >>> rule_sets = [["T1"], ["T1", "T2"], ["T1", "T2", "T3"]]
            >>> results = analyzer.compare_with_multiple_rules(df1, df2, rule_sets)
            >>> for result in results:
            ...     print(f"Rules {result['matching_rules']}: {result['overlap_percentage']:.2f}%")
        """
        results = []
        for rules in rule_sets:
            result = self.analyze_overlap(
                dataset1, dataset2, rules, dataset1_name, dataset2_name
            )
            results.append(result)
        return results

    def print_summary(self, results: Dict[str, any]) -> None:
        """
        Print a formatted summary of overlap analysis results.

        Args:
            results: Results dictionary from analyze_overlap()

        Example:
            >>> results = analyzer.analyze_overlap(df1, df2, ["T1", "T2"])
            >>> analyzer.print_summary(results)
        """
        print("=" * 70)
        print(f"Dataset Overlap Analysis")
        print("=" * 70)
        print(f"Dataset 1: {results['dataset1_name']}")
        print(f"  Total records: {results['total_records_dataset1']:,}")
        print(f"  Matching records: {results['matching_records_dataset1']:,}")
        print(f"  Unique records: {results['unique_to_dataset1']:,}")
        print()
        print(f"Dataset 2: {results['dataset2_name']}")
        print(f"  Total records: {results['total_records_dataset2']:,}")
        print(f"  Matching records: {results['matching_records_dataset2']:,}")
        print(f"  Unique records: {results['unique_to_dataset2']:,}")
        print()
        print(f"Matching Rules: {', '.join(results['matching_rules'])}")
        print(f"Overlap Percentage: {results['overlap_percentage']:.2f}%")
        print("=" * 70)
