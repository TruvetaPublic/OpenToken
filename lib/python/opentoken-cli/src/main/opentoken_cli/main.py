"""
Copyright (c) Truveta. All rights reserved.
"""

import logging
import sys
from typing import List

from opentoken_cli.command_line_arguments import CommandLineArguments
from opentoken_cli.io.csv.person_attributes_csv_reader import PersonAttributesCSVReader
from opentoken_cli.io.csv.person_attributes_csv_writer import PersonAttributesCSVWriter
from opentoken_cli.io.csv.token_csv_reader import TokenCSVReader
from opentoken_cli.io.csv.token_csv_writer import TokenCSVWriter
from opentoken_cli.io.json.metadata_json_writer import MetadataJsonWriter
from opentoken_cli.io.parquet.person_attributes_parquet_reader import PersonAttributesParquetReader
from opentoken_cli.io.parquet.person_attributes_parquet_writer import PersonAttributesParquetWriter
from opentoken_cli.io.parquet.token_parquet_reader import TokenParquetReader
from opentoken_cli.io.parquet.token_parquet_writer import TokenParquetWriter
from opentoken_cli.processor.person_attributes_processor import PersonAttributesProcessor
from opentoken_cli.processor.token_decryption_processor import TokenDecryptionProcessor
from opentoken_cli.io.output_packager import OutputPackager
from opentoken.metadata import Metadata
from opentoken.tokentransformer.decrypt_token_transformer import DecryptTokenTransformer
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer
from opentoken.tokentransformer.hash_token_transformer import HashTokenTransformer
from opentoken.tokentransformer.token_transformer import TokenTransformer
from opentoken.keyexchange import KeyPairManager, KeyExchange, PublicKeyLoader, KeyExchangeException

import os
from cryptography.hazmat.primitives import serialization


# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main():
    """Main entry point for the OpenToken application."""
    command_line_arguments = _load_command_line_arguments(sys.argv[1:])
    
    # Handle keypair generation mode
    if command_line_arguments.generate_keypair:
        _generate_keypair()
        return
    
    hashing_secret = command_line_arguments.hashing_secret
    encryption_key = command_line_arguments.encryption_key
    input_path = command_line_arguments.input_path
    input_type = command_line_arguments.input_type
    output_path = command_line_arguments.output_path
    output_type = command_line_arguments.output_type if command_line_arguments.output_type else input_type
    decrypt_mode = command_line_arguments.decrypt
    hash_only_mode = command_line_arguments.hash_only
    decrypt_with_ecdh = command_line_arguments.decrypt_with_ecdh
    receiver_public_key_path = command_line_arguments.receiver_public_key
    sender_public_key_path = command_line_arguments.sender_public_key
    sender_keypair_path = command_line_arguments.sender_keypair_path
    receiver_keypair_path = command_line_arguments.receiver_keypair_path

    logger.info(f"Decrypt Mode: {decrypt_mode}")
    logger.info(f"Decrypt with ECDH: {decrypt_with_ecdh}")
    logger.info(f"Hash-Only Mode: {hash_only_mode}")
    logger.info(f"Receiver Public Key: {receiver_public_key_path}")
    logger.info(f"Hashing Secret: {_mask_string(hashing_secret)}")
    logger.info(f"Encryption Key: {_mask_string(encryption_key)}")
    logger.info(f"Input Path: {input_path}")
    logger.info(f"Input Type: {input_type}")
    logger.info(f"Output Path: {output_path}")
    logger.info(f"Output Type: {output_type}")

    # Validate input and output types for both modes
    if input_type not in [CommandLineArguments.TYPE_CSV, CommandLineArguments.TYPE_PARQUET]:
        logger.error("Only csv and parquet input types are supported!")
        return
    if output_type not in [CommandLineArguments.TYPE_CSV, CommandLineArguments.TYPE_PARQUET]:
        logger.error("Only csv and parquet output types are supported!")
        return

    # Process based on mode
    if decrypt_mode or decrypt_with_ecdh:
        # Decrypt mode
        if decrypt_with_ecdh:
            # ECDH-based decryption
            _decrypt_tokens_with_ecdh(input_path, output_path, input_type, output_type,
                                     sender_public_key_path, receiver_keypair_path)
        else:
            # Secret-based decryption (legacy)
            if not encryption_key or not encryption_key.strip():
                logger.error("Encryption key must be specified for decryption")
                return
            _decrypt_tokens(input_path, output_path, input_type, output_type, encryption_key)
        logger.info("Token decryption completed successfully.")
    else:
        # Token generation mode
        if receiver_public_key_path and receiver_public_key_path.strip():
            # ECDH-based encryption
            _process_tokens_with_ecdh(input_path, output_path, input_type, output_type,
                                     receiver_public_key_path, sender_keypair_path)
        else:
            # Secret-based encryption (legacy)
            if not hashing_secret or not hashing_secret.strip():
                logger.error("Hashing secret must be specified")
                return
            if not hash_only_mode and (not encryption_key or not encryption_key.strip()):
                logger.error("Encryption key must be specified (or use --hash-only to skip encryption)")
                return
            _process_tokens(input_path, output_path, input_type, output_type, hashing_secret, encryption_key, hash_only_mode)


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


