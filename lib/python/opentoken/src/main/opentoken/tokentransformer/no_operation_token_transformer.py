"""
Copyright (c) Truveta. All rights reserved.
"""

from opentoken.tokens.token import Token
from opentoken.tokentransformer.token_transformer import TokenTransformer


class NoOperationTokenTransformer(TokenTransformer):
    """
    A No Operation token transformer. No transformation is
    applied whatsoever.
    """

    def transform(self, token: str) -> str:
        """
        No operation token transformer.

        Does not transform the token in any ways.

        Args:
            token: The token to be transformed.

        Returns:
            The unchanged token.
        """
        if token is None or (isinstance(token, str) and token.strip() == ""):
            token = Token.BLANK  # Return blank token for null or blank input
        return token
