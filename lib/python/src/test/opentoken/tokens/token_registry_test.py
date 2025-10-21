"""
Copyright (c) Truveta. All rights reserved.
"""
import pytest
from opentoken.tokens.token_registry import TokenRegistry

def test_load_all_tokens_returns_non_empty_dict():
    tokens = TokenRegistry.load_all_tokens()
    assert tokens, "Tokens dict should not be empty"

    expected_tokens = ["T1", "T2", "T3", "T4", "T5"]
    for token_id in expected_tokens:
        assert token_id in tokens, f"Tokens dict should contain {token_id}"
        definitions = tokens[token_id]
        assert definitions is not None, f"Definitions for {token_id} should not be None"
        assert definitions, f"Definitions for {token_id} should not be empty"
