# OpenToken Token & Attribute Registration Guide

This parent guide provides an overview of how to add and register new Token and Attribute classes in the OpenToken project for both Java and Python implementations. For detailed, language-specific instructions, see the linked guides below.

## Overview
OpenToken supports dynamic discovery and registration of Token and Attribute classes, enabling extensibility without modifying core registry code. The registration process differs between Java and Python:

- **Java:** Uses the Service Provider Interface (SPI) via `META-INF/services` and Java's `ServiceLoader`.
- **Python:** Uses dynamic module discovery and explicit registration in loader classes.

## Guides
- [Java Token & Attribute Registration Guide](./dev-guide-java-token-attribute-registration.md)
- [Python Token & Attribute Registration Guide](./dev-guide-python-token-attribute-registration.md)

## When to Use These Guides
- When adding a new Token or Attribute to OpenToken
- When updating or refactoring existing Token/Attribute classes
- When troubleshooting dynamic loading issues

## Additional Resources
- See the [OpenToken README](../README.md) for project-wide instructions and requirements.
- Always bump the project version before submitting a PR (see project instructions).

---
For further details, refer to the language-specific guides above.