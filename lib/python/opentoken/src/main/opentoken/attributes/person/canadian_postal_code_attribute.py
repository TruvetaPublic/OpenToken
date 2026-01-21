"""
Copyright (c) Truveta. All rights reserved.
"""

import re
from typing import List
from opentoken.attributes.base_attribute import BaseAttribute
from opentoken.attributes.utilities.attribute_utilities import AttributeUtilities
from opentoken.attributes.validation.not_starts_with_validator import NotStartsWithValidator
from opentoken.attributes.validation.regex_validator import RegexValidator


class CanadianPostalCodeAttribute(BaseAttribute):
    """
    Represents Canadian postal codes.

    This class handles validation and normalization of Canadian postal codes,
    supporting the A1A 1A1 format (letter-digit-letter space digit-letter-digit).
    """

    NAME = "CanadianPostalCode"
    ALIASES = [NAME, "CanadianZipCode"]

    # Regular expression pattern for validating Canadian postal codes
    # Supports 3-character (ZIP-3), partial (4-5 char), and full 6-character formats
    CANADIAN_POSTAL_REGEX = r"^\s*[A-Za-z]\d[A-Za-z](\s?\d([A-Za-z]\d?)?)?\s*$"

    INVALID_ZIP_CODES = {
        # 6-character Canadian postal code placeholders
        "A1A 1A1",
        "X0X 0X0",
        "Y0Y 0Y0",
        "Z0Z 0Z0",
        "A0A 0A0",
        "B1B 1B1",
        "C2C 2C2",
        # 3-character invalid codes (ZIP-3 prefixes that should be invalidated)
        # Note: "K1A" invalidates "K1A 0A6" and all codes starting with "K1A"
        # Note: "H0H" invalidates "H0H 0H0" and all codes starting with "H0H"
        "K1A",  # Canadian government
        "M7A",  # Government of Ontario
        "H0H"   # Santa Claus
    }

    def __init__(self, min_length: int = 6):
        """
        Initialize CanadianPostalCodeAttribute.
        
        Args:
            min_length: Minimum length for postal codes (default: 6)
        """
        validation_rules = []
        super().__init__(validation_rules)
        self.min_length = min_length
        self.regex_validator = RegexValidator(self.CANADIAN_POSTAL_REGEX)
        self.not_starts_with_validator = NotStartsWithValidator(self.INVALID_ZIP_CODES)

    def validate(self, value: str) -> bool:
        """
        Validate the Canadian postal code value.

        Args:
            value: The postal code value to validate

        Returns:
            True if the value is a valid Canadian postal code, False otherwise
        """
        if value is None:
            return False

        # First, check the regex pattern on the ORIGINAL value
        # This ensures the format is valid before normalization
        if not self.regex_validator.eval(value):
            return False

        # Normalize the value to ensure idempotency
        # This converts to uppercase and formats consistently
        normalized_value = self.normalize(value)

        # Validate the NORMALIZED value against the prefix validator
        # This ensures "k1a0b1" and "K1A 0B1" are treated consistently
        if not self.not_starts_with_validator.eval(normalized_value):
            return False

        return True

    def get_name(self) -> str:
        return self.NAME

    def get_aliases(self) -> List[str]:
        return self.ALIASES.copy()

    def normalize(self, value: str) -> str:
        """
        Normalize a Canadian postal code to standard A1A 1A1 format.

        For Canadian postal codes:
        - Codes shorter than min_length are rejected (return original)
        - 3-character format (e.g., "J1X") is padded with " 000" to create full format (e.g., "J1X 000") if min_length <= 3
        - 4-character format (e.g., "J1X1") is padded with "A0" to create full format (e.g., "J1X 1A0") if min_length <= 4
        - 5-character format (e.g., "J1X1A") is padded with "0" to create full format (e.g., "J1X 1A0") if min_length <= 5
        - 6-character format returns uppercase format with space (e.g., "k1a0a6" becomes "K1A 0A6")
        If the input value is null or doesn't match Canadian postal pattern, the original
        trimmed value is returned.
        """
        if not value:
            return value

        trimmed = AttributeUtilities.remove_whitespace(value.strip())

        # Check if it's a 3-character Canadian postal code (ZIP-3) - pad with " 000" if min_length allows
        if re.match(r"^[A-Za-z]\d[A-Za-z]$", trimmed):
            if self.min_length <= 3:
                upper = trimmed.upper()
                return f"{upper} 000"
            # If min_length > 3, reject this by returning original
            return value.strip()

        # Check if it's a 4-character partial postal code (e.g., "A1A1") - pad with "A0" if min_length allows
        if re.match(r"^[A-Za-z]\d[A-Za-z]\d$", trimmed):
            if self.min_length <= 4:
                upper = trimmed.upper()
                return f"{upper[:3]} {upper[3]}A0"
            # If min_length > 4, reject this by returning original
            return value.strip()

        # Check if it's a 5-character partial postal code (e.g., "A1A1A") - pad with "0" if min_length allows
        if re.match(r"^[A-Za-z]\d[A-Za-z]\d[A-Za-z]$", trimmed):
            if self.min_length <= 5:
                upper = trimmed.upper()
                return f"{upper[:3]} {upper[3:]}0"
            # If min_length > 5, reject this by returning original
            return value.strip()

        # Check if it's a Canadian postal code (6 alphanumeric characters)
        if re.match(r"[A-Za-z]\d[A-Za-z]\d[A-Za-z]\d", trimmed):
            upper = trimmed.upper()
            return f"{upper[:3]} {upper[3:6]}"

        # For values that don't match Canadian postal patterns, return trimmed original
        return value.strip()