def _create_token_reader(input_path: str, input_type: str):
    """Create a TokenReader based on input type."""
    input_type_lower = input_type.lower()
    if input_type_lower == CommandLineArguments.TYPE_CSV:
        return TokenCSVReader(input_path)
    elif input_type_lower == CommandLineArguments.TYPE_PARQUET:
        return TokenParquetReader(input_path)
    else:
        raise ValueError(f"Unsupported input type: {input_type}")


def _create_token_writer(output_path: str, output_type: str):
    """Create a TokenWriter based on output type."""
    output_type_lower = output_type.lower()
    if output_type_lower == CommandLineArguments.TYPE_CSV:
        return TokenCSVWriter(output_path)
    elif output_type_lower == CommandLineArguments.TYPE_PARQUET:
        return TokenParquetWriter(output_path)
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


def _process_tokens(input_path: str, output_path: str, input_type: str, output_type: str,
                    hashing_secret: str, encryption_key: str, hash_only_mode: bool):
    """
    Process tokens from person attributes and write to output file.
    
    Args:
        input_path: Path to input file with person attributes.
        output_path: Path to output file for tokens.
        input_type: Type of input file (csv or parquet).
        output_type: Type of output file (csv or parquet).
        hashing_secret: Secret for hashing tokens.
        encryption_key: Key for encrypting tokens (not used in hash-only mode).
        hash_only_mode: If True, skip encryption step.
    """
    token_transformer_list: List[TokenTransformer] = []
    try:
        # Always add hash transformer
        token_transformer_list.append(HashTokenTransformer(hashing_secret))
        
        # Only add encryption transformer if not in hash-only mode
        if not hash_only_mode:
            token_transformer_list.append(EncryptTokenTransformer(encryption_key))
    except Exception as e:
        logger.error("Error in initializing the transformer. Execution halted.", exc_info=e)
        return

    try:
        with _create_person_attributes_reader(input_path, input_type) as reader, \
             _create_person_attributes_writer(output_path, output_type) as writer:

            # Create initial metadata with system information
            metadata = Metadata()
            metadata_map = metadata.initialize()

            # Set hashing secret
            metadata.add_hashed_secret(Metadata.HASHING_SECRET_HASH, hashing_secret)
            
            # Set encryption secret if applicable
            if not hash_only_mode:
                metadata.add_hashed_secret(Metadata.ENCRYPTION_SECRET_HASH, encryption_key)

            # Process data and get updated metadata
            PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)

            # Write the metadata to file
            metadata_writer = MetadataJsonWriter(output_path)
            metadata_writer.write(metadata_map)

    except Exception as e:
        logger.error("Error in processing the input file. Execution halted.", exc_info=e)


