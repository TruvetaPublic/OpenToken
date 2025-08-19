"""
Copyright (c) Truveta. All rights reserved.
"""

import importlib
import pkgutil
from typing import Dict, List, Set
from opentoken.attributes.attribute_expression import AttributeExpression
from opentoken.tokens.base_token_definition import BaseTokenDefinition
from opentoken.tokens.token import Token


class TokenDefinition(BaseTokenDefinition):
    """
    Encapsulates the token definitions.

    The tokens are generated using some token generation rules. This class
    encapsulates the definition of those rules. Together, they are commonly
    referred to as token definitions or rule definitions.

    Each token/rule definition is a collection of AttributeExpression that are
    concatenated together to get the token signature.
    """

    def __init__(self):
        """Initialize the token definitions."""
        self.definitions: Dict[str, List[AttributeExpression]] = {}
        self._load_token_definitions()

    def _load_token_definitions(self):
        """Load all implementations of Token interface and store in definitions."""
        # Import the definitions module to ensure all token classes are loaded
        try:
            definitions_module = importlib.import_module('opentoken.tokens.definitions')

            # Walk through all modules in the definitions package
            for importer, modname, ispkg in pkgutil.iter_modules(definitions_module.__path__):
                if not ispkg:
                    full_module_name = f'opentoken.tokens.definitions.{modname}'
                    module = importlib.import_module(full_module_name)

                    # Look for Token implementations in the module
                    for item_name in dir(module):
                        item = getattr(module, item_name)
                        if (isinstance(item, type) and issubclass(item, Token) and item is not Token):
                            try:
                                token_instance = item()
                                token_id = token_instance.get_identifier()
                                self.definitions[token_id] = token_instance.get_definition()
                            except Exception as e:
                                raise RuntimeError(f"Failed to instantiate token {item_name}") from e
        except ImportError:
            # If definitions module doesn't exist, manually load known tokens
            from opentoken.tokens.definitions.t1_token import T1Token
            from opentoken.tokens.definitions.t2_token import T2Token
            from opentoken.tokens.definitions.t3_token import T3Token
            from opentoken.tokens.definitions.t4_token import T4Token
            from opentoken.tokens.definitions.t5_token import T5Token

            token_classes = [T1Token, T2Token, T3Token, T4Token, T5Token]
            for token_class in token_classes:
                try:
                    token = token_class()
                    self.definitions[token.get_identifier()] = token.get_definition()
                except Exception as e:
                    raise RuntimeError(f"Failed to instantiate token {token_class.__name__}") from e

    def get_version(self) -> str:
        """Get the version of the token definition."""
        return "2.0"

    def get_token_identifiers(self) -> Set[str]:
        """Get all token identifiers."""
        return set(self.definitions.keys())

    def get_token_definition(self, token_id: str) -> List[AttributeExpression]:
        """Get the token definition for a given token identifier."""
        return self.definitions.get(token_id, [])
