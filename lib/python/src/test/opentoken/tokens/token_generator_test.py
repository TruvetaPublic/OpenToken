"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import Type
from unittest.mock import Mock, patch

import pytest

from opentoken.attributes.attribute import Attribute
from opentoken.attributes.attribute_expression import AttributeExpression
from opentoken.attributes.person.first_name_attribute import FirstNameAttribute
from opentoken.attributes.person.last_name_attribute import LastNameAttribute
from opentoken.tokens.base_token_definition import BaseTokenDefinition
from opentoken.tokens.sha256_tokenizer import SHA256Tokenizer
from opentoken.tokens.token_generation_exception import TokenGenerationException
from opentoken.tokens.token_generator import TokenGenerator
from opentoken.tokens.token_generator_result import TokenGeneratorResult
from opentoken.tokentransformer.token_transformer import TokenTransformer


class TestTokenGenerator:
    """Test cases for TokenGenerator."""

    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.tokenizer = Mock(spec=SHA256Tokenizer)
        self.token_transformer_list = []
        self.token_definition = Mock(spec=BaseTokenDefinition)

        # Mock the AttributeLoader to avoid dependencies
        with patch('opentoken.tokens.token_generator.AttributeLoader') as mock_loader:
            mock_loader.load.return_value = {
                FirstNameAttribute(), 
                LastNameAttribute()
            }
            self.token_generator = TokenGenerator(self.token_definition, self.token_transformer_list)
        
        # Inject mock tokenizer
        self.token_generator.tokenizer = self.tokenizer

    def test_get_all_tokens_valid_tokens_with_expressions(self):
        """Test generating all tokens with valid tokens and expressions."""
        self.token_definition.get_token_identifiers.return_value = {"token1", "token2"}

        attr_expr1 = AttributeExpression(FirstNameAttribute, "U")
        attr_expr2 = AttributeExpression(LastNameAttribute, "R('MacDonald','Donald')")

        attribute_expressions1 = [attr_expr1]
        attribute_expressions2 = [attr_expr2]

        self.token_definition.get_token_definition.side_effect = lambda token_id: {
            "token1": attribute_expressions1,
            "token2": attribute_expressions2
        }[token_id]

        person_attributes = {
            FirstNameAttribute: "John",
            LastNameAttribute: "Old MacDonald"
        }

        self.tokenizer.tokenize.return_value = "hashedToken"

        tokens = self.token_generator.get_all_tokens(person_attributes).tokens

        assert tokens is not None
        assert len(tokens) == 2
        assert tokens.get("token1") == "hashedToken"
        assert tokens.get("token2") == "hashedToken"

    def test_get_all_tokens_invalid_attribute_returns_empty_token(self):
        """Test that invalid attributes return empty tokens."""
        self.token_definition.get_token_identifiers.return_value = {"token1"}

        self.token_generator.tokenizer = SHA256Tokenizer(self.token_transformer_list)

        attr_expr = AttributeExpression(FirstNameAttribute, "U")
        attribute_expressions = [attr_expr]
        self.token_definition.get_token_definition.return_value = attribute_expressions

        # Person attributes (invalid case with missing name)
        person_attributes = {
            LastNameAttribute: "MacDonald"
        }

        tokens = self.token_generator.get_all_tokens(person_attributes).tokens

        # Validate that 1 token was generated
        assert len(tokens) == 1, "Expected one token to be generated due to validation failure"
        assert "token1" in tokens, "Expected token1 to be present in generated tokens"
        assert tokens["token1"] == SHA256Tokenizer.EMPTY, "Expected empty token for invalid attribute"

    def test_get_all_tokens_error_in_token_generation_logs_error(self):
        """Test that errors in token generation are logged and handled gracefully."""
        self.token_definition.get_token_identifiers.return_value = {"token1"}

        attr_expr = AttributeExpression(FirstNameAttribute, "U")
        attribute_expressions = [attr_expr]
        self.token_definition.get_token_definition.return_value = attribute_expressions

        person_attributes = {
            FirstNameAttribute: "John"
        }

        # Simulate error during tokenization
        self.tokenizer.tokenize.side_effect = RuntimeError("Tokenization error")

        tokens = self.token_generator.get_all_tokens(person_attributes).tokens

        # Validate that no tokens are generated due to tokenization error
        assert len(tokens) == 0, "Expected no tokens to be generated due to tokenization error"

    def test_get_token_signature_valid_signature(self):
        """Test getting a valid token signature."""
        attr_expr1 = AttributeExpression(FirstNameAttribute, "U")
        attr_expr2 = AttributeExpression(LastNameAttribute, "U")

        attribute_expressions = [attr_expr1, attr_expr2]
        self.token_definition.get_token_definition.return_value = attribute_expressions

        person_attributes = {
            FirstNameAttribute: "John",
            LastNameAttribute: "Smith"
        }

        signature = self.token_generator._get_token_signature("token1", person_attributes, TokenGeneratorResult())

        assert signature is not None
        assert signature == "JOHN|SMITH"

    def test_get_token_signature_null_person_attributes(self):
        """Test that null person attributes raise an exception."""
        with pytest.raises(ValueError):
            self.token_generator._get_token_signature("token1", None, TokenGeneratorResult())

    def test_get_token_signature_missing_required_attribute(self):
        """Test that missing required attributes return None."""
        attr_expr = AttributeExpression(FirstNameAttribute, "U")
        attribute_expressions = [attr_expr]
        self.token_definition.get_token_definition.return_value = attribute_expressions

        person_attributes = {
            LastNameAttribute: "Smith"
        }

        signature = self.token_generator._get_token_signature("token1", person_attributes, TokenGeneratorResult())

        assert signature is None

    def test_get_token_signature_invalid_attribute_value(self):
        """Test that invalid attribute values return None."""
        attr_expr = AttributeExpression(FirstNameAttribute, "U")
        attribute_expressions = [attr_expr]
        self.token_definition.get_token_definition.return_value = attribute_expressions

        person_attributes = {
            FirstNameAttribute: ""  # Invalid empty name
        }

        signature = self.token_generator._get_token_signature("token1", person_attributes, TokenGeneratorResult())

        assert signature is None

    def test_get_token_valid_input_returns_hashed_token(self):
        """Test that valid input returns a hashed token."""
        attr_expr = AttributeExpression(FirstNameAttribute, "U")
        attribute_expressions = [attr_expr]

        self.token_definition.get_token_definition.return_value = attribute_expressions
        self.tokenizer.tokenize.return_value = "hashedToken123"

        person_attributes = {
            FirstNameAttribute: "John"
        }

        token = self.token_generator._get_token("token1", person_attributes, TokenGeneratorResult())

        assert token is not None
        assert token == "hashedToken123"

    def test_get_token_null_signature_returns_none(self):
        """Test that null signature returns None."""
        self.token_generator.tokenizer = SHA256Tokenizer(self.token_transformer_list)

        attr_expr = AttributeExpression(FirstNameAttribute, "U")
        attribute_expressions = [attr_expr]

        self.token_definition.get_token_definition.return_value = attribute_expressions

        person_attributes = {
            # Missing required attribute leads to null signature
            LastNameAttribute: "Smith"
        }

        token = self.token_generator._get_token("token1", person_attributes, TokenGeneratorResult())

        assert token == SHA256Tokenizer.EMPTY, "Expected empty token for null signature"

    def test_get_token_tokenization_error_throws_exception(self):
        """Test that tokenization errors throw TokenGenerationException."""
        attr_expr = AttributeExpression(FirstNameAttribute, "U")
        attribute_expressions = [attr_expr]

        self.token_definition.get_token_definition.return_value = attribute_expressions
        self.tokenizer.tokenize.side_effect = RuntimeError("Tokenization failed")

        person_attributes = {
            FirstNameAttribute: "John"
        }

        with pytest.raises(TokenGenerationException):
            self.token_generator._get_token("token1", person_attributes, TokenGeneratorResult())