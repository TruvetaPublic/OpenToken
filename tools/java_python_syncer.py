#!/usr/bin/env python3
"""
Java to Python Sync Tool
Detects changes in Java codebase and creates corresponding Python sync tasks
"""

import json
import subprocess
from pathlib import Path
from datetime import datetime


class JavaPythonSyncer:
    FALLBACK_MAPPINGS = {
        "critical_files": {},
        "directory_mappings": {},
        "ignore_patterns": [],
        "auto_generate_unmapped": True
    }

    def __init__(self, mapping_file="tools/java-python-mapping.json"):
        self.root_dir = Path(__file__).parent.parent
        self.mapping_file = self.root_dir / mapping_file
        self.load_mappings()

    def load_mappings(self):
        """Load the Java to Python file mappings"""
        try:
            with open(self.mapping_file, 'r') as f:
                self.mappings = json.load(f)
        except FileNotFoundError:
            print(f"Warning: Mapping file not found: {self.mapping_file}")
            self.mappings = self.FALLBACK_MAPPINGS.copy()
        except json.JSONDecodeError as e:
            print(f"Error: Invalid JSON in mapping file: {e}")
            self.mappings = self.FALLBACK_MAPPINGS.copy()

    def validate_configuration(self):
        """Validate the mapping configuration and repository state
        
        Args:
            None
            
        Returns:
            list: A list of validation issues found.
        """
        issues = []
        
        # Check if mapping file exists
        if not self.mapping_file.exists():
            issues.append(f"Mapping file not found: {self.mapping_file}")
            return issues
        
        # Validate mapping structure
        required_keys = ['critical_files', 'directory_mappings', 'ignore_patterns']
        for key in required_keys:
            if key not in self.mappings:
                issues.append(f"Missing required mapping key: {key}")
        
        # Check if mapped files actually exist
        if 'critical_files' in self.mappings:
            for java_path, mapping in self.mappings['critical_files'].items():
                java_file = self.root_dir / java_path
                if not java_file.exists():
                    issues.append(f"Mapped Java file not found: {java_path}")
                
                python_file = self.root_dir / mapping['python_file']
                if not python_file.exists():
                    issues.append(f"Mapped Python file not found: {mapping['python_file']}")
        
        # Check Git repository status
        try:
            result = subprocess.run(['git', 'status', '--porcelain'], 
                                  capture_output=True, text=True, cwd=self.root_dir)
            # Check if there are any uncommitted changes
            if result.stdout.strip():
                issues.append("Working directory has uncommitted changes")
        except subprocess.CalledProcessError:
            issues.append("Not in a Git repository or Git not available")
        
        return issues
    
    def health_check(self):
        """Perform a comprehensive health check of the sync system.
            Prints the health check results.
        """
        print("🔍 Performing Java-Python sync health check...")
        print("=" * 50)
        
        issues = self.validate_configuration()
        
        if not issues:
            print("✅ Configuration validation passed")
        else:
            print("❌ Configuration issues found:")
            for issue in issues:
                print(f"   - {issue}")
        
        # Statistics
        print(f"\nMapping Statistics:")
        critical_count = len(self.mappings.get('critical_files', {}))
        directory_count = len(self.mappings.get('directory_mappings', {}))
        ignore_count = len(self.mappings.get('ignore_patterns', []))
        
        print(f"   - Critical file mappings: {critical_count}")
        print(f"   - Directory mappings: {directory_count}")  
        print(f"   - Ignore patterns: {ignore_count}")
        
        return len(issues) == 0
    
    def _convert_python_to_java_path(self, python_path):
        """Convert Python file path back to Java path format
        Args:
            python_path (string): The Python file path to convert.

        Returns:
            The corresponding Java file path.
        """
        # Implementation would depend on your naming conventions
        import re
        
        # Convert snake_case back to CamelCase for file names
        path_parts = python_path.split('/')
        if path_parts[-1].endswith('.py'):
            filename = path_parts[-1][:-3]  # Remove .py
            
            # Convert snake_case to CamelCase
            if '_test' in filename:
                # Handle test files: some_test -> SomeTest
                base_name = filename.replace('_test', '')
                camel_name = ''.join(word.capitalize() for word in base_name.split('_'))
                java_filename = camel_name + 'Test.java'
            else:
                # Regular files: some_class -> SomeClass
                camel_name = ''.join(word.capitalize() for word in filename.split('_'))
                java_filename = camel_name + '.java'
            
            path_parts[-1] = java_filename
        
        return '/'.join(path_parts)

    def get_java_changes(self, since_commit="HEAD~1"):
        """Get list of changed Java files since specified commit with timestamps

        Args:
            since_commit: The commit to compare against (default: HEAD~1)

        Returns:
            A list of changed Java files with their last modified timestamps
        """
        try:
            # For PR workflows, compare against the base branch
            if since_commit == "HEAD~1":
                # Try to get the merge base with main/origin/main
                try:
                    merge_base_result = subprocess.run([
                        'git', 'merge-base', 'HEAD', 'origin/main'
                    ], capture_output=True, text=True, cwd=self.root_dir)
                    
                    if merge_base_result.returncode == 0:
                        since_commit = merge_base_result.stdout.strip()
                        print(f"Comparing against PR base: {since_commit[:8]}")
                except subprocess.CalledProcessError:
                    # Fallback to HEAD~1 if merge-base fails
                    pass
            
            result = subprocess.run([
                'git', 'diff', '--name-only', f'{since_commit}', 'HEAD', '--', 'lib/java/opentoken/src/'
            ], capture_output=True, text=True, cwd=self.root_dir)

            if result.returncode == 0:
                all_files = [line.strip() for line in result.stdout.splitlines() if line.strip()]
                # Filter out ignored files
                return self._filter_ignored_files(all_files)
            return []
        except subprocess.CalledProcessError:
            return []

    def get_file_last_modified_commit(self, file_path, since_commit="HEAD~1"):
        """Get the most recent commit that modified a specific file
        
        Args:
            file_path: The path to the file to check.
            since_commit: The commit after which we would like to check file_path's status (default: HEAD~1)

        Returns:
            A dictionary containing the commit hash and timestamp of the last modification,
            or None if the file was not modified.
        """
        try:
            result = subprocess.run([
                'git', 'log', '-1', '--format=%H %ct', f'{since_commit}..HEAD', '--', file_path
            ], capture_output=True, text=True, cwd=self.root_dir)
            
            if result.returncode == 0 and result.stdout.strip():
                commit_hash, timestamp = result.stdout.strip().split()
                return {
                    'commit': commit_hash,
                    'timestamp': int(timestamp)
                }
            return None
        except subprocess.CalledProcessError:
            return None

    def is_python_file_up_to_date(self, java_file, python_file, since_commit="HEAD~1"):
        """Check if Python file is up-to-date relative to Java file changes
        
        Args:
            java_file: The path to the Java file.
            python_file: The path to the Python file.
            since_commit: The commit after which we would like to check file modifications 
                (default: HEAD~1 will check against base of the branch).

        Returns:
            bool: True if the Python file is up-to-date, False otherwise.
        """
        java_last_modified = self.get_file_last_modified_commit(java_file, since_commit)
        python_last_modified = self.get_file_last_modified_commit(python_file, since_commit)
        
        # If Java file wasn't modified at all in this PR, no sync needed
        if not java_last_modified:
            return True
            
        # If Python file wasn't modified at all in this PR, it's out of date
        if not python_last_modified:
            return False
            
        # Python is up-to-date if it was modified after the Java file
        return python_last_modified['timestamp'] >= java_last_modified['timestamp']

    def _filter_ignored_files(self, files):
        """Filter out files that match ignore patterns

        Args:
            files: A list of file paths to filter.

        Returns:
            A list of file paths that do not match any ignore patterns.
        """
        import fnmatch
        
        ignore_patterns = self.mappings.get("ignore_patterns", [])
        filtered_files = []
        
        for file in files:
            should_ignore = False
            for pattern in ignore_patterns:
                if fnmatch.fnmatch(file, pattern):
                    should_ignore = True
                    break
            
            if not should_ignore:
                filtered_files.append(file)
        
        return filtered_files

    def map_java_to_python(self, java_file):
        """Map a Java file to its Python equivalent using enhanced strategy.
        
        Args:
            java_file (string): The path to the Java file.
            
        Returns:
            A mapping of the Python file path and its sync metadata, or None if no mapping is found.
        """
        # First check critical files (exact matches)
        if "critical_files" in self.mappings:
            for java_path, mapping in self.mappings["critical_files"].items():
                if java_file == java_path:
                    return {
                        "python_file": mapping["python_file"],
                        "sync_priority": mapping.get("sync_priority", "medium"),
                        "description": mapping.get("description", ""),
                        "auto_sync": mapping.get("auto_sync", False),
                        "requires_manual_review": mapping.get("requires_manual_review", True)
                    }

        # Then check directory mappings
        if "directory_mappings" in self.mappings:
            for java_dir, mapping in self.mappings["directory_mappings"].items():
                if java_file.startswith(java_dir):
                    # Auto-generate Python file path
                    relative_path = java_file[len(java_dir):]
                    python_file = mapping["python_directory"] + self._convert_to_python_naming(relative_path)

                    return {
                        "python_file": python_file,
                        "sync_priority": mapping.get("sync_priority", "medium"),
                        "description": mapping.get("description", "Auto-generated mapping"),
                        "auto_sync": mapping.get("auto_sync", True),
                        "requires_manual_review": False
                    }

        # Fallback: auto-generate if enabled
        if self.mappings.get("auto_generate_unmapped", True):
            return self._auto_generate_mapping(java_file)

        return None

    def _convert_to_python_naming(self, java_file_path):
        """Convert Java file path to Python naming conventions.
        
        Args:
            java_file_path: The path to the Java file.
        
        Returns:
            A string representing the Python file path.
        """
        # Convert CamelCase to snake_case for file names
        import re

        # Handle paths with directories
        if '/' in java_file_path:
            path_parts = java_file_path.split('/')
            # Convert only the filename (last part), keep directory structure
            directory_path = '/'.join(path_parts[:-1])
            filename = path_parts[-1]
            converted_filename = self._convert_to_python_naming(filename)
            return directory_path + '/' + converted_filename

        # Handle .java extension
        if java_file_path.endswith('.java'):
            base_name = java_file_path[:-5]  # Remove .java
            
            # Handle test files: JavaTest -> java_test.py (not test_java.py)
            if base_name.endswith('Test'):
                # Remove 'Test' suffix, convert to snake_case, then add '_test'
                class_name = base_name[:-4]  # Remove 'Test'
                snake_case = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', class_name)
                snake_case = re.sub('([a-z0-9])([A-Z])', r'\1_\2', snake_case).lower()
                return snake_case + '_test.py'
            else:
                # Regular files: convert CamelCase to snake_case
                snake_case = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', base_name)
                snake_case = re.sub('([a-z0-9])([A-Z])', r'\1_\2', snake_case).lower()
                return snake_case + '.py'

        return java_file_path

    def _auto_generate_mapping(self, java_file):
        """Auto-generate Python mapping for unmapped Java files.
        
        Args:
            java_file: The path to the Java file.
        
        Returns:
            A mapping of the Python file path and its sync metadata, or None if no mapping is found.
        """
        # Convert directory paths for both main and test files
        python_file = java_file
        
        # Handle main source files
        if "lib/java/opentoken/src/main/java/com/truveta/opentoken/" in java_file:
            python_file = java_file.replace(
                "lib/java/opentoken/src/main/java/com/truveta/opentoken/",
                "lib/python/opentoken/src/main/opentoken/"
            )
        # Handle test files
        elif "lib/java/opentoken/src/test/java/com/truveta/opentoken/" in java_file:
            python_file = java_file.replace(
                "lib/java/opentoken/src/test/java/com/truveta/opentoken/",
                "lib/python/opentoken/src/test/opentoken/"
            )
        
        python_file = self._convert_to_python_naming(python_file)

        return {
            "python_file": python_file,
            "sync_priority": "low",
            "description": f"Auto-generated mapping for {java_file}",
            "auto_sync": True,
            "requires_manual_review": True  # Always review auto-generated
        }

    def get_python_changes(self, since_commit="HEAD~1"):
        """Get list of changed Python files since specified commit
        
        Args:
            since_commit: The commit to compare since.

        Returns:
            A list of changed Python file paths.
        """
        try:
            # Use same logic as Java changes for PR base comparison
            if since_commit == "HEAD~1":
                try:
                    merge_base_result = subprocess.run([
                        'git', 'merge-base', 'HEAD', 'origin/main'
                    ], capture_output=True, text=True, cwd=self.root_dir)
                    
                    if merge_base_result.returncode == 0:
                        since_commit = merge_base_result.stdout.strip()
                except subprocess.CalledProcessError:
                    pass
            
            result = subprocess.run([
                'git', 'diff', '--name-only', f'{since_commit}', 'HEAD', '--', 'lib/python/opentoken/src/'
            ], capture_output=True, text=True, cwd=self.root_dir)

            if result.returncode == 0:
                return [line.strip() for line in result.stdout.splitlines() if line.strip()]
            return []
        except subprocess.CalledProcessError:
            return []

    def check_python_file_exists(self, python_file):
        """Check if the corresponding Python file exists
        
        Args:
            python_file: The path to the Python file.
            
        Returns:
            True if the file exists, False otherwise.
        """
        python_path = self.root_dir / python_file
        return python_path.exists()

    def generate_sync_report(self, output_format="console", since_commit="HEAD~1"):
        """Generate a report of files that need syncing
        
        Args:
            output_format: The format for the output report.
            since_commit: The commit to compare since.
        
        Returns:
            A report of files that need syncing.
        """
        changed_files = self.get_java_changes(since_commit)
        python_changes = self.get_python_changes(since_commit)

        if not changed_files:
            if output_format == "github-checklist":
                return "✅ All Java changes appear to be in sync with Python!"
            else:
                print("No Java changes detected.")
                return

        # Build mapping list for all formats
        mappings = []
        for java_file in changed_files:
            mapping = self.get_mapping_for_file(java_file)
            if mapping:
                # Handle both single and multiple Python files
                python_files = mapping.get('python_files', [mapping.get('python_file', '')])
                if isinstance(python_files, str):
                    python_files = [python_files]
                
                mappings.append({
                    'java_file': java_file,
                    'python_files': python_files
                })

        return self.format_output(mappings, python_changes, output_format, since_commit)

    def format_output(self, mappings, python_changes, output_format="console", since_commit="HEAD~1"):
        """Format the output based on the specified format
        
        Args:
            mappings: The mapping information between Java and Python files.
            python_changes: The list of changed Python files.
            output_format: The format for the output report.
            since_commit: The commit to compare since.
            
        Returns:
            A formatted output based on the specified format.
        """
        if output_format == "github-checklist":
            return self.format_github_checklist(mappings, python_changes, since_commit)
        elif output_format == "json":
            # Update JSON format to also use timestamp-based logic
            total_items = 0
            completed_items = 0
            for mapping in mappings:
                java_file = mapping['java_file']
                for python_file in mapping['python_files']:
                    total_items += 1
                    if self.is_python_file_up_to_date(java_file, python_file, since_commit):
                        completed_items += 1
            
            return json.dumps({
                "mappings": mappings,
                "python_changes": python_changes,
                "total_items": total_items,
                "completed_items": completed_items
            }, indent=2)
        else:
            # Enhanced console format with timestamp-based completion tracking
            output = f"Java changes detected ({len(mappings)} Java files):\n"
            output += "=" * 60 + "\n"
            
            total_items = 0
            completed_items = 0
            
            for mapping in mappings:
                java_file = mapping['java_file']
                python_files = mapping['python_files']
                
                output += f"\n📁 {java_file}:\n"
                for python_file in python_files:
                    total_items += 1
                    exists = "✅" if self.check_python_file_exists(python_file) else "❌"
                    is_up_to_date = self.is_python_file_up_to_date(java_file, python_file, since_commit)
                    
                    if is_up_to_date:
                        status_icon = "🔄"
                        completed_items += 1
                    else:
                        status_icon = "⏳"
                    
                    output += f"   {exists} {status_icon} {python_file}\n"
                output += "-" * 40 + "\n"
            
            # Summary section
            output += "\nPROGRESS SUMMARY:\n"
            output += f"Total sync items: {total_items}\n"
            output += f"Recently updated: {completed_items}\n"
            output += f"Still pending: {total_items - completed_items}\n"
            
            # Legend
            output += "\nLEGEND:\n"
            output += "  ✅ = File exists, ❌ = File missing\n"
            output += "  🔄 = Up-to-date (Python modified after Java), ⏳ = Out-of-date (needs update)\n"
            
            print(output)
            
            # Save enhanced report
            self.save_enhanced_report(mappings, python_changes, total_items, completed_items)

    def format_github_checklist(self, mappings, python_changes, since_commit="HEAD~1"):
        """Format as GitHub checklist with completion status using timestamp-based sync checking
        
        Args:
            mappings: The mapping information between Java and Python files.
            python_changes: The list of changed Python files.
            since_commit: The commit to compare since.
            
        Returns:
            A formatted GitHub checklist.
        """
        if not mappings:
            return "✅ All Java changes appear to be in sync with Python!"
        
        total_items = 0
        completed_items = 0
        
        output = f"## Java to Python Sync Required\n\n"
        
        for mapping in mappings:
            java_file = mapping['java_file']
            python_files = mapping['python_files']
            
            output += f"### 📁 `{java_file}`\n"
            for python_file in python_files:
                total_items += 1
                exists = self.check_python_file_exists(python_file)
                is_up_to_date = self.is_python_file_up_to_date(java_file, python_file, since_commit)
                
                if is_up_to_date:
                    checkbox = "- [x]"
                    status = "🔄 UPDATED"
                    completed_items += 1
                elif exists:
                    checkbox = "- [ ]"
                    status = "⏳ NEEDS UPDATE"
                else:
                    checkbox = "- [ ]"
                    status = "❌ CREATE NEW"
                
                output += f"{checkbox} **{status}**: `{python_file}`\n"
            output += "\n"
        
        # Update the header with completion count
        output = output.replace(
            "## Java to Python Sync Required\n\n",
            f"## Java to Python Sync Required ({completed_items}/{total_items} completed)\n\n"
        )
        
        if completed_items > 0:
            output += f"\n✅ **Progress**: {completed_items} of {total_items} items completed\n"
        
        return output

    def save_enhanced_report(self, mappings, python_changes, total_items, completed_items):
        """Save enhanced report with completion tracking
        
        Args:
            mappings: The mapping information between Java and Python files.
            python_changes: The list of changed Python files.
            total_items: The total number of sync items.
            completed_items: The number of completed sync items.
            
        Returns:
            None
        """
        report_data = {
            "timestamp": datetime.now().isoformat(),
            "summary": {
                "total_java_files": len(mappings),
                "total_sync_items": total_items,
                "completed_items": completed_items,
                "completion_percentage": (
                    round((completed_items / total_items) * 100, 1)
                    if total_items > 0 else 100
                )
            },
            "mappings": mappings,
            "python_changes": python_changes,
            "details": []
        }
        
        for mapping in mappings:
            java_file = mapping['java_file']
            for python_file in mapping['python_files']:
                report_data["details"].append({
                    "java_file": java_file,
                    "python_file": python_file,
                    "exists": self.check_python_file_exists(python_file),
                    "recently_modified": python_file in python_changes,
                    "status": "completed" if python_file in python_changes else "pending"
                })
        
        report_file = self.root_dir / "tools" / f"sync-report-{datetime.now().strftime('%Y%m%d-%H%M%S')}.json"
        with open(report_file, 'w') as f:
            json.dump(report_data, f, indent=2)

        print(f"\nDetailed report saved to: {report_file}")

    def get_mapping_for_file(self, java_file):
        """Get the mapping configuration for a specific Java file
        
        Args:
            java_file: The Java file path to get the mapping for.
            
        Returns:
            A mapping configuration for the Java file, or None if not found.
        """
        # First check exact matches in critical files
        if "critical_files" in self.mappings:
            for exact_file, mapping in self.mappings["critical_files"].items():
                if java_file == exact_file:
                    return {
                        "python_files": [mapping["python_file"]],
                        "sync_priority": mapping.get("sync_priority", "medium"),
                        "description": mapping.get("description", ""),
                        "auto_sync": mapping.get("auto_sync", False)
                    }

        # Then check directory mappings
        if "directory_mappings" in self.mappings:
            for dir_pattern, mapping in self.mappings["directory_mappings"].items():
                if java_file.startswith(dir_pattern):
                    # Convert the java file path using the mapping
                    relative_path = java_file[len(dir_pattern):]
                    if relative_path.startswith('/'):
                        relative_path = relative_path[1:]
                    
                    python_base = mapping['python_directory']
                    python_file = python_base + self._convert_to_python_naming(relative_path)
                    
                    return {
                        "python_files": [python_file],
                        "sync_priority": mapping.get("sync_priority", "medium"),
                        "description": f"Directory mapping for {java_file}",
                        "auto_sync": mapping.get("auto_sync", True)
                    }

        # Fallback: auto-generate if enabled
        if self.mappings.get("auto_generate_unmapped", True):
            auto_mapping = self._auto_generate_mapping(java_file)
            return {
                "python_files": [auto_mapping["python_file"]],
                "sync_priority": auto_mapping.get("sync_priority", "low"),
                "description": auto_mapping.get("description", "Auto-generated mapping"),
                "auto_sync": auto_mapping.get("auto_sync", True)
            }

        return None

    def _to_snake_case(self, camel_str):
        """Convert CamelCase to snake_case
        
        Args:
            camel_str: camel case string to convert to snake case
            
        Returns:
            snake_case string representation of camel_str
        """
        import re
        s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', camel_str)
        return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()


def main():
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Java-Python synchronization checker',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --format console --since origin/main
  %(prog)s --health-check
  %(prog)s --validate-only
        """
    )
    
    parser.add_argument('--format', choices=['console', 'github-checklist', 'json'],
                        default='console', help='Output format')
    parser.add_argument('--since', default='HEAD~1',
                        help='Compare changes since this commit/branch')
    parser.add_argument('--health-check', action='store_true',
                        help='Perform comprehensive health check')
    parser.add_argument('--validate-only', action='store_true',
                        help='Only validate configuration, don\'t check changes')
    
    args = parser.parse_args()
    
    syncer = JavaPythonSyncer()
    
    if args.health_check:
        success = syncer.health_check()
        return 0 if success else 1
    
    if args.validate_only:
        issues = syncer.validate_configuration()
        if issues:
            print("❌ Configuration validation failed:")
            for issue in issues:
                print(f"   - {issue}")
            return 1
        else:
            print("✅ Configuration validation passed")
            return 0
    
    # Default: generate sync report
    result = syncer.generate_sync_report(output_format=args.format, since_commit=args.since)
    
    if args.format == "github-checklist":
        print(result)
    
    return 0


if __name__ == "__main__":
    main()
