"""
Copyright (c) Truveta. All rights reserved.
"""

import argparse
from typing import Optional


class CommandLineArguments:
    """Processes the application's command line arguments."""

    TYPE_CSV = "csv"
    TYPE_PARQUET = "parquet"

    def __init__(self):
        """Initialize with default values."""
        self.input_path: str = "csv"
        self.input_type: str = ""
        self.output_path: str = ""
        self.output_type: str = ""
        self.receiver_public_key: Optional[str] = None
        self.sender_public_key: Optional[str] = None
        self.sender_keypair_path: Optional[str] = None
        self.receiver_keypair_path: Optional[str] = None
        self.generate_keypair: bool = False
        self.decrypt_with_ecdh: bool = False
        self.hash_only: bool = False
        self.ecdh_curve: str = "P-256"

    @classmethod
    def parse_args(cls, args: Optional[list] = None) -> 'CommandLineArguments':
        """
        Parse command line arguments and return a CommandLineArguments instance.

        Args:
            args: List of command line arguments. If None, uses sys.argv.

        Returns:
            CommandLineArguments instance with parsed values.
        """
        parser = argparse.ArgumentParser(
            description="OpenToken command line tool for ECDH-based secure token generation",
            add_help=False
            )

        parser.add_argument(
            "--help",
            action="help",
            help="Show this help message and exit."
        )

        parser.add_argument(
            "-i", "--input",
            dest="input_path",
            help="Input file path.",
            required=True
        )

        parser.add_argument(
            "-t", "--type",
            dest="input_type",
            help="Input file type.",
            required=True
        )

        parser.add_argument(
            "-o", "--output",
            dest="output_path",
            help="Output file path.",
            required=True
        )

        parser.add_argument(
            "-ot", "--output-type",
            dest="output_type",
            help="Output file type if different from input.",
            required=False,
            default=""
        )

        parser.add_argument(
            "--receiver-public-key",
            dest="receiver_public_key",
            help="Path to receiver's public key file for ECDH key exchange.",
            required=False,
            default=None
        )

        parser.add_argument(
            "--sender-public-key",
            dest="sender_public_key",
            help="Path to sender's public key file (for decryption with ECDH).",
            required=False,
            default=None
        )

        parser.add_argument(
            "--sender-keypair-path",
            dest="sender_keypair_path",
            help="Path to sender's private key file (default: ~/.opentoken/keypair.pem).",
            required=False,
            default=None
        )

        parser.add_argument(
            "--receiver-keypair-path",
            dest="receiver_keypair_path",
            help="Path to receiver's private key file (default: ~/.opentoken/keypair.pem).",
            required=False,
            default=None
        )

        parser.add_argument(
            "--generate-keypair",
            dest="generate_keypair",
            help="Generate a new ECDH P-256 key pair and exit.",
            action="store_true",
            required=False,
            default=False
        )

        parser.add_argument(
            "-h", "--hash-only",
            dest="hash_only",
            help="Hash-only mode. Generate hashed tokens without encryption (keys still derived via ECDH).",
            action="store_true",
            required=False,
            default=False
        )

        parser.add_argument(
            "-d", "--decrypt",
            dest="decrypt_with_ecdh",
            help="Decrypt mode using ECDH key exchange.",
            action="store_true",
            required=False,
            default=False
        )

        parser.add_argument(
            "--ecdh-curve",
            dest="ecdh_curve",
            help="Elliptic curve name for ECDH (default: P-256 / secp256r1).",
            required=False,
            default="P-256"
        )

        parsed_args = parser.parse_args(args)

        # Create instance and populate with parsed values
        instance = cls()
        instance.input_path = parsed_args.input_path
        instance.input_type = parsed_args.input_type
        instance.output_path = parsed_args.output_path
        instance.output_type = parsed_args.output_type
        instance.receiver_public_key = parsed_args.receiver_public_key
        instance.sender_public_key = parsed_args.sender_public_key
        instance.sender_keypair_path = parsed_args.sender_keypair_path
        instance.receiver_keypair_path = parsed_args.receiver_keypair_path
        instance.generate_keypair = parsed_args.generate_keypair
        instance.hash_only = parsed_args.hash_only
        instance.decrypt_with_ecdh = parsed_args.decrypt_with_ecdh
        instance.ecdh_curve = parsed_args.ecdh_curve

        return instance

    # Property accessors for compatibility with Java-style getters
    @property
    def inputPath(self) -> str:
        """Get the input path (Java-style getter for compatibility)."""
        return self.input_path

    @property
    def inputType(self) -> str:
        """Get the input type (Java-style getter for compatibility)."""
        return self.input_type

    @property
    def outputPath(self) -> str:
        """Get the output path (Java-style getter for compatibility)."""
        return self.output_path

    @property
    def outputType(self) -> str:
        """Get the output type (Java-style getter for compatibility)."""
        return self.output_type

    def __str__(self) -> str:
        """String representation of the command line arguments."""
        return (
            f"CommandLineArguments("
            f"input_path='{self.input_path}', "
            f"input_type='{self.input_type}', "
            f"output_path='{self.output_path}', "
            f"output_type='{self.output_type}')"
        )

    def __repr__(self) -> str:
        """Detailed representation of the command line arguments."""
        return self.__str__()
