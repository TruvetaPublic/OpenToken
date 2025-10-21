"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import List
from opentoken.attributes.combined_attribute import CombinedAttribute
from opentoken.attributes.serializable_attribute import SerializableAttribute
from opentoken.attributes.person.us_postal_code_attribute import USPostalCodeAttribute
from opentoken.attributes.person.canadian_postal_code_attribute import CanadianPostalCodeAttribute


class PostalCodeAttribute(CombinedAttribute):
    """
    Represents the postal code of a person.

    This class combines US and Canadian postal code implementations to provide
    functionality for working with postal code fields. It recognizes "PostalCode",
    "ZipCode", "ZIP3", and "ZIP5" as valid aliases for this attribute type.

    The attribute performs normalization on input values, converting them to a
    standard format. Supports both US ZIP codes (3, 5, or 9 digits) and Canadian
    postal codes (3 or 6 characters in A1A 1A1 format). ZIP-3 codes (3 digits/characters)
    are automatically padded to full length during normalization.
    """

    NAME = "PostalCode"
    ALIASES = [NAME, "ZipCode", "ZIP3", "ZIP5"]

    def __init__(self):
        self._implementations = [
            USPostalCodeAttribute(),
            CanadianPostalCodeAttribute()
        ]
        super().__init__()

    def get_name(self) -> str:
        return self.NAME

    def get_aliases(self) -> List[str]:
        return self.ALIASES.copy()

    def get_attribute_implementations(self) -> List[SerializableAttribute]:
        return self._implementations
