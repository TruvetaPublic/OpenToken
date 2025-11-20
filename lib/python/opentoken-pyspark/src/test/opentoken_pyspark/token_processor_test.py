"""
Copyright (c) Truveta. All rights reserved.

Tests for OpenToken PySpark token processor.
"""

import pytest
from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, StringType
from opentoken_pyspark import OpenTokenProcessor


@pytest.fixture(scope="module")
def spark():
    """Create a Spark session for testing."""
    spark = SparkSession.builder \
        .appName("OpenTokenTest") \
        .master("local[2]") \
        .config("spark.sql.shuffle.partitions", "2") \
        .getOrCreate()
    yield spark
    spark.stop()


@pytest.fixture
def sample_data():
    """Sample person data for testing."""
    return [
        {
            "RecordId": "891dda6c-961f-4154-8541-b48fe18ee620",
            "FirstName": "John",
            "LastName": "Doe",
            "PostalCode": "98004",
            "Sex": "Male",
            "BirthDate": "2000-01-01",
            "SocialSecurityNumber": "123-45-6789"
        },
        {
            "RecordId": "2f97f0f1-4617-40bd-8264-4d24a9adf20a",
            "FirstName": "Joe",
            "LastName": "Price",
            "PostalCode": "15635",
            "Sex": "Male",
            "BirthDate": "1951-10-22",
            "SocialSecurityNumber": "172-10-0983"
        }
    ]


