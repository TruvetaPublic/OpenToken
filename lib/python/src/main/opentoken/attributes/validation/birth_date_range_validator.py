"""
Copyright (c) Truveta. All rights reserved.
"""

from datetime import date
from opentoken.attributes.validation.date_range_validator import DateRangeValidator


class BirthDateRangeValidator(DateRangeValidator):
    """
    A Validator that asserts that the birth date value is within
    an acceptable range (between 1910/1/1 and today).

    This validator checks that birth dates are:
    - Not before January 1, 1910
    - Not after today's date

    If the date is outside this range, the validation fails.
    """

    def __init__(self):
        """Initialize the validator with birth date range (1910/1/1 to today)."""
        min_date = date(1910, 1, 1)
        super().__init__(min_date=min_date, use_current_as_max=True)
