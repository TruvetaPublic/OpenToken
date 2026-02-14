"""
Copyright (c) Truveta. All rights reserved.

Utility module for masking sensitive strings in CLI commands.
"""


def mask_string(input_str: str | None) -> str:
    """
    Mask a sensitive string for logging.
    
    - None values are masked as "<null>"
    - Strings with 3 or fewer characters are masked as "***"
    - Longer strings show first 3 characters followed by asterisks
    
    Args:
        input_str: The string to mask
        
    Returns:
        The masked string
    """
    if input_str is None:
        return "<null>"
    if len(input_str) <= 3:
        return "***"
    return input_str[:3] + "*" * (len(input_str) - 3)
