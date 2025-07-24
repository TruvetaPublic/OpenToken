# src/test/opentoken/attributes/person/test_canadian_postal_code_attribute.py
"""
Copyright (c) Truveta. All rights reserved.
"""

import pickle
import pytest
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed

from opentoken.attributes.person.canadian_postal_code_attribute import CanadianPostalCodeAttribute


class TestCanadianPostalCodeAttribute:
    """Test cases for CanadianPostalCodeAttribute class."""
    
    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.canadian_postal_code_attribute = CanadianPostalCodeAttribute()
    
    def test_get_name_should_return_canadian_postal_code(self):
        """Test that get_name returns 'CanadianPostalCode'."""
        assert self.canadian_postal_code_attribute.get_name() == "CanadianPostalCode"
    
    def test_get_aliases_should_return_canadian_zip_code_aliases(self):
        """Test that get_aliases returns CanadianZipCode aliases."""
        expected_aliases = ["CanadianPostalCode", "CanadianZipCode"]
        assert self.canadian_postal_code_attribute.get_aliases() == expected_aliases
    
    def test_normalize_should_handle_canadian_postal_codes(self):
        """Test normalization of various Canadian postal code formats."""
        assert self.canadian_postal_code_attribute.normalize("K1A0A6") == "K1A 0A6"
        assert self.canadian_postal_code_attribute.normalize("k1a0a6") == "K1A 0A6"
        assert self.canadian_postal_code_attribute.normalize("K1A 0A6") == "K1A 0A6"
        assert self.canadian_postal_code_attribute.normalize("m5v3l9") == "M5V 3L9"
        assert self.canadian_postal_code_attribute.normalize("H3Z2Y7") == "H3Z 2Y7"
        assert self.canadian_postal_code_attribute.normalize("t2x1v4") == "T2X 1V4"
        assert self.canadian_postal_code_attribute.normalize("v6b1a1") == "V6B 1A1"
        assert self.canadian_postal_code_attribute.normalize("N2L3G1") == "N2L 3G1"
    
    def test_validate_should_return_true_for_valid_canadian_postal_codes(self):
        """Test validation of valid Canadian postal codes."""
        assert self.canadian_postal_code_attribute.validate("K1A 0A7") is True
        assert self.canadian_postal_code_attribute.validate("K1A0A7") is True
        assert self.canadian_postal_code_attribute.validate("k1a 0a7") is True
        assert self.canadian_postal_code_attribute.validate("k1a0a7") is True
        assert self.canadian_postal_code_attribute.validate("M5V 3L9") is True
        assert self.canadian_postal_code_attribute.validate("H3Z 2Y7") is True
        assert self.canadian_postal_code_attribute.validate("T2X 1V4") is True
        assert self.canadian_postal_code_attribute.validate(" K1A 0A7 ") is True
        assert self.canadian_postal_code_attribute.validate("  K1A0A7  ") is True
        assert self.canadian_postal_code_attribute.validate("V6B 1A1") is True
        assert self.canadian_postal_code_attribute.validate("N2L 3G1") is True
    
    def test_validate_should_return_false_for_invalid_canadian_postal_codes(self):
        """Test validation of invalid Canadian postal codes."""
        # Null and empty values
        assert self.canadian_postal_code_attribute.validate(None) is False, \
               "Null value should not be allowed"
        assert self.canadian_postal_code_attribute.validate("") is False, \
               "Empty value should not be allowed"
        
        # Invalid Canadian postal code formats
        assert self.canadian_postal_code_attribute.validate("K1A") is False, \
               "Incomplete Canadian postal code should not be allowed"
        assert self.canadian_postal_code_attribute.validate("K1A 0A") is False, \
               "Incomplete Canadian postal code should not be allowed"
        assert self.canadian_postal_code_attribute.validate("K1A 0A67") is False, \
               "Too long Canadian postal code should not be allowed"
        assert self.canadian_postal_code_attribute.validate("K11 0A6") is False, \
               "Invalid Canadian postal code format should not be allowed"
        assert self.canadian_postal_code_attribute.validate("KAA 0A6") is False, \
               "Invalid Canadian postal code format should not be allowed"
        assert self.canadian_postal_code_attribute.validate("K1A 0AA") is False, \
               "Invalid Canadian postal code format should not be allowed"
        
        # Invalid placeholder values
        assert self.canadian_postal_code_attribute.validate("A1A 1A1") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("K1A 0A6") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("H0H 0H0") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("X0X 0X0") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("Y0Y 0Y0") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("Z0Z 0Z0") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("A0A 0A0") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("B1B 1B1") is False, \
               "Invalid placeholder should not be allowed"
        assert self.canadian_postal_code_attribute.validate("C2C 2C2") is False, \
               "Invalid placeholder should not be allowed"
        
        # US ZIP codes should not validate
        assert self.canadian_postal_code_attribute.validate("12345") is False, \
               "US ZIP code should not validate"
        assert self.canadian_postal_code_attribute.validate("12345-6789") is False, \
               "US ZIP code should not validate"
    
    def test_normalize_should_handle_whitespace(self):
        """Test different types of whitespace handling for Canadian postal codes."""
        assert self.canadian_postal_code_attribute.normalize("K1A0A7") == "K1A 0A7", "No space"
        assert self.canadian_postal_code_attribute.normalize(" K1A0A7") == "K1A 0A7", "Leading space"
        assert self.canadian_postal_code_attribute.normalize("K1A0A7 ") == "K1A 0A7", "Trailing space"
        assert self.canadian_postal_code_attribute.normalize(" K1A 0A7 ") == "K1A 0A7", \
               "Leading and trailing spaces"
        assert self.canadian_postal_code_attribute.normalize("K1A\t0A7") == "K1A 0A7", "Tab character"
        assert self.canadian_postal_code_attribute.normalize("K1A\n0A7") == "K1A 0A7", "Newline character"
        assert self.canadian_postal_code_attribute.normalize("K1A\r\n0A7") == "K1A 0A7", \
               "Carriage return and newline"
        assert self.canadian_postal_code_attribute.normalize("  K1A   0A7  ") == "K1A 0A7", \
               "Multiple spaces"
    
    def test_normalize_thread_safety(self):
        """Test thread safety of normalize method."""
        thread_count = 100
        test_postal_code = "k1a0a7"
        expected_result = "K1A 0A7"
        results = []
        
        def normalize_postal_code():
            """Function to be executed by each thread."""
            try:
                result = self.canadian_postal_code_attribute.normalize(test_postal_code)
                return result
            except Exception as e:
                pytest.fail(f"Thread failed with exception: {e}")
        
        # Use ThreadPoolExecutor for better thread management
        with ThreadPoolExecutor(max_workers=thread_count) as executor:
            # Submit all tasks
            futures = [executor.submit(normalize_postal_code) for _ in range(thread_count)]
            
            # Collect results
            for future in as_completed(futures, timeout=15):
                try:
                    result = future.result()
                    results.append(result)
                except Exception as e:
                    pytest.fail(f"Thread execution failed: {e}")
        
        # Verify all threads got the same result
        assert len(results) == thread_count, f"Expected {thread_count} results, got {len(results)}"
        
        for i, result in enumerate(results):
            assert result == expected_result, f"Thread {i} got unexpected result: {result}"
    
    def test_normalize_should_handle_edge_cases(self):
        """Test edge cases for normalization."""
        # Test null and empty values
        assert self.canadian_postal_code_attribute.normalize(None) is None
        assert self.canadian_postal_code_attribute.normalize("") == ""
        
        # Test non-Canadian formats - should return trimmed original
        assert self.canadian_postal_code_attribute.normalize("12345") == "12345"
        assert self.canadian_postal_code_attribute.normalize("12345-6789") == "12345-6789"
        assert self.canadian_postal_code_attribute.normalize("1234 ") == "1234"
        assert self.canadian_postal_code_attribute.normalize("invalid") == "invalid"
    
    def test_serialization(self):
        """Test serialization and deserialization of CanadianPostalCodeAttribute."""
        # Serialize the attribute using pickle
        serialized_data = pickle.dumps(self.canadian_postal_code_attribute)
        
        # Deserialize the attribute
        deserialized_attribute = pickle.loads(serialized_data)
        
        # Test various Canadian postal code values with both original and deserialized attributes
        test_values = [
            "K1A 0A7",
            "k1a0a7",
            "M5V 3L9",
            "H3Z2Y7",
            "T2X 1V4",
            "V6B 1A1"
        ]
        
        for value in test_values:
            # Test that attribute names match
            assert (self.canadian_postal_code_attribute.get_name() == 
                   deserialized_attribute.get_name()), "Attribute names should match"
            
            # Test that attribute aliases match
            assert (self.canadian_postal_code_attribute.get_aliases() == 
                   deserialized_attribute.get_aliases()), "Attribute aliases should match"
            
            # Test that normalization is identical
            original_normalized = self.canadian_postal_code_attribute.normalize(value)
            deserialized_normalized = deserialized_attribute.normalize(value)
            assert (original_normalized == deserialized_normalized), \
                   f"Normalization should be identical for value: {value}"
            
            # Test that validation is identical
            original_valid = self.canadian_postal_code_attribute.validate(value)
            deserialized_valid = deserialized_attribute.validate(value)
            assert (original_valid == deserialized_valid), \
                   f"Validation should be identical for value: {value}"
    
    @pytest.mark.parametrize("input_code,expected_output", [
        ("K1A0A6", "K1A 0A6"),
        ("k1a0a6", "K1A 0A6"),
        ("K1A 0A6", "K1A 0A6"),
        ("m5v3l9", "M5V 3L9"),
        ("H3Z2Y7", "H3Z 2Y7"),
        ("t2x1v4", "T2X 1V4"),
        ("v6b1a1", "V6B 1A1"),
        ("N2L3G1", "N2L 3G1"),
        ("  K1A0A7  ", "K1A 0A7"),
        ("k1a\t0a7", "K1A 0A7"),
    ])
    def test_normalize_parametrized(self, input_code, expected_output):
        """Parametrized test for normalization with various Canadian postal codes."""
        assert self.canadian_postal_code_attribute.normalize(input_code) == expected_output
    
    @pytest.mark.parametrize("valid_code", [
        "K1A 0A7",
        "K1A0A7",
        "k1a 0a7",
        "k1a0a7",
        "M5V 3L9",
        "H3Z 2Y7",
        "T2X 1V4",
        " K1A 0A7 ",
        "  K1A0A7  ",
        "V6B 1A1",
        "N2L 3G1",
    ])
    def test_validate_valid_codes_parametrized(self, valid_code):
        """Parametrized test for validation with valid Canadian postal codes."""
        assert self.canadian_postal_code_attribute.validate(valid_code) is True
    
    @pytest.mark.parametrize("invalid_code", [
        None,
        "",
        "K1A",
        "K1A 0A",
        "K1A 0A67",
        "K11 0A6",
        "KAA 0A6",
        "K1A 0AA",
        "A1A 1A1",  # Placeholder
        "K1A 0A6",  # Placeholder
        "H0H 0H0",  # Placeholder
        "12345",    # US ZIP
        "12345-6789",  # US ZIP+4
    ])
    def test_validate_invalid_codes_parametrized(self, invalid_code):
        """Parametrized test for validation with invalid Canadian postal codes."""
        assert self.canadian_postal_code_attribute.validate(invalid_code) is False