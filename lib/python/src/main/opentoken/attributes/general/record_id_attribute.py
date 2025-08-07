"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import List

from opentoken.attributes.base_attribute import BaseAttribute


class RecordIdAttribute(BaseAttribute):
    """Represents a record identifier attribute.

    This class extends BaseAttribute and provides functionality for working with
    record ID fields. It recognizes "RecordId" as a valid alias for this
    attribute type.

    The attribute performs no normalization on input values, returning them
    unchanged. It validates that the value is not null or empty using the
    default BaseAttribute validation rules.
    """

    NAME = "RecordId"
    ALIASES = [NAME, "Id"]

    def __init__(self):
        # Use default validation rules from BaseAttribute (not null or empty)
        super().__init__()

    def get_name(self) -> str:
        """Get the name of the attribute.

        Returns:
            str: The name "RecordId"
        """
        return self.NAME

    def get_aliases(self) -> List[str]:
        """Get the aliases for the attribute.

        Returns:
            List[str]: A list containing the aliases for this attribute
        """
        return self.ALIASES.copy()

    def normalize(self, value: str) -> str:
        """Normalize the record ID value.

        This implementation returns the value unchanged, as record IDs
        typically don't require normalization.

        Args:
            value (str): The record ID value to normalize

        Returns:
            str: The unchanged value
        """
        return value if value is not None else ""