class TestOpenTokenProcessor:
    """Tests for OpenTokenProcessor class."""

    def test_initialization_with_valid_secrets(self):
        """Test that processor initializes with valid secrets."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")
        assert processor.hashing_secret == "HashingKey"
        assert processor.encryption_key == "Secret-Encryption-Key-Goes-Here."

    def test_initialization_with_empty_hashing_secret(self):
        """Test that initialization fails with empty hashing secret."""
        with pytest.raises(ValueError, match="Hashing secret cannot be empty"):
            OpenTokenProcessor("", "Secret-Encryption-Key-Goes-Here.")

    def test_initialization_with_empty_encryption_key(self):
        """Test that initialization fails with empty encryption key."""
        with pytest.raises(ValueError, match="Encryption key cannot be empty"):
            OpenTokenProcessor("HashingKey", "")

    def test_initialization_with_whitespace_secrets(self):
        """Test that initialization fails with whitespace-only secrets."""
        with pytest.raises(ValueError, match="Hashing secret cannot be empty"):
            OpenTokenProcessor("   ", "Secret-Encryption-Key-Goes-Here.")

    def test_process_dataframe_with_valid_data(self, spark, sample_data):
        """Test processing a DataFrame with valid data."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")

        # Create DataFrame
        df = spark.createDataFrame(sample_data)

        # Process the DataFrame
        result_df = processor.process_dataframe(df)

        # Verify result structure
        assert "RecordId" in result_df.columns
        assert "RuleId" in result_df.columns
        assert "Token" in result_df.columns

        # Collect results
        results = result_df.collect()

        # Should have tokens for each record (5 tokens per record)
        assert len(results) > 0

        # Verify we have multiple rule IDs
        rule_ids = set(row.RuleId for row in results)
        assert len(rule_ids) > 1

    def test_process_dataframe_with_alternative_column_names(self, spark):
        """Test processing with alternative column names."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")

        # Create DataFrame with alternative column names
        data = [{
            "Id": "test-123",
            "GivenName": "John",
            "Surname": "Doe",
            "ZipCode": "98004",
            "Gender": "Male",
            "DateOfBirth": "2000-01-01",
            "NationalIdentificationNumber": "123-45-6789"
        }]

        df = spark.createDataFrame(data)

        # Process should work with alternative names
        result_df = processor.process_dataframe(df)

        # Verify result
        assert result_df.count() > 0

    def test_process_dataframe_with_missing_required_column(self, spark):
        """Test that processing fails with missing required columns."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")

        # Create DataFrame missing SocialSecurityNumber
        data = [{
            "RecordId": "test-123",
            "FirstName": "John",
            "LastName": "Doe",
            "PostalCode": "98004",
            "Sex": "Male",
            "BirthDate": "2000-01-01"
        }]

        df = spark.createDataFrame(data)

        # Should raise ValueError
        with pytest.raises(ValueError, match="Missing required columns"):
            processor.process_dataframe(df)

    def test_process_dataframe_with_none_dataframe(self):
        """Test that processing fails with None DataFrame."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")

        with pytest.raises(ValueError, match="DataFrame cannot be None"):
            processor.process_dataframe(None)

    def test_tokens_are_consistent(self, spark, sample_data):
        """Test that the same input produces the same tokens."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")

        # Create DataFrame
        df = spark.createDataFrame(sample_data)

        # Process twice
        result1 = processor.process_dataframe(df).collect()
        result2 = processor.process_dataframe(df).collect()

        # Convert to sets for comparison (order might differ)
        tokens1 = {(r.RecordId, r.RuleId, r.Token) for r in result1}
        tokens2 = {(r.RecordId, r.RuleId, r.Token) for r in result2}

        # Should be identical
        assert tokens1 == tokens2

    def test_different_secrets_produce_different_tokens(self, spark, sample_data):
        """Test that different secrets produce different tokens."""
        processor1 = OpenTokenProcessor("HashingKey1", "EncryptionKey1")
        processor2 = OpenTokenProcessor("HashingKey2", "EncryptionKey2")

        # Create DataFrame
        df = spark.createDataFrame(sample_data)

        # Process with different secrets
        result1 = processor1.process_dataframe(df).collect()
        result2 = processor2.process_dataframe(df).collect()

        # Get tokens for the same record and rule
        tokens1 = {(r.RecordId, r.RuleId, r.Token) for r in result1}
        tokens2 = {(r.RecordId, r.RuleId, r.Token) for r in result2}

        # Tokens should be different
        assert tokens1 != tokens2

    def test_column_mapping(self, spark):
        """Test that column mapping works correctly."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")

        # Create DataFrame with various column names
        data = [{
            "RecordId": "test-1",
            "FirstName": "John",
            "LastName": "Doe",
            "PostalCode": "98004",
            "Sex": "Male",
            "BirthDate": "2000-01-01",
            "SocialSecurityNumber": "123-45-6789"
        }]

        df = spark.createDataFrame(data)

        # Get column mapping
        mapping = processor._get_column_mapping(df)

        # Verify mapping
        assert mapping["RecordId"] == "RecordId"
        assert mapping["FirstName"] == "FirstName"
        assert mapping["LastName"] == "LastName"
        assert mapping["PostalCode"] == "PostalCode"
        assert mapping["Sex"] == "Sex"
        assert mapping["BirthDate"] == "BirthDate"
        assert mapping["SocialSecurityNumber"] == "SocialSecurityNumber"

    def test_validation_with_empty_dataframe(self, spark):
        """Test validation with an empty DataFrame."""
        processor = OpenTokenProcessor("HashingKey", "Secret-Encryption-Key-Goes-Here.")

        # Create empty DataFrame with correct schema
        schema = StructType([
            StructField("RecordId", StringType(), True),
            StructField("FirstName", StringType(), True),
            StructField("LastName", StringType(), True),
            StructField("PostalCode", StringType(), True),
            StructField("Sex", StringType(), True),
            StructField("BirthDate", StringType(), True),
            StructField("SocialSecurityNumber", StringType(), True)
        ])

        df = spark.createDataFrame([], schema)

        # Should not raise an error during validation
        processor._validate_dataframe(df)

        # Process should return empty DataFrame
        result = processor.process_dataframe(df)
        assert result.count() == 0

    def test_custom_token_definition(self, spark, sample_data):
        """Test using custom token definition with processor."""
        from opentoken.notebook_helpers import TokenBuilder, CustomTokenDefinition

        # Create a custom T6 token
        t6_token = TokenBuilder("T6") \
            .add("last_name", "T|U") \
            .add("first_name", "T|U") \
            .add("birth_date", "T|D") \
            .build()

        custom_definition = CustomTokenDefinition().add_token(t6_token)

        # Create processor with custom definition
        processor = OpenTokenProcessor(
            hashing_secret="test-hash-secret",
            encryption_key="12345678901234567890123456789012",
            token_definition=custom_definition
        )

        # Create DataFrame
        df = spark.createDataFrame(sample_data)

        # Process with custom tokens
        result = processor.process_dataframe(df)

        # Verify we got T6 tokens (not default T1-T5)
        rule_ids = [row.RuleId for row in result.select("RuleId").distinct().collect()]
        assert "T6" in rule_ids
        assert "T1" not in rule_ids  # Default tokens should not be present

        # Verify we have tokens for each record
        assert result.count() == len(sample_data)  # One T6 token per record

    def test_multiple_custom_tokens(self, spark, sample_data):
        """Test using multiple custom tokens."""
        from opentoken.notebook_helpers import TokenBuilder, CustomTokenDefinition

        # Create two custom tokens
        t6_token = TokenBuilder("T6") \
            .add("last_name", "T|U") \
            .add("first_name", "T|U") \
            .build()

        t7_token = TokenBuilder("T7") \
            .add("last_name", "T|S(0,3)|U") \
            .add("birth_date", "T|D") \
            .build()

        custom_definition = CustomTokenDefinition() \
            .add_token(t6_token) \
            .add_token(t7_token)

        # Create processor with multiple custom tokens
        processor = OpenTokenProcessor(
            hashing_secret="test-hash-secret",
            encryption_key="12345678901234567890123456789012",
            token_definition=custom_definition
        )

        # Create DataFrame
        df = spark.createDataFrame(sample_data)

        # Process with custom tokens
        result = processor.process_dataframe(df)

        # Verify we got both T6 and T7 tokens
        rule_ids = [row.RuleId for row in result.select("RuleId").distinct().collect()]
        assert "T6" in rule_ids
        assert "T7" in rule_ids
        assert "T1" not in rule_ids  # Default tokens should not be present

        # Verify we have 2 tokens per record (T6 and T7)
        assert result.count() == len(sample_data) * 2
