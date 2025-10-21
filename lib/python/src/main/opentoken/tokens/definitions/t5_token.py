"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import List
from opentoken.attributes.attribute_expression import AttributeExpression
from opentoken.attributes.person.first_name_attribute import FirstNameAttribute
from opentoken.attributes.person.last_name_attribute import LastNameAttribute
from opentoken.attributes.person.sex_attribute import SexAttribute
from opentoken.tokens.token import Token


class T5Token(Token):
    """
    Represents the token definition for token T5.

    It is a collection of attribute expressions that are concatenated together
    to get the token signature. The token signature is as follows:
    U(last-name)|U(first-name-3)|U(gender)
    """

    ID = "T5"

    def __init__(self):
        """Initialize the T5 token definition."""
        self._definition = [
            AttributeExpression(LastNameAttribute, "T|U"),
            AttributeExpression(FirstNameAttribute, "T|S(0,3)|U"),
            AttributeExpression(SexAttribute, "T|U")
        ]

    def get_identifier(self) -> str:
        """Get the unique identifier for the token."""
        return self.ID

    def get_definition(self) -> List[AttributeExpression]:
        """Get the list of attribute expressions that define the token."""
        return self._definition