def _decrypt_tokens(input_path: str, output_path: str, input_type: str, output_type: str, encryption_key: str):
    """
    Decrypt tokens from input file and write to output file.
    
    Args:
        input_path: Path to input file with encrypted tokens.
        output_path: Path to output file for decrypted tokens.
        input_type: Type of input file (csv or parquet).
        output_type: Type of output file (csv or parquet).
        encryption_key: Encryption key for decryption.
    """
    try:
        decryptor = DecryptTokenTransformer(encryption_key)
        
        with _create_token_reader(input_path, input_type) as reader, \
             _create_token_writer(output_path, output_type) as writer:
            TokenDecryptionProcessor.process(reader, writer, decryptor)
                
    except Exception as e:
        logger.error(f"Error during token decryption: {e}")
        raise


def _generate_keypair():
    """Generate a new ECDH key pair and save it to the default location."""
    try:
        logger.info("Generating new ECDH P-256 key pair...")
        key_pair_manager = KeyPairManager()
        private_key, public_key = key_pair_manager.generate_and_save_key_pair()
        
        logger.info("✓ Key pair generated successfully")
        logger.info(f"✓ Private key saved to: {key_pair_manager.get_key_directory()}/keypair.pem (0600 permissions)")
        logger.info(f"✓ Public key saved to: {key_pair_manager.get_key_directory()}/public_key.pem")
        
    except KeyExchangeException as e:
        logger.error(f"Error generating key pair: {e}")


def _process_tokens_with_ecdh(input_path: str, output_path: str, input_type: str, output_type: str,
                              receiver_public_key_path: str, sender_keypair_path: str = None):
    """
    Process tokens using ECDH-based key exchange.
    
    Args:
        input_path: Path to person attributes file.
        output_path: Path to output ZIP file.
        input_type: Type of input file (csv or parquet).
        output_type: Type of output file (csv or parquet).
        receiver_public_key_path: Path to receiver's public key.
        sender_keypair_path: Optional path to sender's keypair.
    """
    try:
        logger.info("Processing tokens with ECDH key exchange...")
        
        # Load receiver's public key
        public_key_loader = PublicKeyLoader()
        receiver_public_key = public_key_loader.load_public_key(receiver_public_key_path)
        logger.info("✓ Loaded receiver's public key")
        
        # Load or generate sender's key pair
        if sender_keypair_path:
            sender_key_manager = KeyPairManager(os.path.dirname(sender_keypair_path))
        else:
            sender_key_manager = KeyPairManager()
        sender_private_key, sender_public_key = sender_key_manager.get_or_create_key_pair()
        logger.info(f"✓ Sender key pair ready (saved to: {sender_key_manager.get_key_directory()})")
        
        # Perform ECDH key exchange
        key_exchange = KeyExchange()
        keys = key_exchange.exchange_and_derive_keys(sender_private_key, receiver_public_key)
        logger.info("✓ Performed ECDH key exchange")
        logger.info("✓ Derived hashing key (32 bytes)")
        logger.info("✓ Derived encryption key (32 bytes)")
        
        # Create transformers with derived keys
        token_transformer_list = [
            HashTokenTransformer(keys.get_hashing_key_as_string()),
            EncryptTokenTransformer(keys.get_encryption_key_as_string())
        ]
        
        # Create temporary output for tokens
        temp_output_path = output_path.replace('.zip', f'_temp.{output_type}') if output_path.endswith('.zip') else f"{output_path}_temp"
        
        with _create_person_attributes_reader(input_path, input_type) as reader, \
             _create_person_attributes_writer(temp_output_path, output_type) as writer:
            
            # Create metadata with key exchange info
            metadata = Metadata()
            metadata_map = metadata.initialize()
            
            # Add key exchange metadata
            sender_public_key_bytes = sender_public_key.public_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PublicFormat.SubjectPublicKeyInfo
            )
            receiver_public_key_bytes = receiver_public_key.public_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PublicFormat.SubjectPublicKeyInfo
            )
            metadata.add_key_exchange_metadata(sender_public_key_bytes, receiver_public_key_bytes)
            
            # Process data
            PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)
            
            # Write metadata
            metadata_path = temp_output_path + Metadata.METADATA_FILE_EXTENSION
            metadata_writer = MetadataJsonWriter(temp_output_path)
            metadata_writer.write(metadata_map)
            
            # Package output as ZIP if needed
            if OutputPackager.is_zip_file(output_path):
                OutputPackager.package_output(temp_output_path, metadata_path, 
                                            sender_public_key, output_path)
                logger.info(f"✓ Output package created: {output_path}")
                logger.info(f"  ├─ tokens.{output_type} (encrypted)")
                logger.info("  ├─ tokens.metadata.json")
                logger.info("  └─ sender_public_key.pem")
                
                # Clean up temp files
                if os.path.exists(temp_output_path):
                    os.remove(temp_output_path)
                if os.path.exists(metadata_path):
                    os.remove(metadata_path)
            else:
                logger.info("✓ Tokens generated successfully")
                logger.info("Note: Use .zip extension for automatic packaging with sender's public key")
        
    except Exception as e:
        logger.error(f"Error processing tokens with ECDH: {e}", exc_info=True)


