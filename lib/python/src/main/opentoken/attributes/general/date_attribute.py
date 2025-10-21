"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import List
import re
from datetime import datetime
from opentoken.attributes.base_attribute import BaseAttribute
from opentoken.attributes.validation import RegexValidator
from opentoken.attributes.validation.serializable_attribute_validator import SerializableAttributeValidator


class DateAttribute(BaseAttribute):
    """Represents a generic date attribute.

    This class extends BaseAttribute and provides functionality for working with
    date fields. It recognizes "Date" as a valid alias for this attribute type.

    The attribute performs normalization on input values, converting them to a
    standard format (yyyy-MM-dd).

    Supported formats:
    - yyyy-MM-dd
    - yyyy/MM/dd
    - MM/dd/yyyy
    - MM-dd-yyyy
    - dd.MM.yyyy
    """

    NAME = "Date"
    ALIASES = [NAME]

    # Regular expression pattern for validating date formats
    VALIDATION_PATTERN = re.compile(
        r"^(?:\d{4}[-/]\d{2}[-/]\d{2}|\d{2}[-./]\d{2}[-./]\d{4})$"
    )

    def __init__(self, additional_validators: List[SerializableAttributeValidator] = None):
        """
        Initialize the DateAttribute with optional additional validators.

        Args:
            additional_validators: Optional list of additional validators to apply
        """
        validation_rules = [RegexValidator(self.VALIDATION_PATTERN)]
        if additional_validators:
            validation_rules.extend(additional_validators)
        super().__init__(validation_rules)

    def get_name(self) -> str:
        """Get the name of the attribute.

        Returns:
            str: The name "Date"
        """
        return self.NAME

    def get_aliases(self) -> List[str]:
        """Get the aliases for the attribute.

        Returns:
            List[str]: A list containing the aliases for this attribute
        """
        return self.ALIASES.copy()

    def normalize(self, value: str) -> str:
        """Normalizes date to yyyy-MM-dd format.

        Args:
            value: The date string to normalize

        Returns:
            str: The normalized date in yyyy-MM-dd format

        Raises:
            ValueError: If the date format is invalid
        """
        value = value.strip()

        if not value:
            raise ValueError("Invalid date format: empty or whitespace")

        # Try different date formats
        date_formats = [
            "%Y-%m-%d",  # yyyy-MM-dd
            "%Y/%m/%d",  # yyyy/MM/dd
            "%m/%d/%Y",  # MM/dd/yyyy
            "%m-%d-%Y",  # MM-dd-yyyy
            "%d.%m.%Y"   # dd.MM.yyyy
        ]

        for format in date_formats:
            try:
                parsed_date = datetime.strptime(value, format)
                return parsed_date.strftime("%Y-%m-%d")
            except ValueError:
                continue

        raise ValueError(f"Invalid date format: {value}")
