"""
Copyright (c) Truveta. All rights reserved.
"""

import logging
import uuid
from collections import defaultdict
from typing import Dict, List, Type, Any

from opentoken.attributes.attribute import Attribute
from opentoken.attributes.general.record_id_attribute import RecordIdAttribute
from opentoken.io.person_attributes_reader import PersonAttributesReader
from opentoken.io.person_attributes_writer import PersonAttributesWriter
from opentoken.tokens.token_definition import TokenDefinition
from opentoken.tokens.token_generator import TokenGenerator
from opentoken.tokens.token_generator_result import TokenGeneratorResult
from opentoken.tokentransformer.token_transformer import TokenTransformer


logger = logging.getLogger(__name__)


class PersonAttributesProcessor:
    """
    Process all person attributes.

    This class is used to read person attributes from input source,
    generate tokens for each person record and write the tokens back
    to the output data source.
    """

    TOKEN = "Token"
    RULE_ID = "RuleId"
    RECORD_ID = "RecordId"

    TOTAL_ROWS = "TotalRows"
    TOTAL_ROWS_WITH_INVALID_ATTRIBUTES = "TotalRowsWithInvalidAttributes"
    INVALID_ATTRIBUTES_BY_TYPE = "InvalidAttributesByType"
    BLANK_TOKENS_BY_RULE = "BlankTokensByRule"

    def __init__(self):
        """Private constructor to prevent instantiation."""
        pass

    @staticmethod
    def process(reader: PersonAttributesReader,
                writer: PersonAttributesWriter,
                token_transformer_list: List[TokenTransformer],
                metadata_map: Dict[str, Any] = None) -> None:
        """
        Read person attributes from the input data source, generate tokens, and
        write the result back to the output data source. The tokens can be optionally
        transformed before writing.

        Args:
            reader: The reader initialized with the input data source.
            writer: The writer initialized with the output data source.
            token_transformer_list: A list of token transformers.
            metadata_map: Optional metadata map to update with processing statistics.
        """
        # TokenGenerator code
        token_generator = TokenGenerator(TokenDefinition(), token_transformer_list)

        row_counter = 0
        invalid_attribute_count: Dict[str, int] = defaultdict(int)
        blank_tokens_by_rule_count: Dict[str, int] = defaultdict(int)

        try:
            for row in reader:
                row_counter += 1

                token_generator_result = token_generator.get_all_tokens(row)
                logger.debug(f"Tokens: {token_generator_result.tokens}")

                PersonAttributesProcessor._keep_track_of_invalid_attributes(
                    token_generator_result, row_counter, invalid_attribute_count
                )

                PersonAttributesProcessor._keep_track_of_blank_tokens(
                    token_generator_result, row_counter, blank_tokens_by_rule_count
                )

                PersonAttributesProcessor._write_tokens(
                    writer, row, row_counter, token_generator_result
                )

                if row_counter % 10000 == 0:
                    logger.info(f"Processed {row_counter:,} records")

        except Exception as e:
            logger.error(f"Error processing records: {e}", exc_info=True)
            raise

        logger.info(f"Processed a total of {row_counter:,} records")

        # Log invalid attribute statistics
        for attribute_name, count in invalid_attribute_count.items():
            logger.info(f"Total invalid Attribute count for [{attribute_name}]: {count:,}")

        total_invalid_records = sum(invalid_attribute_count.values())
        logger.info(f"Total number of records with invalid attributes: {total_invalid_records:,}")

        # Log blank token statistics
        for rule_id, count in blank_tokens_by_rule_count.items():
            logger.info(f"Total blank tokens for rule [{rule_id}]: {count:,}")

        total_blank_tokens = sum(blank_tokens_by_rule_count.values())
        logger.info(f"Total blank tokens generated: {total_blank_tokens:,}")

        # Update metadata if provided
        if metadata_map is not None:
            metadata_map[PersonAttributesProcessor.TOTAL_ROWS] = row_counter
            metadata_map[PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES] = total_invalid_records
            metadata_map[PersonAttributesProcessor.INVALID_ATTRIBUTES_BY_TYPE] = dict(invalid_attribute_count)
            metadata_map[PersonAttributesProcessor.BLANK_TOKENS_BY_RULE] = dict(blank_tokens_by_rule_count)

    @staticmethod
    def _write_tokens(writer: PersonAttributesWriter,
                      row: Dict[Type[Attribute], str],
                      row_counter: int,
                      token_generator_result: TokenGeneratorResult) -> None:
        """
        Write tokens to the output writer.

        Args:
            writer: The writer to write tokens to.
            row: The original row data.
            row_counter: The current row number.
            token_generator_result: The result from token generation.
        """
        # Sort token IDs for consistent output
        token_ids = sorted(token_generator_result.tokens.keys())

        # Generate a UUID for RecordId if it's not present in the input data
        record_id = row.get(RecordIdAttribute)
        if record_id is None or record_id == '':
            record_id = str(uuid.uuid4())

        for token_id in token_ids:
            row_result = {
                PersonAttributesProcessor.RULE_ID: token_id,
                PersonAttributesProcessor.TOKEN: token_generator_result.tokens[token_id],
                PersonAttributesProcessor.RECORD_ID: record_id
            }

            try:
                writer.write_attributes(row_result)
            except IOError:
                logger.error(f"Error writing attributes to file for row {row_counter:,}", exc_info=True)

    @staticmethod
    def _keep_track_of_invalid_attributes(token_generator_result: TokenGeneratorResult,
                                          row_counter: int,
                                          invalid_attribute_count: Dict[str, int]) -> None:
        """
        Keep track of invalid attributes for logging purposes.

        Args:
            token_generator_result: The result from token generation.
            row_counter: The current row number.
            invalid_attribute_count: Dictionary to track invalid attribute counts.
        """
        if token_generator_result.invalid_attributes:
            logger.info(
                f"Invalid Attributes for row {row_counter:,}: {token_generator_result.invalid_attributes}"
            )

            for invalid_attribute in token_generator_result.invalid_attributes:
                invalid_attribute_count[invalid_attribute] += 1

    @staticmethod
    def _keep_track_of_blank_tokens(token_generator_result: TokenGeneratorResult,
                                    row_counter: int,
                                    blank_tokens_by_rule_count: Dict[str, int]) -> None:
        """
        Keep track of blank tokens for logging purposes.

        Args:
            token_generator_result: The result from token generation.
            row_counter: The current row number.
            blank_tokens_by_rule_count: Dictionary to track blank token counts by rule.
        """
        if token_generator_result.blank_tokens_by_rule:
            logger.debug(
                f"Blank tokens for row {row_counter:,}: {token_generator_result.blank_tokens_by_rule}"
            )

            for rule_id in token_generator_result.blank_tokens_by_rule:
                blank_tokens_by_rule_count[rule_id] += 1
