"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import List
import re
from opentoken.attributes.base_attribute import BaseAttribute
from opentoken.attributes.validation import RegexValidator
from opentoken.attributes.validation.age_range_validator import AgeRangeValidator


class AgeAttribute(BaseAttribute):
    """Represents the age attribute.

    This class extends BaseAttribute and provides functionality for working with
    age fields. It recognizes "Age" as a valid alias for this attribute type.

    The attribute performs normalization on input values by trimming whitespace
    and validates that the age is a valid integer between 0 and 120.
    """

    NAME = "Age"
    ALIASES = [NAME]

    # Regular expression pattern for validating integer format with optional whitespace
    VALIDATION_PATTERN = re.compile(r"^\s*\d+\s*$")

    def __init__(self):
        validation_rules = [
            RegexValidator(self.VALIDATION_PATTERN),
            AgeRangeValidator()
        ]
        super().__init__(validation_rules)

    def get_name(self) -> str:
        """Get the name of the attribute.

        Returns:
            str: The name "Age"
        """
        return self.NAME

    def get_aliases(self) -> List[str]:
        """Get the aliases for the attribute.

        Returns:
            List[str]: A list containing the aliases for this attribute
        """
        return self.ALIASES.copy()

    def normalize(self, value: str) -> str:
        """Normalize the age value by trimming whitespace.

        Args:
            value: The age string to normalize

        Returns:
            str: The trimmed age value

        Raises:
            ValueError: If the age is not a valid integer
        """
        trimmed = value.strip()
        
        # Validate it's a valid integer
        try:
            int(trimmed)
            return trimmed
        except ValueError:
            raise ValueError(f"Invalid age format: {value}")
