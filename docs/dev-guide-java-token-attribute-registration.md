# OpenToken Java Token & Attribute Registration Guide

This guide explains how to add new Token or Attribute classes to the OpenToken Java project and register them for dynamic loading using the `META-INF/services` mechanism.

## Overview
OpenToken uses Java's [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to discover and load implementations of `Token` and `Attribute` at runtime. This allows new tokens and attributes to be added without modifying core code—just by creating new classes and updating the appropriate service files.

## Steps to Add a New Token

1. **Create the Token Class**
   - Implement a new class in `com.truveta.opentoken.tokens.definitions` that extends `com.truveta.opentoken.tokens.Token`.
   - Example:
     ```java
     package com.truveta.opentoken.tokens.definitions;
     import com.truveta.opentoken.tokens.Token;
     public class T6Token extends Token {
         // Implement required methods
     }
     ```

2. **Register the Token in META-INF/services**
   - Open the file: `lib/java/src/main/resources/META-INF/services/com.truveta.opentoken.tokens.Token`
   - Add the fully qualified class name of your new token (one per line):
     ```
     com.truveta.opentoken.tokens.definitions.T6Token
     ```

3. **Build and Test**
   - Run `mvn clean install` to ensure your new token is discovered and loaded.
   - Add or update unit tests as needed.

## Steps to Add a New Attribute

1. **Create the Attribute Class**
   - Implement a new class in the appropriate package (e.g., `com.truveta.opentoken.attributes.person`) that extends `com.truveta.opentoken.attributes.Attribute`.
   - Example:
     ```java
     package com.truveta.opentoken.attributes.person;
     import com.truveta.opentoken.attributes.Attribute;
     public class MiddleNameAttribute extends Attribute {
         // Implement required methods
     }
     ```

2. **Register the Attribute in META-INF/services**
   - Open the file: `lib/java/src/main/resources/META-INF/services/com.truveta.opentoken.attributes.Attribute`
   - Add the fully qualified class name of your new attribute (one per line):
     ```
     com.truveta.opentoken.attributes.person.MiddleNameAttribute
     ```

3. **Build and Test**
   - Run `mvn clean install` to ensure your new attribute is discovered and loaded.
   - Add or update unit tests as needed.

## Notes
- The service files must list each implementation class on a separate line, with no extra spaces or comments.
- If you rename or move a class, update the service file accordingly.
- Always bump the project version before submitting a PR (see project instructions).

## Troubleshooting
- If your new token or attribute is not loaded, check for typos in the service file and ensure your class is public and has a no-argument constructor.
- Use unit tests to verify dynamic loading.

---
For more details, see the official Java ServiceLoader documentation and the OpenToken README.
