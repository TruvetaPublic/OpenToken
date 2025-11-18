"""
Copyright (c) Truveta. All rights reserved.

PySpark token processor for distributed token generation.
"""

import logging
from typing import Dict, Type, Optional
from pyspark.sql import DataFrame
from pyspark.sql.functions import pandas_udf, col
from pyspark.sql.types import StructType, StructField, StringType, ArrayType
import pandas as pd

# Import OpenToken core functionality
from opentoken.attributes.attribute import Attribute
from opentoken.attributes.attribute_loader import AttributeLoader
from opentoken.attributes.person.first_name_attribute import FirstNameAttribute
from opentoken.attributes.person.last_name_attribute import LastNameAttribute
from opentoken.attributes.person.birth_date_attribute import BirthDateAttribute
from opentoken.attributes.person.sex_attribute import SexAttribute
from opentoken.attributes.person.postal_code_attribute import PostalCodeAttribute
from opentoken.attributes.person.social_security_number_attribute import SocialSecurityNumberAttribute
from opentoken.attributes.general.record_id_attribute import RecordIdAttribute
from opentoken.tokens.token_definition import TokenDefinition
from opentoken.tokens.base_token_definition import BaseTokenDefinition
from opentoken.tokens.token_generator import TokenGenerator
from opentoken.tokentransformer.hash_token_transformer import HashTokenTransformer
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer


logger = logging.getLogger(__name__)


