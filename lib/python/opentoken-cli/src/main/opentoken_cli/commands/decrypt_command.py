"""
Copyright (c) Truveta. All rights reserved.
"""



class DecryptCommand:
    """Command for decrypting tokens using ECDH key exchange."""

    def __init__(self):
        """Initialize command with default values."""
        self.input_path = None
        self.input_type = None
        self.output_path = None
        self.output_type = None
        self.sender_public_key = None
        self.receiver_keypair_path = None
        self.ecdh_curve = "P-384"

    @staticmethod
    def configure_parser(subparsers):
        """
        Configure the argument parser for the decrypt subcommand.
        
        Args:
            subparsers: The subparsers object from argparse.
        
        Returns:
            The configured parser for this command.
        """
        parser = subparsers.add_parser(
            'decrypt',
            help='Decrypt tokens using ECDH key exchange',
            description='Decrypt tokens using ECDH key exchange'
        )
        
        parser.add_argument(
            '-i', '--input',
            dest='input_path',
            help='Input file path (or ZIP package)',
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
            '--sender-public-key',
            dest='sender_public_key',
            help="Path to sender's public key file (extracted from ZIP if not provided)",
            default=None,
            required=False
        )
        
        parser.add_argument(
            '--receiver-keypair-path',
            dest='receiver_keypair_path',
            help="Path to receiver's private key file (default: ~/.opentoken/keypair.pem)",
            default=None,
            required=False
        )
        
        parser.add_argument(
            '--ecdh-curve',
            dest='ecdh_curve',
            help='Elliptic curve name for ECDH (default: P-384 / secp384r1)',
            default='P-384',
            required=False
        )
        
        return parser

    @classmethod
    def from_args(cls, args):
        """
        Create a DecryptCommand instance from parsed arguments.
        
        Args:
            args: Parsed arguments from argparse.
        
        Returns:
            DecryptCommand instance.
        """
        instance = cls()
        instance.input_path = args.input_path
        instance.input_type = args.input_type
        instance.output_path = args.output_path
        instance.output_type = args.output_type
        instance.sender_public_key = args.sender_public_key
        instance.receiver_keypair_path = args.receiver_keypair_path
        instance.ecdh_curve = args.ecdh_curve
        return instance
