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
    CANADIAN_POSTAL_REGEX = r"^\s*[A-Za-z]\d[A-Za-z]\s?\d[A-Za-z]\d\s*$"

    INVALID_ZIP_CODES = {
        # Canadian postal code placeholders
        "A1A 1A1",
        "K1A 0A6",
        "H0H 0H0",
        "X0X 0X0",
        "Y0Y 0Y0",
        "Z0Z 0Z0",
        "A0A 0A0",
        "B1B 1B1",
        "C2C 2C2"
    }

    def __init__(self):
        validation_rules = [
            RegexValidator(self.CANADIAN_POSTAL_REGEX),
            NotStartsWithValidator(self.INVALID_ZIP_CODES)
        ]
        super().__init__(validation_rules)

    def get_name(self) -> str:
        return self.NAME

    def get_aliases(self) -> List[str]:
        return self.ALIASES.copy()

    def normalize(self, value: str) -> str:
        """
        Normalize a Canadian postal code to standard A1A 1A1 format.

        For Canadian postal codes: returns uppercase format with space (e.g.,
        "k1a0a6" becomes "K1A 0A6")
        If the input value is null or doesn't match Canadian postal pattern, the original
        trimmed value is returned.
        """
        if not value:
            return value

        trimmed = AttributeUtilities.remove_whitespace(value.strip())

        # Check if it's a Canadian postal code (6 alphanumeric characters)
        if re.match(r"[A-Za-z]\d[A-Za-z]\d[A-Za-z]\d", trimmed):
            upper = trimmed.upper()
            return f"{upper[:3]} {upper[3:6]}"

        # For values that don't match Canadian postal patterns, return trimmed original
        return value.strip()
