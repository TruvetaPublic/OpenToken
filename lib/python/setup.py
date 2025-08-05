#!/usr/bin/env python3
"""Setup script for OpenToken Python package."""

from setuptools import setup
import os

# Read the contents of README file
this_directory = os.path.abspath(os.path.dirname(__file__))
with open(os.path.join(this_directory, "README.md"), encoding="utf-8") as f:
    long_description = f.read()

# Read requirements from requirements.txt
with open(os.path.join(this_directory, "requirements.txt"), encoding="utf-8") as f:
    requirements = [line.strip() for line in f if line.strip() and not line.startswith("#")]

setup(
    name="opentoken",
    version="1.9.3",
    author="Truveta",
    description="OpenToken Python implementation for person matching",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/Truveta/OpenToken",
    project_urls={
        "Source": "https://github.com/Truveta/OpenToken",
        "Documentation": "https://github.com/Truveta/OpenToken/blob/main/README.md",
    },
    python_requires=">=3.9",
    install_requires=requirements,
    extras_require={
        "dev": [
            "csv2parquet",
            "pandas",
            "pyarrow",
            "cryptography"
        ],
        "test": [
            "pytest"
        ],
    },
    entry_points={
        "console_scripts": [
            "opentoken=opentoken.main:main",
        ],
    },
)
