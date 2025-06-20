This is a Java based repository with a library to generate privacy preserving tokens for data matching. 
Please follow these guidelines when contributing:

## Code Standards

### Required Before Each Commit
- Organize all imports and remove unused imports
- Ensure Truveta header is the first line of each file

### Development Flow
- Build: `mvn clean install -DskipTests`
- Test: `mvn test`

## Repository Structure
- `README.md`: Overview of the project
- `src/main`: Main Java source code
- `src/test`: Test code
- `docs/`: Documentation
- `.devcontainer/`: Protocol buffer definitions. Run `make proto` after making updates here.
- `tools/`: Tools to generate test data or validate encryption

## Key Guidelines
1. Follow Java best practices and idiomatic patterns
2. Maintain existing code structure and organization
3. Use dependency injection patterns where appropriate
4. Write unit tests for new functionality. Use table-driven unit tests when possible.
5. Suggest changes to the `docs/` folder when appropriate