"""
Copyright (c) Truveta. All rights reserved.
"""

from datetime import datetime, date
from typing import List
from opentoken.attributes.validation.serializable_attribute_validator import SerializableAttributeValidator


class BirthDateRangeValidator(SerializableAttributeValidator):
    """
    A Validator that asserts that the birth date value is within
    an acceptable range (between 1910/1/1 and today).
    
    This validator checks that birth dates are:
    - Not before January 1, 1910
    - Not after today's date
    
    If the date is outside this range, the validation fails.
    """
    
    # Supported date formats for parsing
    POSSIBLE_INPUT_FORMATS = [
        "%Y-%m-%d", "%Y/%m/%d", "%m/%d/%Y",
        "%m-%d-%Y", "%d.%m.%Y"
    ]
    
    def __init__(self):
        """Initialize the validator with default date range."""
        self._min_date = date(1910, 1, 1)
        self._max_date = date.today()
    
    @property
    def min_date(self) -> date:
        """Get the minimum allowed birth date."""
        return self._min_date
    
    @property 
    def max_date(self) -> date:
        """Get the maximum allowed birth date (today)."""
        return self._max_date
    
    def eval(self, value: str) -> bool:
        """
        Validate that the birth date value is within acceptable range.
        
        Args:
            value: The birth date string to validate
            
        Returns:
            True if the date is within acceptable range (1910/1/1 to today),
            False otherwise
        """
        if not value or not value.strip():
            return False
        
        parsed_date = self._parse_date(value.strip())
        if parsed_date is None:
            return False
        
        # Check if date is not before 1910/1/1 and not after today
        return self._min_date <= parsed_date <= self._max_date
    
    def _parse_date(self, value: str) -> date:
        """
        Parse the date string using various supported formats.
        
        Args:
            value: The date string to parse
            
        Returns:
            Parsed date object, or None if parsing fails
        """
        for date_format in self.POSSIBLE_INPUT_FORMATS:
            try:
                parsed_datetime = datetime.strptime(value, date_format)
                return parsed_datetime.date()
            except ValueError:
                continue
        
        return None  # If no format matched