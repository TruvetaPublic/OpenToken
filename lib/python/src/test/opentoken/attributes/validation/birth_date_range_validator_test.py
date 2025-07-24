"""
Tests for BirthDateRangeValidator.
"""

import pickle
from datetime import datetime, timedelta

import pytest

from opentoken.attributes.validation.birth_date_range_validator import BirthDateRangeValidator


class TestBirthDateRangeValidator:
    """Test cases for BirthDateRangeValidator."""

    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.validator = BirthDateRangeValidator()

    def test_eval_valid_dates_within_range_should_return_true(self):
        """Test valid dates within the acceptable range."""
        # Test boundary dates that should be valid
        assert self.validator.eval("1910-01-01") is True  # Minimum valid date
        assert self.validator.eval("01/01/1910") is True  # Same date, different format

        # Test today's date (should be valid)
        today = datetime.now().date()
        assert self.validator.eval(today.strftime("%Y-%m-%d")) is True

        # Test dates within acceptable range
        assert self.validator.eval("1950-06-15") is True
        assert self.validator.eval("12/25/1990") is True
        assert self.validator.eval("29.02.2000") is True  # Leap year
        assert self.validator.eval("2000-12-31") is True
        assert self.validator.eval("01-01-2020") is True

    def test_eval_dates_before_minimum_should_return_false(self):
        """Test dates before the minimum valid date."""
        assert self.validator.eval("1909-12-31") is False
        assert self.validator.eval("12/31/1909") is False
        assert self.validator.eval("1900-01-01") is False
        assert self.validator.eval("01/01/1900") is False
        assert self.validator.eval("31.12.1909") is False
        assert self.validator.eval("1850-06-15") is False

    def test_eval_dates_after_today_should_return_false(self):
        """Test dates after today's date."""
        tomorrow = datetime.now().date() + timedelta(days=1)
        next_year = datetime.now().date() + timedelta(days=365)

        assert self.validator.eval(tomorrow.strftime("%Y-%m-%d")) is False
        assert self.validator.eval(next_year.strftime("%Y-%m-%d")) is False
        assert self.validator.eval("2030-01-01") is False
        assert self.validator.eval("01/01/2030") is False
        assert self.validator.eval("2050-12-25") is False

    def test_eval_invalid_date_formats_should_return_false(self):
        """Test invalid date formats."""
        assert self.validator.eval("20231026") is False  # No separators
        assert self.validator.eval("2023-13-01") is False  # Invalid month
        assert self.validator.eval("2023-02-30") is False  # Invalid day for February
        assert self.validator.eval("invalid-date") is False
        assert self.validator.eval("2023/15/45") is False  # Invalid month and day
        assert self.validator.eval("abc-def-ghi") is False

    def test_eval_null_and_empty_values_should_return_false(self):
        """Test null and empty values."""
        assert self.validator.eval(None) is False
        assert self.validator.eval("") is False
        assert self.validator.eval("   ") is False  # Whitespace only
        assert self.validator.eval("\t\n") is False

    def test_eval_various_date_formats_should_work_correctly(self):
        """Test all supported formats for the same date."""
        assert self.validator.eval("1995-07-15") is True  # yyyy-MM-dd
        assert self.validator.eval("1995/07/15") is True  # yyyy/MM/dd
        assert self.validator.eval("07/15/1995") is True  # MM/dd/yyyy
        assert self.validator.eval("07-15-1995") is True  # MM-dd-yyyy
        assert self.validator.eval("15.07.1995") is True  # dd.MM.yyyy

    def test_eval_leap_year_dates_should_work_correctly(self):
        """Test leap year dates."""
        # Valid leap year dates
        assert self.validator.eval("2000-02-29") is True  # Year 2000 is a leap year
        assert self.validator.eval("1996-02-29") is True  # 1996 is a leap year

        # Invalid leap year dates
        assert self.validator.eval("1900-02-29") is False  # 1900 is not a leap year
        assert self.validator.eval("2001-02-29") is False  # 2001 is not a leap year

    def test_serialization_should_preserve_validation_behavior(self):
        """Test serialization and deserialization of the validator."""
        # Serialize the validator
        serialized_data = pickle.dumps(self.validator)

        # Deserialize the validator
        deserialized_validator = pickle.loads(serialized_data)

        # Test that both validators behave identically
        test_values = [
            "1910-01-01",  # Valid boundary
            "1909-12-31",  # Invalid (too old)
            "2030-01-01",  # Invalid (future)
            "1990-06-15",  # Valid middle range
            "invalid-date",  # Invalid format
            None  # Null value
        ]

        for value in test_values:
            original_result = self.validator.eval(value)
            deserialized_result = deserialized_validator.eval(value)

            assert original_result == deserialized_result, (
                f"Validation results should match for value: {value} "
                f"(original: {original_result}, deserialized: {deserialized_result})"
            )

    def test_eval_edge_case_dates_should_work_correctly(self):
        """Test edge cases around boundaries."""
        today = datetime.now().date()
        yesterday = today - timedelta(days=1)
        min_date = datetime(1910, 1, 1).date()
        day_before_min_date = min_date - timedelta(days=1)

        # Test edge cases around boundaries
        assert self.validator.eval(today.strftime("%Y-%m-%d")) is True  # Today should be valid
        assert self.validator.eval(yesterday.strftime("%Y-%m-%d")) is True  # Yesterday should be valid
        assert self.validator.eval(min_date.strftime("%Y-%m-%d")) is True  # Minimum date should be valid
        assert self.validator.eval(day_before_min_date.strftime("%Y-%m-%d")) is False  # Day before minimum should be invalid