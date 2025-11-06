# Development Tools

## Java-Python Sync Tool

### Overview
The Java-Python sync tool automatically detects changes in Java files and tracks corresponding Python file updates to ensure the codebase stays synchronized. It provides intelligent mapping between Java and Python implementations, progress tracking across multiple commits, and seamless GitHub PR integration.

This tool is essential for maintaining consistency between the Java and Python implementations of the OpenToken library, ensuring that updates to one language are properly reflected in the other.

### Usage

#### Command Line Interface
```bash
# Basic sync check (compares against HEAD~1)
python3 tools/java_python_syncer.py

# Check against specific branch/commit
python3 tools/java_python_syncer.py --since origin/main

# Generate GitHub-style checklist
python3 tools/java_python_syncer.py --format github-checklist

# Output as JSON for automation
python3 tools/java_python_syncer.py --format json

# Comprehensive health check
python3 tools/java_python_syncer.py --health-check

# Validate configuration only
python3 tools/java_python_syncer.py --validate-only
```

#### GitHub Actions Integration
The tool automatically runs on pull requests via the `.github/workflows/java-python-sync-enhanced.yml` workflow:

- **Triggers**: On PR open, synchronize, or reopen
- **Scope**: Changes to Java files in `lib/java/opentoken/src/main/java/com/truveta/opentoken/` or Python files in `lib/python/opentoken/src/`
- **Output**: Automated PR comments with progress tracking and checklists
- **Permissions**: Requires `issues: write` and `pull-requests: write` permissions

### Configuration

#### Mapping File: `tools/java-python-mapping.json`
The tool's behavior is configured via a JSON mapping file with the following structure:

```json
{
  "critical_files": {
    "lib/java/opentoken/src/main/java/com/truveta/opentoken/SpecialClass.java": {
      "python_file": "lib/python/opentoken/src/main/opentoken/special_class.py",
      "sync_priority": "high",
      "description": "Critical authentication component",
      "auto_sync": false,
      "requires_manual_review": true
    }
  },
  "directory_mappings": {
    "lib/java/opentoken/src/main/java/com/truveta/opentoken/": {
      "python_directory": "lib/python/opentoken/src/main/opentoken/",
      "sync_priority": "medium",
      "description": "Main source directory mapping",
      "auto_sync": true
    },
    "lib/java/opentoken/src/test/java/com/truveta/opentoken/": {
      "python_directory": "lib/python/opentoken/src/test/opentoken/",
      "sync_priority": "low",
      "description": "Test directory mapping",
      "auto_sync": true
    }
  },
  "ignore_patterns": [
    "*/generated/*",
    "**/target/**",
    "**/*Test.java"
  ],
  "auto_generate_unmapped": true
}
```

#### Configuration Options

- **`critical_files`**: Exact file-to-file mappings for important components
  - `python_file`: Target Python file path
  - `sync_priority`: `high`, `medium`, or `low`
  - `description`: Human-readable description
  - `auto_sync`: Whether changes can be automatically synchronized
  - `requires_manual_review`: Always require manual review

- **`directory_mappings`**: Pattern-based mappings for directory structures
  - `python_directory`: Target Python directory prefix
  - Supports automatic naming convention conversion (CamelCase → snake_case)

- **`ignore_patterns`**: Files/patterns to exclude from sync checking
  - Supports glob patterns (`*`, `**`, `?`)
  - Useful for generated files, build artifacts, etc.

- **`auto_generate_unmapped`**: Whether to auto-generate mappings for unmapped files

### File Naming Conventions

The tool automatically converts between Java and Python naming conventions:

#### Java → Python Conversion Rules
- **Classes**: `TokenTransformer.java` → `token_transformer.py`
- **Test files**: `TokenTransformerTest.java` → `token_transformer_test.py`
- **Directories**: Preserved as-is
- **Packages**: `com.truveta.opentoken` → `opentoken`

#### Examples
```
Java: lib/java/opentoken/src/main/java/com/truveta/opentoken/attributes/BirthDateAttribute.java
Python: lib/python/opentoken/src/main/opentoken/attributes/birth_date_attribute.py

Java: lib/java/opentoken/src/test/java/com/truveta/opentoken/TokenGeneratorTest.java
Python: lib/python/opentoken/src/test/opentoken/token_generator_test.py
```

### Output Formats

#### Console Format (Default)
```
Java changes detected (1 Java files):
============================================================

📁 lib/java/opentoken/src/main/java/com/truveta/opentoken/TokenGenerator.java:
   ✅ 🔄 lib/python/opentoken/src/main/opentoken/token_generator.py
----------------------------------------

PROGRESS SUMMARY:
Total sync items: 1
Recently updated: 1
Still pending: 0

LEGEND:
  ✅ = File exists, ❌ = File missing
  🔄 = Up-to-date (Python modified after Java), ⏳ = Out-of-date (needs update)
```

#### GitHub Checklist Format
```markdown
## Java to Python Sync Required (1/2 completed)

### 📁 `lib/java/opentoken/src/main/java/com/truveta/opentoken/TokenGenerator.java`
- [x] **🔄 UPDATED**: `lib/python/opentoken/src/main/opentoken/token_generator.py`
- [ ] **⏳ NEEDS UPDATE**: `lib/python/opentoken/src/test/opentoken/token_generator_test.py`

✅ **Progress**: 1 of 2 items completed
```

#### JSON Format
```json
{
  "mappings": [...],
  "python_changes": [...],
  "total_items": 2,
  "completed_items": 1
}
```

### Workflow Integration

#### GitHub Actions Workflow
The enhanced workflow (`.github/workflows/java-python-sync-enhanced.yml`) provides:

1. **Change Detection**: Compares against PR base branch for accurate change detection
2. **Progress Tracking**: Tracks completion across multiple commits
3. **Comment Management**: Maintains clean PR history by replacing previous comments
4. **Status Reporting**: Provides both workflow logs and PR comments

### Related Files
- `tools/java_python_syncer.py` - Main tool implementation
- `tools/java-python-mapping.json` - Configuration file
- `tools/sync-check-enhanced.sh` - Shell wrapper script
- `.github/workflows/java-python-sync-enhanced.yml` - GitHub Actions workflow