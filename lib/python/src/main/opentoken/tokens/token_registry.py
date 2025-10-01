"""
Copyright (c) Truveta. All rights reserved.
"""

import pkgutil
import importlib
import pathlib
from typing import Dict, List
from opentoken.attributes.attribute_expression import AttributeExpression
from opentoken.tokens.token import Token


class TokenRegistry:
    @staticmethod
    def load_all_tokens() -> Dict[str, List[AttributeExpression]]:
        definitions: Dict[str, List[AttributeExpression]] = {}

        # package name for import
        package = "opentoken.tokens.definitions"

        # real filesystem path (relative to this file)
        package_path = str(pathlib.Path(__file__).parent / "definitions")

        # iterate over modules in the definitions package
        for _, modname, _ in pkgutil.iter_modules([package_path]):
            module = importlib.import_module(f"{package}.{modname}")

            # scan for Token subclasses
            for obj in module.__dict__.values():
                if isinstance(obj, type) and issubclass(obj, Token) and obj is not Token:
                    token = obj()
                    definitions[token.get_identifier()] = token.get_definition()

        return definitions
