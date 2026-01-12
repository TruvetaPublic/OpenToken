"""
Copyright (c) Truveta. All rights reserved.
"""



class TokenizeCommand:
    """Command for tokenizing person attributes (with or without encryption)."""

    def __init__(self):
        """Initialize command with default values."""
        self.input_path = None
        self.input_type = None
        self.output_path = None
        self.output_type = None
        self.receiver_public_key = None
        self.sender_keypair_path = None
        self.hash_only = False
        self.ecdh_curve = "P-256"

    @staticmethod
    def configure_parser(subparsers):
        """
        Configure the argument parser for the tokenize subcommand.
        
        Args:
            subparsers: The subparsers object from argparse.
        
        Returns:
            The configured parser for this command.
        """
        parser = subparsers.add_parser(
            'tokenize',
            help='Tokenize person attributes using ECDH key exchange',
            description='Tokenize person attributes using ECDH key exchange'
        )
        
        parser.add_argument(
            '-i', '--input',
            dest='input_path',
            help='Input file path',
            required=True
        )
        
        parser.add_argument(
            '-t', '--type',
            dest='input_type',
            help='Input file type (csv or parquet)',
            required=True
        )
        
        parser.add_argument(
            '-o', '--output',
            dest='output_path',
            help='Output file path',
            required=True
        )
        
        parser.add_argument(
            '-ot', '--output-type',
            dest='output_type',
            help='Output file type if different from input (csv or parquet)',
            default=None,
            required=False
        )
        
        parser.add_argument(
            '--receiver-public-key',
            dest='receiver_public_key',
            help="Path to receiver's public key file for ECDH key exchange",
            required=True
        )
        
        parser.add_argument(
            '--sender-keypair-path',
            dest='sender_keypair_path',
            help="Path to sender's private key file (default: ~/.opentoken/keypair.pem)",
            default=None,
            required=False
        )
        
        parser.add_argument(
            '--hash-only',
            dest='hash_only',
            help='Hash-only mode. Generates hashed tokens without encryption',
            action='store_true',
            default=False,
            required=False
        )
        
        parser.add_argument(
            '--ecdh-curve',
            dest='ecdh_curve',
            help='Elliptic curve name for ECDH (default: P-256 / secp256r1)',
            default='P-256',
            required=False
        )
        
        return parser

    @classmethod
    def from_args(cls, args):
        """
        Create a TokenizeCommand instance from parsed arguments.
        
        Args:
            args: Parsed arguments from argparse.
        
        Returns:
            TokenizeCommand instance.
        """
        instance = cls()
        instance.input_path = args.input_path
        instance.input_type = args.input_type
        instance.output_path = args.output_path
        instance.output_type = args.output_type
        instance.receiver_public_key = args.receiver_public_key
        instance.sender_keypair_path = args.sender_keypair_path
        instance.hash_only = args.hash_only
        instance.ecdh_curve = args.ecdh_curve
        return instance