def _decrypt_tokens_with_ecdh(input_path: str, output_path: str, input_type: str, output_type: str,
                              sender_public_key_path: str = None, receiver_keypair_path: str = None):
    """
    Decrypt tokens using ECDH-based key exchange.
    
    Args:
        input_path: Path to input file (or ZIP).
        output_path: Path to decrypted output file.
        input_type: Type of input file (csv or parquet).
        output_type: Type of output file (csv or parquet).
        sender_public_key_path: Optional path to sender's public key (extracted from ZIP if not provided).
        receiver_keypair_path: Optional path to receiver's keypair.
    """
    try:
        logger.info("Decrypting tokens with ECDH key exchange...")
        
        # Extract sender's public key from ZIP if needed
        if OutputPackager.is_zip_file(input_path):
            sender_public_key = OutputPackager.extract_sender_public_key(input_path)
            logger.info("✓ Extracted sender's public key from ZIP")
            
            # Extract tokens to temp file
            tokens_input_path = input_path.replace('.zip', f'_temp.{input_type}')
            OutputPackager.extract_tokens_file(input_path, tokens_input_path)
        elif sender_public_key_path and sender_public_key_path.strip():
            public_key_loader = PublicKeyLoader()
            sender_public_key = public_key_loader.load_public_key(sender_public_key_path)
            logger.info(f"✓ Loaded sender's public key from: {sender_public_key_path}")
            tokens_input_path = input_path
        else:
            logger.error("Sender's public key must be provided or available in ZIP")
            return
        
        # Load receiver's private key
        if receiver_keypair_path:
            receiver_key_manager = KeyPairManager(os.path.dirname(receiver_keypair_path))
            receiver_private_key, _ = receiver_key_manager.load_key_pair(receiver_keypair_path)
        else:
            receiver_key_manager = KeyPairManager()
            keypair_path = os.path.join(receiver_key_manager.get_key_directory(), "keypair.pem")
            receiver_private_key, _ = receiver_key_manager.load_key_pair(keypair_path)
        logger.info(f"✓ Loaded receiver's private key from: {receiver_key_manager.get_key_directory()}")
        
        # Perform ECDH key exchange (same as sender)
        key_exchange = KeyExchange()
        keys = key_exchange.exchange_and_derive_keys(receiver_private_key, sender_public_key)
        logger.info("✓ Performed ECDH key exchange")
        logger.info("✓ Derived encryption key (matches sender's key)")
        
        # Decrypt tokens
        decryptor = DecryptTokenTransformer(keys.get_encryption_key_as_string())
        
        with _create_token_reader(tokens_input_path, input_type) as reader, \
             _create_token_writer(output_path, output_type) as writer:
            TokenDecryptionProcessor.process(reader, writer, decryptor)
        
        # Clean up temp file if we extracted from ZIP
        if OutputPackager.is_zip_file(input_path) and os.path.exists(tokens_input_path):
            os.remove(tokens_input_path)
        
        logger.info("✓ Tokens decrypted successfully")
        logger.info(f"✓ Output written to: {output_path}")
        
    except Exception as e:
        logger.error(f"Error decrypting tokens with ECDH: {e}", exc_info=True)


if __name__ == "__main__":
    main()
