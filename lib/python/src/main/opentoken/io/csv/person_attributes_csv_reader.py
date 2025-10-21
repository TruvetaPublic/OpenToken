"""
Copyright (c) Truveta. All rights reserved.
"""

import csv
import logging
import uuid
from typing import Dict, Type, Set
from opentoken.io.person_attributes_reader import PersonAttributesReader
from opentoken.attributes.attribute import Attribute
from opentoken.attributes.attribute_loader import AttributeLoader
from opentoken.attributes.general.record_id_attribute import RecordIdAttribute


logger = logging.getLogger(__name__)


class PersonAttributesCSVReader(PersonAttributesReader):
    """
    Reads person attributes from a CSV file.
    Implements the PersonAttributesReader interface.
    """

    def __init__(self, file_path: str):
        """
        Initialize the class with the input file in CSV format.

        Args:
            file_path: The input file path.

        Raises:
            IOError: If an I/O error occurs.
        """
        try:
            self.file_path = file_path
            self.file_handle = open(file_path, 'r', encoding='utf-8')
            self.csv_reader = csv.DictReader(self.file_handle)
            self.iterator = iter(self.csv_reader)
            self.attribute_map: Dict[str, Attribute] = {}
            self.has_record_id = False

            # Load attributes and build the mapping
            attributes: Set[Attribute] = AttributeLoader.load()
            for header_name in self.csv_reader.fieldnames or []:
                for attribute in attributes:
                    for alias in attribute.get_aliases():
                        if header_name.lower() == alias.lower():
                            self.attribute_map[header_name] = attribute
                            if isinstance(attribute, RecordIdAttribute):
                                self.has_record_id = True
                            break

        except IOError as e:
            logger.error(f"Error in reading CSV file: {e}")
            raise

    def __iter__(self):
        """Return the iterator object."""
        return self

    def __next__(self) -> Dict[Type[Attribute], str]:
        """
        Get the next record from the CSV file.

        Returns:
            A person attributes map.
        """
        record = next(self.iterator)

        person_attributes: Dict[Type[Attribute], str] = {}
        for key, value in record.items():
            attribute = self.attribute_map.get(key)
            if attribute is not None:
                person_attributes[type(attribute)] = value
            # else ignore attribute as it's not supported

        # Generate UUID for RecordId if not present in the input
        if not self.has_record_id:
            person_attributes[RecordIdAttribute] = str(uuid.uuid4())

        return person_attributes

    def close(self) -> None:
        """Close the CSV reader and file handle."""
        if self.file_handle:
            self.file_handle.close()
