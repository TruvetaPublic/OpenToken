"""
Copyright (c) Truveta. All rights reserved.
"""

import argparse
import logging
import os
import sys

from opentoken_cli.commands import GenerateKeypairCommand, TokenizeCommand, DecryptCommand
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
from opentoken.keyexchange import KeyPairManager, KeyExchange, PublicKeyLoader, KeyExchangeException

from cryptography.hazmat.primitives import serialization


# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main(argv=None):
    """Main entry point for the OpenToken application."""
    # Build argument parser with subcommands
    parser = argparse.ArgumentParser(
        prog='opentoken',
        description='OpenToken command line tool for ECDH-based secure token generation'
    )
    
    subparsers = parser.add_subparsers(dest='command', help='Available commands', required=True)
    
    # Configure subcommands
    GenerateKeypairCommand.configure_parser(subparsers)
    TokenizeCommand.configure_parser(subparsers)
    DecryptCommand.configure_parser(subparsers)
    
    # Parse arguments
    try:
        args = parser.parse_args(argv)
    except SystemExit as e:
        if e.code != 0:
            sys.exit(e.code)
        return
    
    # Route to appropriate handler
    if args.command == 'generate-keypair':
        command = GenerateKeypairCommand.from_args(args)
        _handle_generate_keypair(command)
    elif args.command == 'tokenize':
        command = TokenizeCommand.from_args(args)
        _handle_tokenize(command)
    elif args.command == 'decrypt':
        command = DecryptCommand.from_args(args)
        _handle_decrypt(command)
    else:
        logger.error(f"Unknown command: {args.command}")
        parser.print_help()
        sys.exit(1)


def _handle_generate_keypair(command: GenerateKeypairCommand):
    """
    Handle the generate-keypair subcommand.
    
    Args:
        command: The parsed command object.
    """
    key_dir = command.output_dir if command.output_dir else None
    _generate_keypair(command.ecdh_curve, key_dir)


def _handle_tokenize(command: TokenizeCommand):
    """
    Handle the tokenize subcommand.
    
    Args:
        command: The parsed command object.
    """
    input_path = command.input_path
    input_type = command.input_type
    output_path = command.output_path
    output_type = command.output_type if command.output_type else input_type
    receiver_public_key_path = command.receiver_public_key
    sender_keypair_path = command.sender_keypair_path
    hash_only = command.hash_only
    ecdh_curve = command.ecdh_curve
    
    mode = 'Hash-only (ECDH-derived hash key, no encryption)' if hash_only else 'Encrypt with ECDH'
    logger.info(f"Mode: {mode}")
    logger.info(f"ECDH Curve: {ecdh_curve}")
    logger.info(f"Receiver Public Key: {receiver_public_key_path}")
    logger.info(f"Input Path: {input_path}")
    logger.info(f"Input Type: {input_type}")
    logger.info(f"Output Path: {output_path}")
    logger.info(f"Output Type: {output_type}")
    
    # Validate input and output types
    if input_type not in ['csv', 'parquet']:
        logger.error("Only csv and parquet input types are supported!")
        sys.exit(1)
    if output_type not in ['csv', 'parquet']:
        logger.error("Only csv and parquet output types are supported!")
        sys.exit(1)
    
    _process_tokens_with_ecdh(input_path, output_path, input_type, output_type,
                             receiver_public_key_path, sender_keypair_path, hash_only, ecdh_curve)


def _handle_decrypt(command: DecryptCommand):
    """
    Handle the decrypt subcommand.
    
    Args:
        command: The parsed command object.
    """
    input_path = command.input_path
    input_type = command.input_type
    output_path = command.output_path
    output_type = command.output_type if command.output_type else input_type
    sender_public_key_path = command.sender_public_key
    receiver_keypair_path = command.receiver_keypair_path
    ecdh_curve = command.ecdh_curve
    
    logger.info("Mode: Decrypt with ECDH")
    logger.info(f"Sender Public Key: {sender_public_key_path}")
    logger.info(f"Input Path: {input_path}")
    logger.info(f"Input Type: {input_type}")
    logger.info(f"Output Path: {output_path}")
    logger.info(f"Output Type: {output_type}")
    
    # Validate input and output types
    if input_type not in ['csv', 'parquet']:
        logger.error("Only csv and parquet input types are supported!")
        sys.exit(1)
    if output_type not in ['csv', 'parquet']:
        logger.error("Only csv and parquet output types are supported!")
        sys.exit(1)
    
    _decrypt_tokens_with_ecdh(input_path, output_path, input_type, output_type,
                             sender_public_key_path, receiver_keypair_path, ecdh_curve)
    logger.info("Token decryption completed successfully.")


def _create_person_attributes_reader(input_path: str, input_type: str):
    """Create a PersonAttributesReader based on input type."""
    input_type_lower = input_type.lower()
    if input_type_lower == 'csv':
        return PersonAttributesCSVReader(input_path)
    elif input_type_lower == 'parquet':
        return PersonAttributesParquetReader(input_path)
    else:
        raise ValueError(f"Unsupported input type: {input_type}")


def _create_person_attributes_writer(output_path: str, output_type: str):
    """Create a PersonAttributesWriter based on output type."""
    output_type_lower = output_type.lower()
    if output_type_lower == 'csv':
        return PersonAttributesCSVWriter(output_path)
    elif output_type_lower == 'parquet':
        return PersonAttributesParquetWriter(output_path)
    else:
        raise ValueError(f"Unsupported output type: {output_type}")


