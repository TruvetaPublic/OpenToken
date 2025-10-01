# OpenToken Python Token & Attribute Registration Guide

This guide explains how to add new Token or Attribute classes to the OpenToken Python project and register them for dynamic loading.

## Overview
OpenToken uses dynamic module discovery to load all `Token` and `Attribute` implementations at runtime. This allows you to add new tokens and attributes by simply creating new classes in the appropriate directories—no need to modify core registry code.

## Steps to Add a New Token

1. **Create the Token Class**
   - Implement a new class in `lib/python/src/main/opentoken/tokens/definitions` that inherits from `Token`.
   - Example:
     ```python
     from opentoken.tokens.token import Token
     from opentoken.attributes.attribute_expression import AttributeExpression
     class T6Token(Token):
         def get_identifier(self):
             return "T6"
         def get_definition(self):
             return [AttributeExpression(...), ...]
     ```

2. **No Manual Registration Needed**
   - The registry (`TokenRegistry`) will automatically discover all token classes in the `definitions` directory.
   - Just ensure your file is named `t6_token.py` and your class is public and properly implemented.

3. **Build and Test**
   - Run your tests (e.g., `pytest src/test`) to ensure your new token is discovered and loaded.
   - Add or update unit tests as needed.

## Steps to Add a New Attribute

1. **Create the Attribute Class**
   - Implement a new class in the appropriate package (e.g., `opentoken/attributes/person`) that inherits from `Attribute`.
   - Example:

     ```python
     from opentoken.attributes.attribute import Attribute
     class MiddleNameAttribute(Attribute):
         ...
     ```

2. **Register the Attribute in AttributeLoader**
   - Open `lib/python/src/main/opentoken/attributes/attribute_loader.py`.
   - Import your new attribute class at the top of the file.
   - Add an instance of your new attribute to the set in the `AttributeLoader.load()` method:

     ```python
     attributes.add(MiddleNameAttribute())
     ```

3. **Build and Test**
   - Run your tests to ensure your new attribute is discovered and loaded.
   - Add or update unit tests as needed.

## Notes
- Each token or attribute class should be in its own file, named appropriately (e.g., `t6_token.py`, `middle_name_attribute.py`).
- The registry uses Python's `pkgutil` and `importlib` to discover classes, so ensure your directories contain `__init__.py` files.
- If you rename or move a class, ensure the registry can still find it.
- Always bump the project version before submitting a PR (see project instructions).

## Troubleshooting
- If your new token or attribute is not loaded, check for typos in the class name, file name, and ensure your class is public and has a no-argument constructor.
- Use unit tests to verify dynamic loading.

---
For more details, see the OpenToken README and the registry implementation in `token_registry.py`.
