"""
Copyright (c) Truveta. All rights reserved.
"""

import re
from typing import List
from opentoken.attributes.base_attribute import BaseAttribute
from opentoken.attributes.utilities.attribute_utilities import AttributeUtilities
from opentoken.attributes.validation.not_starts_with_validator import NotStartsWithValidator
from opentoken.attributes.validation.regex_validator import RegexValidator


class USPostalCodeAttribute(BaseAttribute):
    """
    Represents US postal codes (ZIP codes).

    This class handles validation and normalization of US ZIP codes, supporting
    both 5-digit format (12345) and 5+4 format (12345-6789).
    """

    NAME = "USPostalCode"
    ALIASES = [NAME, "USZipCode"]

    # Regular expression pattern for validating US postal (ZIP) codes
    # Supports 3-digit (ZIP-3), 5-digit, and 9-digit formats
    US_ZIP_REGEX = r"^\s*(\d{3}|\d{5}(-\d{4})?|\d{9})\s*$"

    INVALID_ZIP_CODES = {
        # 5-digit invalid codes
        "11111",
        "22222",
        "33333",
        "66666",
        "77777",
        "99999",
        # Commonly used placeholders
        "01234",
        "12345",
        "54321",
        "98765",
        # 3-digit invalid codes (ZIP-3 prefixes that should be invalidated)
        # Note: "000" invalidates "00000" and all codes starting with "000"
        # Note: "555" invalidates "55555" and all codes starting with "555"
        # Note: "888" invalidates "88888" and all codes starting with "888"
        "000",
        "555",
        "888"
    }

    def __init__(self):
        validation_rules = [
            RegexValidator(self.US_ZIP_REGEX),
            NotStartsWithValidator(self.INVALID_ZIP_CODES)
        ]
        super().__init__(validation_rules)

    def get_name(self) -> str:
        return self.NAME

    def get_aliases(self) -> List[str]:
        return self.ALIASES.copy()

    def normalize(self, value: str) -> str:
        """
        Normalize a US ZIP code to standard 5-digit format.

        For US ZIP codes:
        - 3-digit ZIP code (ZIP-3) is padded with "00" to create 5-digit format (e.g., "951" becomes "95100")
        - 5-digit or longer ZIP codes return the first 5 digits (e.g., "12345-6789" becomes "12345")
        If the input value is null or doesn't match US ZIP pattern, the original
        trimmed value is returned.
        """
        if not value:
            return value

        trimmed = AttributeUtilities.remove_whitespace(value.strip())

        # Check if it's a 3-digit ZIP code (ZIP-3) - pad with "00"
        if re.match(r"^\d{3}$", trimmed):
            return trimmed + "00"

        # Check if it's a US ZIP code (5 digits, 5+4 with dash, or 9 digits without dash)
        if re.match(r"\d{5}(-?\d{4})?", trimmed) or re.match(r"\d{9}", trimmed):
            return trimmed[:5]

        # For values that don't match US ZIP patterns, return trimmed original
        return value.strip()
