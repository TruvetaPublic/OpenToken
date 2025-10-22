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

        For Canadian postal codes:
        - 3-character format (e.g., "J1X") is padded with " 000" to create full format (e.g., "J1X 000")
        - 4-character format (e.g., "J1X1") is padded with "A0" to create full format (e.g., "J1X 1A0")
        - 5-character format (e.g., "J1X1A") is padded with "0" to create full format (e.g., "J1X 1A0")
        - 6-character format returns uppercase format with space (e.g., "k1a0a6" becomes "K1A 0A6")
        If the input value is null or doesn't match Canadian postal pattern, the original
        trimmed value is returned.
        """
        if not value:
            return value

        trimmed = AttributeUtilities.remove_whitespace(value.strip())

        # Check if it's a 3-character Canadian postal code (ZIP-3) - pad with " 000"
        if re.match(r"^[A-Za-z]\d[A-Za-z]$", trimmed):
            upper = trimmed.upper()
            return f"{upper} 000"

        # Check if it's a 4-character partial postal code (e.g., "A1A1") - pad with "A0"
        if re.match(r"^[A-Za-z]\d[A-Za-z]\d$", trimmed):
            upper = trimmed.upper()
            return f"{upper[:3]} {upper[3]}A0"

        # Check if it's a 5-character partial postal code (e.g., "A1A1A") - pad with "0"
        if re.match(r"^[A-Za-z]\d[A-Za-z]\d[A-Za-z]$", trimmed):
            upper = trimmed.upper()
            return f"{upper[:3]} {upper[3:]}0"

        # Check if it's a Canadian postal code (6 alphanumeric characters)
        if re.match(r"[A-Za-z]\d[A-Za-z]\d[A-Za-z]\d", trimmed):
            upper = trimmed.upper()
            return f"{upper[:3]} {upper[3:6]}"

        # For values that don't match Canadian postal patterns, return trimmed original
        return value.strip()
