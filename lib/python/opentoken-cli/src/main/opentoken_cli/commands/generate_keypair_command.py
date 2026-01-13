"""
Copyright (c) Truveta. All rights reserved.
"""



class GenerateKeypairCommand:
    """Command for generating ECDH key pairs."""

    def __init__(self):
        """Initialize command with default values."""
        self.ecdh_curve = "P-384"
        self.output_dir = None

    @staticmethod
    def configure_parser(subparsers):
        """
        Configure the argument parser for the generate-keypair subcommand.
        
        Args:
            subparsers: The subparsers object from argparse.
        
        Returns:
            The configured parser for this command.
        """
        parser = subparsers.add_parser(
            'generate-keypair',
            help='Generate a new ECDH key pair',
            description='Generate a new ECDH key pair'
        )
        
        parser.add_argument(
            '--ecdh-curve',
            dest='ecdh_curve',
            help='Elliptic curve name for ECDH (default: P-384 / secp384r1)',
            default='P-384',
            required=False
        )
        
        parser.add_argument(
            '--output-dir',
            dest='output_dir',
            help='Directory to save the key pair (default: ~/.opentoken)',
            default=None,
            required=False
        )
        
        return parser

    @classmethod
    def from_args(cls, args):
        """
        Create a GenerateKeypairCommand instance from parsed arguments.
        
        Args:
            args: Parsed arguments from argparse.
        
        Returns:
            GenerateKeypairCommand instance.
        """
        instance = cls()
        instance.ecdh_curve = args.ecdh_curve
        instance.output_dir = args.output_dir
        return instance
