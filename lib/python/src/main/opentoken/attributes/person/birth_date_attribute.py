# src/opentoken/attributes/person/birth_date_attribute.py
from typing import List
import re
from datetime import datetime
from opentoken.attributes.base_attribute import BaseAttribute
from opentoken.attributes.validation import BirthDateRangeValidator
from opentoken.attributes.validation import RegexValidator

class BirthDateAttribute(BaseAttribute):
    """Represents the birth date attribute.
    
    This class extends BaseAttribute and provides functionality for working with
    birth date fields. It recognizes "BirthDate" as a valid alias for this
    attribute type.
    
    The attribute performs normalization on input values, converting them to a
    standard format (yyyy-MM-dd).
    
    Supported formats:
    - yyyy-MM-dd
    - yyyy/MM/dd  
    - MM/dd/yyyy
    - MM-dd-yyyy
    - dd.MM.yyyy
    """
    
    NAME = "BirthDate"
    ALIASES = [NAME]
    
    # Regular expression pattern for validating birth date formats
    VALIDATION_PATTERN = re.compile(
        r"^(?:\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-./]\d{2}[-./]\d{4})$"
    )
    
    def __init__(self):
        validation_rules = [
            RegexValidator(self.VALIDATION_PATTERN),
            BirthDateRangeValidator()
        ]
        super().__init__(validation_rules)
    
    def get_name(self) -> str:
        return self.NAME
    
    def get_aliases(self) -> List[str]:
        return self.ALIASES.copy()
    
    def normalize(self, value: str) -> str:
        """Normalizes birth date to yyyy-MM-dd format."""
        
        value = value.strip()
                
        if not value:
            raise ValueError(f"Invalid date format: empty or whitespace")
        
        # Try different date formats
        date_formats = [
            "%Y-%m-%d", "%Y/%m/%d",  # yyyy-MM-dd, yyyy/MM/dd
            "%m/%d/%Y", "%m-%d-%Y",  # MM/dd/yyyy, MM-dd-yyyy  
            "%d.%m.%Y"               # dd.MM.yyyy
        ]
        
        for format in date_formats:
            try:
                parsed_date = datetime.strptime(value, format)
                return parsed_date.strftime("%Y-%m-%d")
            except ValueError:
                continue
        
        raise ValueError(f"Invalid date format: {value}")