"""
Copyright (c) Truveta. All rights reserved.
"""

import logging
import sys
from typing import List

from opentoken.command_line_arguments import CommandLineArguments
from opentoken.io.csv.person_attributes_csv_reader import PersonAttributesCSVReader
from opentoken.io.csv.person_attributes_csv_writer import PersonAttributesCSVWriter
from opentoken.io.json.metadata_json_writer import MetadataJsonWriter
from opentoken.io.parquet.person_attributes_parquet_reader import PersonAttributesParquetReader
from opentoken.io.parquet.person_attributes_parquet_writer import PersonAttributesParquetWriter
from opentoken.metadata import Metadata
from opentoken.processor.person_attributes_processor import PersonAttributesProcessor
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer
from opentoken.tokentransformer.hash_token_transformer import HashTokenTransformer
from opentoken.tokentransformer.token_transformer import TokenTransformer


# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main():
    """Main entry point for the OpenToken application."""
    try:
        # Parse command line arguments
        args = CommandLineArguments.parse_args()

        hashing_secret = args.hashing_secret
        encryption_key = args.encryption_key
        input_path = args.input_path
        input_type = args.input_type
        output_path = args.output_path
        output_type = args.output_type if args.output_type else input_type

        logger.info(f"Hashing Secret: {_mask_string(hashing_secret)}")
        logger.info(f"Encryption Key: {_mask_string(encryption_key)}")
        logger.info(f"Input Path: {input_path}")
        logger.info(f"Input Type: {input_type}")
        logger.info(f"Output Path: {output_path}")
        logger.info(f"Output Type: {output_type}")

        # Validate input parameters
        if input_type not in [CommandLineArguments.TYPE_CSV, CommandLineArguments.TYPE_PARQUET]:
            logger.error("Only csv and parquet input types are supported!")
            return
        elif (not hashing_secret or not hashing_secret.strip() or
              not encryption_key or not encryption_key.strip()):
            logger.error("Hashing secret and encryption key must be specified")
            return

        # Create token transformers
        token_transformer_list = []
        try:
            token_transformer_list.append(HashTokenTransformer(hashing_secret))
            token_transformer_list.append(EncryptTokenTransformer(encryption_key))
        except Exception as e:
            logger.error("Error in initializing the transformer. Execution halted.", exc_info=e)
            return

        try:
            # Create reader and writer based on file types
            reader = _create_person_attributes_reader(input_path, input_type)
            writer = _create_person_attributes_writer(output_path, output_type)

            try:
                # Create initial metadata with system information
                metadata = Metadata()
                metadata_map = metadata.initialize()

                # Set secrets separately
                metadata.add_hashed_secret(Metadata.HASHING_SECRET_HASH, hashing_secret)
                metadata.add_hashed_secret(Metadata.ENCRYPTION_SECRET_HASH, encryption_key)

                # Process data and get updated metadata
                PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)

                # Write the metadata to file
                metadata_writer = MetadataJsonWriter(output_path)
                metadata_writer.write(metadata_map)

            finally:
                # Close resources
                if reader:
                    reader.close()
                if writer:
                    writer.close()

        except Exception as e:
            logger.error("Error in processing the input file. Execution halted.", exc_info=e)

        logger.info("OpenToken processing completed successfully.")

    except Exception as e:
        logger.error(f"Error during OpenToken processing: {e}", exc_info=True)
        sys.exit(1)


def _create_person_attributes_reader(input_path: str, input_type: str):
    """Create a PersonAttributesReader based on input type."""
    input_type_lower = input_type.lower()
    if input_type_lower == CommandLineArguments.TYPE_CSV:
        return PersonAttributesCSVReader(input_path)
    elif input_type_lower == CommandLineArguments.TYPE_PARQUET:
        return PersonAttributesParquetReader(input_path)
    else:
        raise ValueError(f"Unsupported input type: {input_type}")


def _create_person_attributes_writer(output_path: str, output_type: str):
    """Create a PersonAttributesWriter based on output type."""
    output_type_lower = output_type.lower()
    if output_type_lower == CommandLineArguments.TYPE_CSV:
        return PersonAttributesCSVWriter(output_path)
    elif output_type_lower == CommandLineArguments.TYPE_PARQUET:
        return PersonAttributesParquetWriter(output_path)
    else:
        raise ValueError(f"Unsupported output type: {output_type}")


def _load_command_line_arguments(args: list) -> CommandLineArguments:
    """Load and parse command line arguments."""
    logger.debug(f"Processing command line arguments: {' | '.join(args)}")
    command_line_arguments = CommandLineArguments.parse_args(args)
    logger.info("Command line arguments processed.")
    return command_line_arguments


def _mask_string(input_str: str) -> str:
    """Mask a string for logging purposes, showing only first 3 characters."""
    if input_str is None or len(input_str) <= 3:
        return input_str
    return input_str[:3] + "*" * (len(input_str) - 3)


def build_token_transformers(args: CommandLineArguments) -> List[TokenTransformer]:
    """
    Build the list of token transformers based on command line arguments.

    Args:
        args: Command line arguments.

    Returns:
        List of token transformers.
    """
    transformers = []

    # Add hash transformer if hashing secret is provided
    if args.hashing_secret:
        logger.info("Adding hash token transformer")
        transformers.append(HashTokenTransformer(args.hashing_secret))

    # Add encrypt transformer if encryption key is provided
    if args.encryption_key:
        logger.info("Adding encrypt token transformer")
        transformers.append(EncryptTokenTransformer(args.encryption_key))

    return transformers


def create_reader(input_path: str, input_type: str):
    """
    Create the appropriate reader based on input type.

    Args:
        input_path: Path to the input file.
        input_type: Type of the input file.

    Returns:
        PersonAttributesReader instance.

    Raises:
        ValueError: If the input type is not supported.
    """
    if input_type == CommandLineArguments.TYPE_CSV:
        return PersonAttributesCSVReader(input_path)
    elif input_type == CommandLineArguments.TYPE_PARQUET:
        return PersonAttributesParquetReader(input_path)
    else:
        raise ValueError(f"Unsupported input type: {input_type}")


def create_writer(output_path: str, output_type: str):
    """
    Create the appropriate writer based on output type.

    Args:
        output_path: Path to the output file.
        output_type: Type of the output file.

    Returns:
        PersonAttributesWriter instance.

    Raises:
        ValueError: If the output type is not supported.
    """
    if output_type == CommandLineArguments.TYPE_CSV:
        return PersonAttributesCSVWriter(output_path)
    elif output_type == CommandLineArguments.TYPE_PARQUET:
        return PersonAttributesParquetWriter(output_path)
    else:
        raise ValueError(f"Unsupported output type: {output_type}")


if __name__ == "__main__":
    main()
