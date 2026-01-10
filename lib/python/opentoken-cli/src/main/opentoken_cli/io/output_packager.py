"""
Copyright (c) Truveta. All rights reserved.

Utility class for packaging OpenToken output as ZIP files.
"""

import logging
import os
import shutil
import tempfile
import zipfile

from cryptography.hazmat.primitives.asymmetric import ec

from opentoken.keyexchange import KeyPairManager, PublicKeyLoader, KeyExchangeException


logger = logging.getLogger(__name__)


class OutputPackager:
    """
    Utility class for packaging OpenToken output as ZIP files.
    
    Handles creating ZIP packages containing tokens, metadata, and sender's public key
    for ECDH-based key exchange workflows.
    """
    
    SENDER_PUBLIC_KEY_FILENAME = "sender_public_key.pem"
    
    @staticmethod
    def package_output(tokens_file: str, metadata_file: str, 
                      sender_public_key: ec.EllipticCurvePublicKey, 
                      output_zip_path: str) -> None:
        """
        Package output files into a ZIP archive.
        
        Args:
            tokens_file: The tokens file (CSV or Parquet)
            metadata_file: The metadata JSON file
            sender_public_key: The sender's public key
            output_zip_path: The output ZIP file path
            
        Raises:
            IOError: If an I/O error occurs
            KeyExchangeException: If key operations fail
        """
        logger.info(f"Packaging output to ZIP: {output_zip_path}")
        
        # Create temp file for sender's public key
        temp_dir = tempfile.mkdtemp()
        try:
            temp_public_key_file = os.path.join(temp_dir, OutputPackager.SENDER_PUBLIC_KEY_FILENAME)
            
            # Save sender's public key to temp file
            key_pair_manager = KeyPairManager()
            key_pair_manager.save_public_key(sender_public_key, temp_public_key_file)
            
            # Create ZIP
            with zipfile.ZipFile(output_zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
                # Add tokens file
                zipf.write(tokens_file, os.path.basename(tokens_file))
                logger.debug(f"Added tokens file to ZIP: {tokens_file}")
                
                # Add metadata file
                zipf.write(metadata_file, os.path.basename(metadata_file))
                logger.debug(f"Added metadata file to ZIP: {metadata_file}")
                
                # Add sender's public key
                zipf.write(temp_public_key_file, OutputPackager.SENDER_PUBLIC_KEY_FILENAME)
                logger.debug("Added sender public key to ZIP")
            
            logger.info(f"Successfully created ZIP package: {output_zip_path}")
        finally:
            # Clean up temp directory
            shutil.rmtree(temp_dir, ignore_errors=True)
    
    @staticmethod
    def extract_sender_public_key(zip_file_path: str) -> ec.EllipticCurvePublicKey:
        """
        Extract sender's public key from a ZIP package.
        
        Args:
            zip_file_path: The path to the ZIP file
            
        Returns:
            The extracted sender's public key
            
        Raises:
            IOError: If an I/O error occurs
            KeyExchangeException: If key loading fails
        """
        logger.info(f"Extracting sender's public key from ZIP: {zip_file_path}")
        
        # Create temp directory for extraction
        temp_dir = tempfile.mkdtemp()
        try:
            # Extract sender's public key
            with zipfile.ZipFile(zip_file_path, 'r') as zipf:
                if OutputPackager.SENDER_PUBLIC_KEY_FILENAME in zipf.namelist():
                    # Extract public key file
                    public_key_path = os.path.join(temp_dir, OutputPackager.SENDER_PUBLIC_KEY_FILENAME)
                    zipf.extract(OutputPackager.SENDER_PUBLIC_KEY_FILENAME, temp_dir)
                    
                    # Load and return the public key
                    loader = PublicKeyLoader()
                    public_key = loader.load_public_key(public_key_path)
                    logger.info("Successfully extracted sender's public key")
                    return public_key
                else:
                    raise KeyExchangeException("Sender's public key not found in ZIP package")
        finally:
            # Clean up temp directory
            shutil.rmtree(temp_dir, ignore_errors=True)
    
    @staticmethod
    def extract_tokens_file(zip_file_path: str, output_path: str) -> None:
        """
        Extract tokens file from a ZIP package.
        
        Args:
            zip_file_path: The path to the ZIP file
            output_path: The path where tokens should be extracted
            
        Raises:
            IOError: If an I/O error occurs
        """
        logger.info(f"Extracting tokens from ZIP: {zip_file_path}")
        
        with zipfile.ZipFile(zip_file_path, 'r') as zipf:
            for entry_name in zipf.namelist():
                # Extract the tokens file (CSV or Parquet, but not metadata or public key)
                if ((entry_name.endswith('.csv') or entry_name.endswith('.parquet'))
                    and not entry_name.endswith('.metadata.json')
                    and entry_name != OutputPackager.SENDER_PUBLIC_KEY_FILENAME):
                    
                    logger.debug(f"Extracting tokens file: {entry_name}")
                    
                    # Extract to a temp location first
                    temp_dir = tempfile.mkdtemp()
                    try:
                        extracted_path = zipf.extract(entry_name, temp_dir)
                        # Move to desired output location
                        shutil.move(extracted_path, output_path)
                        logger.info(f"Tokens extracted to: {output_path}")
                        return
                    finally:
                        shutil.rmtree(temp_dir, ignore_errors=True)
    
    @staticmethod
    def is_zip_file(file_path: str) -> bool:
        """
        Check if a file is a ZIP file based on its extension.
        
        Args:
            file_path: The file path to check
            
        Returns:
            True if the file has a .zip extension
        """
        return file_path is not None and file_path.lower().endswith('.zip')