def _create_token_reader(input_path: str, input_type: str):
    """Create a TokenReader based on input type."""
    input_type_lower = input_type.lower()
    if input_type_lower == 'csv':
        return TokenCSVReader(input_path)
    elif input_type_lower == 'parquet':
        return TokenParquetReader(input_path)
    else:
        raise ValueError(f"Unsupported input type: {input_type}")


def _create_token_writer(output_path: str, output_type: str):
    """Create a TokenWriter based on output type."""
    output_type_lower = output_type.lower()
    if output_type_lower == 'csv':
        return TokenCSVWriter(output_path)
    elif output_type_lower == 'parquet':
        return TokenParquetWriter(output_path)
    else:
        raise ValueError(f"Unsupported output type: {output_type}")


def _generate_keypair(ecdh_curve: str = "P-384", key_dir: str = None):
    """Generate a new ECDH key pair and save it to the specified location."""
    try:
        logger.info(f"Generating new ECDH key pair (curve: {ecdh_curve})...")
        key_pair_manager = KeyPairManager(key_directory=key_dir, curve_name=ecdh_curve)
        _, _ = key_pair_manager.generate_and_save_key_pair()
        
        logger.info("✓ Key pair generated successfully")
        logger.info(f"✓ Private key saved to: {key_pair_manager.get_key_directory()}/keypair.pem (0600 permissions)")
        logger.info(f"✓ Public key saved to: {key_pair_manager.get_key_directory()}/public_key.pem")
        
    except KeyExchangeException as e:
        logger.error(f"Error generating key pair: {e}")


def _process_tokens_with_ecdh(input_path: str, output_path: str, input_type: str, output_type: str,
                              receiver_public_key_path: str, sender_keypair_path: str = None,
                              hash_only: bool = False, ecdh_curve: str = "P-384"):
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
            sender_key_manager = KeyPairManager(os.path.dirname(sender_keypair_path), curve_name=ecdh_curve)
        else:
            sender_key_manager = KeyPairManager(curve_name=ecdh_curve)
        sender_private_key, sender_public_key = sender_key_manager.get_or_create_key_pair()
        logger.info(f"✓ Sender key pair ready (saved to: {sender_key_manager.get_key_directory()})")
        
        # Perform ECDH key exchange
        key_exchange = KeyExchange()
        keys = key_exchange.exchange_and_derive_keys(sender_private_key, receiver_public_key)
        logger.info("✓ Performed ECDH key exchange")
        logger.info("✓ Derived hashing key (32 bytes)")
        if not hash_only:
            logger.info("✓ Derived encryption key (32 bytes)")
        
        # Create transformers with derived keys
        token_transformer_list = [
            HashTokenTransformer(keys.get_hashing_key_as_string())
        ]
        if not hash_only:
            token_transformer_list.append(EncryptTokenTransformer(keys.get_encryption_key_as_string()))
        
        # Create temporary output for tokens
        temp_output_path = output_path.replace('.zip', f'_temp.{output_type}') if output_path.endswith('.zip') else f"{output_path}_temp"
        base_path = temp_output_path.rsplit('.', 1)[0] if '.' in temp_output_path else temp_output_path
        metadata_path = base_path + Metadata.METADATA_FILE_EXTENSION
        
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
            metadata_writer = MetadataJsonWriter(base_path)
            metadata_writer.write(metadata_map)

        # Package output as ZIP if needed (after writer is closed/flushed)
        if OutputPackager.is_zip_file(output_path):
            OutputPackager.package_output(temp_output_path, metadata_path,
                                        sender_public_key, output_path)
            logger.info(f"✓ Output package created: {output_path}")
            logger.info(f"  ├─ tokens.{output_type} ({'hashed' if hash_only else 'encrypted'})")
            logger.info("  ├─ tokens.metadata.json")
            logger.info("  └─ sender_public_key.pem")

            # Clean up temp files
            if os.path.exists(temp_output_path):
                os.remove(temp_output_path)
            if os.path.exists(metadata_path):
                os.remove(metadata_path)
        else:
            # Rename temp files to final output names
            if os.path.exists(temp_output_path):
                os.rename(temp_output_path, output_path)
            # Rename metadata file so it matches the final output base path
            if os.path.exists(metadata_path):
                output_base_path = output_path.rsplit(".", 1)[0] if "." in output_path else output_path
                final_metadata_path = output_base_path + Metadata.METADATA_FILE_EXTENSION
                os.rename(metadata_path, final_metadata_path)
            logger.info("✓ Tokens generated successfully")
            logger.info("Note: Use .zip extension for automatic packaging with sender's public key")
        
    except Exception as e:
        logger.error(f"Error processing tokens with ECDH: {e}", exc_info=True)


def _decrypt_tokens_with_ecdh(input_path: str, output_path: str, input_type: str, output_type: str,
                              sender_public_key_path: str = None, receiver_keypair_path: str = None,
                              ecdh_curve: str = "P-384"):
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
            receiver_key_manager = KeyPairManager(os.path.dirname(receiver_keypair_path), curve_name=ecdh_curve)
            receiver_private_key, _ = receiver_key_manager.load_key_pair(receiver_keypair_path)
        else:
            receiver_key_manager = KeyPairManager(curve_name=ecdh_curve)
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
