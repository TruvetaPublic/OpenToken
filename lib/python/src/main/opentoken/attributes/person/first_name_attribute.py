from typing import List
import re
from opentoken.attributes.base_attribute import BaseAttribute
from opentoken.attributes.utilities.attribute_utilities import AttributeUtilities
from opentoken.attributes.validation.not_in_validator import NotInValidator


class FirstNameAttribute(BaseAttribute):
    """Represents the first name of a person.

    This class extends BaseAttribute and provides functionality for working with
    first name fields. It recognizes "FirstName" and "GivenName" as valid aliases
    for this attribute type.

    The attribute performs no normalization on input values, returning them
    unchanged.
    """

    NAME = "FirstName"
    ALIASES = [NAME, "GivenName"]

    # Pattern to match and remove common titles
    TITLE_PATTERN = re.compile(
        r"(?i)^\s*(mr|mrs|ms|miss|dr|prof|capt|sir|col|gen|cmdr|lt|"
        r"rabbi|father|brother|sister|hon|honorable|reverend|rev|doctor)\.?\s+",
        re.IGNORECASE
    )

    # Pattern to match trailing periods and middle initials in names.
    #
    # This pattern matches:
    #  - A space, followed by a single non-space character (middle initial),
    #    optionally followed by a period, at the end of the string.
    #
    # Breakdown of the regex:
    #   \s           A space
    #   [^\s]        Any single non-space character (middle initial)
    #   \.?          Optional period
    #   $            End of string
    TRAILING_PERIOD_AND_INITIAL_PATTERN = re.compile(r'\s[^\s]\.?$')

    def __init__(self):
        placeholder_values = AttributeUtilities.COMMON_PLACEHOLDER_NAMES
        validation_rules = [NotInValidator(placeholder_values)]
        super().__init__(validation_rules)

    def get_name(self) -> str:
        return self.NAME

    def get_aliases(self) -> List[str]:
        return self.ALIASES.copy()

    def normalize(self, value: str) -> str:
        """Returns the value unchanged after removing titles."""
        if not value:
            return value

        normalized = AttributeUtilities.normalize_diacritics(value)

        without_title = re.sub(self.TITLE_PATTERN, '', normalized).strip()

        if without_title:
            normalized = without_title

        without_suffix = AttributeUtilities.remove_generational_suffix(normalized)

        if without_suffix:
            normalized = without_suffix

        normalized = re.sub(self.TRAILING_PERIOD_AND_INITIAL_PATTERN, '', normalized).strip()

        # Remove non-alphabetic characters
        normalized = AttributeUtilities.NON_ALPHABETIC_PATTERN.sub('', normalized)

        # Normalize whitespace
        normalized = AttributeUtilities.WHITESPACE_PATTERN.sub(' ', normalized)

        return normalized
