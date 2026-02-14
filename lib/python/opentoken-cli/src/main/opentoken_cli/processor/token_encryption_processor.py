"""
Copyright (c) Truveta. All rights reserved.
"""

import logging

from opentoken.tokens.token import Token
from opentoken.tokentransformer.encrypt_token_transformer import EncryptTokenTransformer
from opentoken_cli.io.token_reader import TokenReader
from opentoken_cli.io.token_writer import TokenWriter
from opentoken_cli.processor.token_constants import TokenConstants

logger = logging.getLogger(__name__)


class TokenEncryptionProcessor:
    """
    Process hashed tokens for encryption.

    This class is used to read hashed tokens from input source,
    encrypt them, and write the encrypted tokens to the output data source.
    """

    @staticmethod
    def process(reader: TokenReader, writer: TokenWriter, encryptor: EncryptTokenTransformer):
        """
        Reads hashed tokens from the input data source, encrypts them, and
        writes the result back to the output data source.

        Args:
            reader: Iterator providing hashed token rows
            writer: Writer instance with write_token method
            encryptor: The encryption transformer
        """
        row_counter = 0
        encrypted_counter = 0
        error_counter = 0

        for row in reader:
            row_counter += 1

            token = row.get(TokenConstants.TOKEN, "")

            # Encrypt the token if it's not blank
            if token and token != Token.BLANK:
                try:
                    encrypted_token = encryptor.transform(token)
                    row[TokenConstants.TOKEN] = encrypted_token
                    encrypted_counter += 1
                except Exception as e:
                    logger.error(
                        f"Failed to encrypt token for RecordId {row.get(TokenConstants.RECORD_ID)}, "
                        f"RuleId {row.get(TokenConstants.RULE_ID)}: {e}"
                    )
                    error_counter += 1
                    # Keep the hashed token in case of error

            writer.write_token(row)

            if row_counter % 10000 == 0:
                logger.info(f'Processed "{row_counter:,}" tokens')

        logger.info(f"Processed a total of {row_counter:,} tokens")
        logger.info(f"Successfully encrypted {encrypted_counter:,} tokens")
        if error_counter > 0:
            logger.warning(f"Failed to encrypt {error_counter:,} tokens")
