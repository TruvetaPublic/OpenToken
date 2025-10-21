"""
Copyright (c) Truveta. All rights reserved.
"""

import pytest

from opentoken.attributes.person.birth_date_attribute import BirthDateAttribute
from opentoken.attributes.person.first_name_attribute import FirstNameAttribute
from opentoken.attributes.person.last_name_attribute import LastNameAttribute
from opentoken.attributes.person.sex_attribute import SexAttribute
from opentoken.attributes.person.social_security_number_attribute import SocialSecurityNumberAttribute
from opentoken.tokens.token_definition import TokenDefinition
from opentoken.tokens.token_generator import TokenGenerator


class TestTokenGeneratorIntegration:
    """Integration test cases for TokenGenerator."""

    def setup_method(self):
        """Set up test fixtures before each test method."""
        # Setup real TokenDefinition and TokenTransformers for integration testing
        self.token_definition = TokenDefinition()
        self.token_transformer_list = []

        self.token_generator = TokenGenerator(self.token_definition, self.token_transformer_list)

    def test_get_all_tokens_valid_person_attributes_generates_tokens(self):
        """Test that valid person attributes generate the expected tokens."""
        # Define token identifiers and attribute expressions
        token_definition = TokenDefinition()

        # Person attributes to be used for token generation
        person_attributes = {
            FirstNameAttribute: "Alice",
            LastNameAttribute: "Wonderland",
            SocialSecurityNumberAttribute: "345-54-6795",
            SexAttribute: "F",
            BirthDateAttribute: "1993-08-10"
        }

        # Generate all tokens
        tokens = self.token_generator.get_all_tokens(person_attributes).tokens

        # Validate the tokens
        assert tokens is not None
        assert len(tokens) == 5, "Expected 5 tokens to be generated"

        # Validate the actual tokens generated
        assert "T1" in tokens
        assert "T2" in tokens
        assert "T3" in tokens
        assert "T4" in tokens
        assert "T5" in tokens

        assert tokens.get("T1") == "02292af14559b4c2a28a772536b81760ad7b8ebac8ce49e8450ca0fa5044e37f"
        assert tokens.get("T2") == "0000000000000000000000000000000000000000000000000000000000000000"
        assert tokens.get("T3") == "a76c3bff664bec8d0f77b4b47ad555d212dc671949ed3cf1c1edef68733835b2"
        assert tokens.get("T4") == "21c3cf1fdb4fd45197e5def14d0228d26c56bcec1b8641079f9b9ec24f9a6a0b"
        assert tokens.get("T5") == "3756556f2323148cb57e1e13b1abcd457e1c1706a84ae83d522a3fc0ad43506d"