class OpenTokenProcessor:
    """
    Process PySpark DataFrames to generate OpenTokens.

    This class provides a bridge between PySpark DataFrames and OpenToken
    token generation functionality, enabling distributed token generation
    across a Spark cluster.
    """

    # Standard column mappings (column name -> attribute class)
    # Built dynamically from attribute classes
    COLUMN_MAPPINGS: Dict[str, Type[Attribute]] = None

    @classmethod
    def _build_column_mappings(cls) -> Dict[str, Type[Attribute]]:
        """
        Build column name to attribute class mappings dynamically from loaded attributes.

        Returns:
            Dictionary mapping column names to their corresponding attribute classes.
        """
        if cls.COLUMN_MAPPINGS is not None:
            return cls.COLUMN_MAPPINGS

        mappings = {}
        for attribute in AttributeLoader.load():
            attribute_class = type(attribute)
            for alias in attribute.get_aliases():
                mappings[alias] = attribute_class

        # Add DateOfBirth as an alias for BirthDate for backward compatibility
        # (documented in README but not in BirthDateAttribute.ALIASES)
        if "BirthDate" in mappings:
            mappings["DateOfBirth"] = mappings["BirthDate"]

        cls.COLUMN_MAPPINGS = mappings
        return mappings

    def __init__(self, hashing_secret: str, encryption_key: str,
                 token_definition: Optional[BaseTokenDefinition] = None):
        """
        Initialize the OpenToken processor with secrets.

        Args:
            hashing_secret: Secret for HMAC-SHA256 hashing
            encryption_key: Key for AES-256 encryption
            token_definition: Optional custom token definition. If None, uses default tokens (T1-T5).
                             Use this to pass custom tokens created with TokenBuilder or CustomTokenDefinition.

        Raises:
            ValueError: If secrets are empty or invalid

        Example:
            >>> # Using default tokens
            >>> processor = OpenTokenProcessor("hash-secret", "encryption-key-32-characters!!")
            >>>
            >>> # Using custom token definition
            >>> from opentoken.notebook_helpers import TokenBuilder, CustomTokenDefinition
            >>> custom_token = TokenBuilder("T6").add("last_name", "T|U").add("first_name", "T|U").build()
            >>> custom_def = CustomTokenDefinition().add_token(custom_token)
            >>> processor = OpenTokenProcessor("hash-secret", "encryption-key-32-chars!!", custom_def)
        """
        if not hashing_secret or not hashing_secret.strip():
            raise ValueError("Hashing secret cannot be empty")
        if not encryption_key or not encryption_key.strip():
            raise ValueError("Encryption key cannot be empty")

        self.hashing_secret = hashing_secret
        self.encryption_key = encryption_key
        self.token_definition = token_definition  # Store custom token definition

        # Build column mappings if not already built
        self._build_column_mappings()

        # Validate secrets can initialize transformers
        try:
            HashTokenTransformer(hashing_secret)
            EncryptTokenTransformer(encryption_key)
        except Exception as e:
            logger.error("Error initializing token transformers", exc_info=e)
            raise ValueError(f"Invalid secrets provided: {e}")

    def process_dataframe(self, df: DataFrame) -> DataFrame:
        """
        Process a PySpark DataFrame and generate tokens for each record.

        The input DataFrame must contain the following columns (case-sensitive alternatives listed):
        - RecordId or Id (optional - auto-generated if not provided)
        - FirstName or GivenName
        - LastName or Surname
        - BirthDate or DateOfBirth
        - Sex or Gender
        - PostalCode or ZipCode
        - SocialSecurityNumber or NationalIdentificationNumber

        Args:
            df: Input PySpark DataFrame with person attributes

        Returns:
            DataFrame with columns: RecordId, RuleId, Token

        Raises:
            ValueError: If required columns are missing or invalid
        """
        # Validate input DataFrame
        self._validate_dataframe(df)

        # Get the secrets and token definition for the UDF
        hashing_secret = self.hashing_secret
        encryption_key = self.encryption_key
        token_definition = self.token_definition

        # Define the schema for the output (array of structs)
        token_schema = ArrayType(StructType([
            StructField("RuleId", StringType(), False),
            StructField("Token", StringType(), False)
        ]))

        # Create a pandas UDF for token generation
        @pandas_udf(token_schema)
        def generate_tokens_udf(
            record_id_series: pd.Series,
            first_name_series: pd.Series,
            last_name_series: pd.Series,
            birth_date_series: pd.Series,
            sex_series: pd.Series,
            postal_code_series: pd.Series,
            ssn_series: pd.Series
        ) -> pd.Series:
            """
            Pandas UDF to generate tokens for a batch of records.

            This function is executed on each partition of the DataFrame
            in parallel across the Spark cluster.
            """
            # Initialize token transformers
            token_transformer_list = [
                HashTokenTransformer(hashing_secret),
                EncryptTokenTransformer(encryption_key)
            ]

            # Use custom token definition if provided, otherwise use default
            definition = token_definition if token_definition is not None else TokenDefinition()

            # Initialize token generator
            token_generator = TokenGenerator(
                definition, token_transformer_list
            )

            results = []

            # Process each row in the batch
            for idx in range(len(record_id_series)):
                # Build person attributes dictionary
                person_attrs = {
                    RecordIdAttribute: str(record_id_series.iloc[idx])
                    if pd.notna(record_id_series.iloc[idx]) else None,
                    FirstNameAttribute: str(first_name_series.iloc[idx])
                    if pd.notna(first_name_series.iloc[idx]) else None,
                    LastNameAttribute: str(last_name_series.iloc[idx])
                    if pd.notna(last_name_series.iloc[idx]) else None,
                    BirthDateAttribute: str(birth_date_series.iloc[idx])
                    if pd.notna(birth_date_series.iloc[idx]) else None,
                    SexAttribute: str(sex_series.iloc[idx])
                    if pd.notna(sex_series.iloc[idx]) else None,
                    PostalCodeAttribute: str(postal_code_series.iloc[idx])
                    if pd.notna(postal_code_series.iloc[idx]) else None,
                    SocialSecurityNumberAttribute: str(ssn_series.iloc[idx])
                    if pd.notna(ssn_series.iloc[idx]) else None,
                }

                # Generate tokens for this record
                try:
                    token_result = token_generator.get_all_tokens(person_attrs)

                    # Convert to list of dicts for this record
                    tokens_list = [
                        {"RuleId": rule_id, "Token": token}
                        for rule_id, token in token_result.tokens.items()
                    ]

                    results.append(tokens_list)
                except Exception as e:
                    logger.error(f"Error generating tokens for record: {e}")
                    # Return empty list for failed records
                    results.append([])

            return pd.Series(results)

        # Normalize column names - find the actual column names in the DataFrame
        column_mapping = self._get_column_mapping(df)

        # Apply the UDF to generate tokens
        tokens_df = df.select(
            col(column_mapping["RecordId"]).alias("RecordId"),
            col(column_mapping["FirstName"]).alias("FirstName"),
            col(column_mapping["LastName"]).alias("LastName"),
            col(column_mapping["BirthDate"]).alias("BirthDate"),
            col(column_mapping["Sex"]).alias("Sex"),
            col(column_mapping["PostalCode"]).alias("PostalCode"),
            col(column_mapping["SocialSecurityNumber"]).alias("SocialSecurityNumber")
        ).withColumn(
            "tokens",
            generate_tokens_udf(
                col("RecordId"),
                col("FirstName"),
                col("LastName"),
                col("BirthDate"),
                col("Sex"),
                col("PostalCode"),
                col("SocialSecurityNumber")
            )
        )

        # Explode the tokens array to get one row per token
        from pyspark.sql.functions import explode
        result_df = tokens_df.select(
            col("RecordId"),
            explode(col("tokens")).alias("token_struct")
        ).select(
            col("RecordId"),
            col("token_struct.RuleId").alias("RuleId"),
            col("token_struct.Token").alias("Token")
        )

        return result_df

    @classmethod
    def _get_required_attribute_groups(cls) -> Dict[str, list]:
        """
        Get required attribute groups with their column name variants.

        Returns:
            Dictionary mapping attribute names to their column name variants.
        """
        # Required attributes for token generation (excluding RecordId which is optional)
        required_attribute_classes = [
            FirstNameAttribute,
            LastNameAttribute,
            BirthDateAttribute,
            SexAttribute,
            PostalCodeAttribute,
            SocialSecurityNumberAttribute,
        ]

        groups = {}
        for attr_class in required_attribute_classes:
            attr_instance = attr_class()
            attr_name = attr_instance.get_name()
            aliases = attr_instance.get_aliases()

            # Add DateOfBirth for BirthDate if not already present
            if attr_name == "BirthDate" and "DateOfBirth" not in aliases:
                aliases = aliases + ["DateOfBirth"]

            groups[attr_name] = aliases

        return groups

    def _validate_dataframe(self, df: DataFrame) -> None:
        """
        Validate that the DataFrame has all required columns.

        Args:
            df: DataFrame to validate

        Raises:
            ValueError: If required columns are missing
        """
        if df is None:
            raise ValueError("DataFrame cannot be None")

        df_columns = set(df.columns)

        # Get required attribute groups dynamically
        required_groups = self._get_required_attribute_groups()

        missing = []
        for group_name, variants in required_groups.items():
            if not any(col in df_columns for col in variants):
                missing.append(f"{group_name} (or variants: {', '.join(variants)})")

        if missing:
            raise ValueError(f"Missing required columns: {', '.join(missing)}")

    def _get_column_mapping(self, df: DataFrame) -> Dict[str, str]:
        """
        Map standard attribute names to actual column names in the DataFrame.

        Args:
            df: DataFrame to map columns from

        Returns:
            Dictionary mapping standard names to actual column names
        """
        df_columns = set(df.columns)
        mapping = {}

        # Get all attribute groups (including optional RecordId)
        column_groups = self._get_required_attribute_groups()

        # Add RecordId separately since it's optional
        record_id_attr = RecordIdAttribute()
        record_id_aliases = record_id_attr.get_aliases()
        column_groups[record_id_attr.get_name()] = record_id_aliases

        # Find the first matching column for each group
        for standard_name, variants in column_groups.items():
            for variant in variants:
                if variant in df_columns:
                    mapping[standard_name] = variant
                    break

            # RecordId is optional, provide a default
            if standard_name == "RecordId" and standard_name not in mapping:
                # We'll need to generate RecordIds - use a placeholder for now
                mapping[standard_name] = variants[0]

        return mapping
