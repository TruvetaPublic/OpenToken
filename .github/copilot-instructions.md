We use Java for our development, and we follow the Java coding conventions. Please ensure that your code adheres to the following guidelines:
We use JUnit for unit testing. Please write unit tests for any new features or bug fixes you implement.
We use Maven for dependency management and project structure. Ensure that your code is compatible with Maven.

Please run `mvn clean install` to ensure that everything works as expected before a commit.

**IMPORTANT: VERSION BUMPING IS REQUIRED FOR ALL PRs**
Before creating a pull request, you MUST bump the version using `bump2version` with the appropriate level:

- `bump2version patch` for bug fixes and minor changes (like adding invalid SSNs)
- `bump2version minor` for new features
- `bump2version major` for breaking changes

DO NOT manually edit version numbers in pom.xml, README.md, or other files - always use the `bump2version` command which automatically updates all version references consistently across the project.

When creating new branches, follow the naming convention `dev/<username>/<feature>` where `<username>` is your GitHub username and `<feature>` is a brief description of the feature or bug fix you are working on.

Ensure that your code is well-documented, including method-level comments and class-level documentation where necessary.
Ensure the README.md is kept up to date with any new features or changes you make.
