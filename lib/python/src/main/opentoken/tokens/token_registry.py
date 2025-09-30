"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import Dict, List
from opentoken.attributes.attribute_expression import AttributeExpression
from opentoken.tokens.definitions.t1_token import T1Token
from opentoken.tokens.definitions.t2_token import T2Token
from opentoken.tokens.definitions.t3_token import T3Token
from opentoken.tokens.definitions.t4_token import T4Token
from opentoken.tokens.definitions.t5_token import T5Token

class TokenRegistry:
    """
    Loads all implementations of Token and returns their definitions.
    """
    @staticmethod
    def load_all_tokens() -> Dict[str, List[AttributeExpression]]:
        definitions: Dict[str, List[AttributeExpression]] = {}
        token_classes = [T1Token, T2Token, T3Token, T4Token, T5Token]
        for token_class in token_classes:
            token = token_class()
            definitions[token.get_identifier()] = token.get_definition()
        return definitions
