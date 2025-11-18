"""
Copyright (c) Truveta. All rights reserved.
"""

import logging

from opentoken.io.token_reader import TokenReader
from opentoken.io.token_writer import TokenWriter
from opentoken.processor.token_constants import TokenConstants
from opentoken.tokens.token import Token
from opentoken.tokentransformer.decrypt_token_transformer import DecryptTokenTransformer


logger = logging.getLogger(__name__)


class TokenDecryptionProcessor:
    """
    Process encrypted tokens for decryption.
    
    This class is used to read encrypted tokens from input source,
    decrypt them, and write the decrypted tokens to the output data source.
    """

    @staticmethod
    def process(reader: TokenReader, writer: TokenWriter, decryptor: DecryptTokenTransformer):
        """
        Reads encrypted tokens from the input data source, decrypts them, and
        writes the result back to the output data source.
        
        Args:
            reader: Iterator providing encrypted token rows
            writer: Writer instance with write_token method
            decryptor: The decryption transformer
        """
        row_counter = 0
        decrypted_counter = 0
        error_counter = 0

        for row in reader:
            row_counter += 1
            
            token = row.get(TokenConstants.TOKEN, '')
            
            # Decrypt the token if it's not blank
            if token and token != Token.BLANK:
                try:
                    decrypted_token = decryptor.transform(token)
                    row[TokenConstants.TOKEN] = decrypted_token
                    decrypted_counter += 1
                except Exception as e:
                    logger.error(f"Failed to decrypt token for RecordId {row.get(TokenConstants.RECORD_ID)}, "
                               f"RuleId {row.get(TokenConstants.RULE_ID)}: {e}")
                    error_counter += 1
                    # Keep the encrypted token in case of error
            
            writer.write_token(row)

            if row_counter % 10000 == 0:
                logger.info(f'Processed "{row_counter:,}" tokens')

        logger.info(f'Processed a total of {row_counter:,} tokens')
        logger.info(f'Successfully decrypted {decrypted_counter:,} tokens')
        if error_counter > 0:
            logger.warning(f'Failed to decrypt {error_counter:,} tokens')
