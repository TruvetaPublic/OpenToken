"""
Copyright (c) Truveta. All rights reserved.
"""

from typing import Dict, List, Type
from unittest.mock import Mock, MagicMock

import pytest

from opentoken.attributes.attribute import Attribute
from opentoken.attributes.general.record_id_attribute import RecordIdAttribute
from opentoken.attributes.person.first_name_attribute import FirstNameAttribute
from opentoken.attributes.person.last_name_attribute import LastNameAttribute
from opentoken.io.person_attributes_reader import PersonAttributesReader
from opentoken.io.person_attributes_writer import PersonAttributesWriter
from opentoken.processor.person_attributes_processor import PersonAttributesProcessor
from opentoken.tokentransformer.hash_token_transformer import HashTokenTransformer
from opentoken.tokentransformer.token_transformer import TokenTransformer
from opentoken.metadata import Metadata


class TestPersonAttributesProcessor:
    """Test cases for PersonAttributesProcessor."""

    def test_process_happy_path(self):
        """Test process happy path."""
        token_transformer_list = [Mock(spec=HashTokenTransformer)]
        data = {
            RecordIdAttribute: "TestRecordId",
            FirstNameAttribute: "John",
            LastNameAttribute: "Spencer"
        }

        reader = Mock(spec=PersonAttributesReader)
        writer = Mock(spec=PersonAttributesWriter)
        reader.__iter__ = Mock(return_value=iter([data]))

        metadata_map = Metadata().initialize()
        PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)

        # Verify writer was called 5 times (5 tokens generated)
        assert writer.write_attributes.call_count == 5

        # Verify metadata was populated
        assert len(metadata_map) > 0, "Metadata map should not be empty after processing"
        assert PersonAttributesProcessor.TOTAL_ROWS in metadata_map, "Metadata should contain totalRows key"

    def test_process_io_exception_writing_attributes(self):
        """Test process with IOException writing attributes."""
        token_transformer_list = [Mock(spec=TokenTransformer)]
        data = {
            RecordIdAttribute: "TestRecordId",
            FirstNameAttribute: "John", 
            LastNameAttribute: "Spencer"
        }

        reader = Mock(spec=PersonAttributesReader)
        writer = Mock(spec=PersonAttributesWriter)
        reader.__iter__ = Mock(return_value=iter([data]))

        # Configure writer to raise IOError (Python equivalent of IOException)
        writer.write_attributes.side_effect = IOError("Test Exception")

        metadata_map = Metadata().initialize()

        PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)

        # Verify writer was called at least once
        assert writer.write_attributes.call_count >= 1

        # Verify metadata was populated
        assert len(metadata_map) > 0, "Metadata map should not be empty after processing"
        assert "TotalRows" in metadata_map, "Metadata should contain totalRows key"

    def test_metadata_map_contains_correct_values(self):
        """Test metadata map contains correct values."""
        token_transformer_list = [Mock(spec=HashTokenTransformer)]
        data = {
            RecordIdAttribute: "TestRecordId",
            FirstNameAttribute: "John",
            LastNameAttribute: "Spencer"
        }

        reader = Mock(spec=PersonAttributesReader)
        writer = Mock(spec=PersonAttributesWriter)
        reader.__iter__ = Mock(return_value=iter([data]))

        metadata_map = Metadata().initialize()

        PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)

        # Check that the metadata map contains all expected keys with correct values
        assert PersonAttributesProcessor.TOTAL_ROWS in metadata_map, "Metadata should contain totalRows key"
        assert PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES in metadata_map, "Metadata should contain totalRowsWithInvalidAttributes key"
        assert PersonAttributesProcessor.INVALID_ATTRIBUTES_BY_TYPE in metadata_map, "Metadata should contain invalidAttributesByType key"

        # Verify values
        assert metadata_map[PersonAttributesProcessor.TOTAL_ROWS] == 1, "Total rows should be 1"
        assert metadata_map[PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES] == 0, "Total rows with invalid attributes should be 0"

        # The invalid attributes map should be an empty Map object
        invalid_attributes_map = metadata_map[PersonAttributesProcessor.INVALID_ATTRIBUTES_BY_TYPE]
        assert len(invalid_attributes_map) == 0, "Invalid attributes map should be empty"

    def test_metadata_map_multiple_rows(self):
        """Test metadata map multiple rows."""
        token_transformer_list = [Mock(spec=HashTokenTransformer)]

        # Create three data records
        data1 = {
            RecordIdAttribute: "TestRecordId1",
            FirstNameAttribute: "John",
            LastNameAttribute: "Spencer"
        }
        data2 = {
            RecordIdAttribute: "TestRecordId2",
            FirstNameAttribute: "Jane",
            LastNameAttribute: "Doe"
        }
        data3 = {
            RecordIdAttribute: "TestRecordId3",
            FirstNameAttribute: "Alex",
            LastNameAttribute: "Smith"
        }

        reader = Mock(spec=PersonAttributesReader)
        writer = Mock(spec=PersonAttributesWriter)
        reader.__iter__ = Mock(return_value=iter([data1, data2, data3]))

        metadata_map = Metadata().initialize()

        # Execute
        PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)

        # Verify
        assert metadata_map[PersonAttributesProcessor.TOTAL_ROWS] == 3, "Total rows should be 3"
        assert metadata_map[PersonAttributesProcessor.TOTAL_ROWS_WITH_INVALID_ATTRIBUTES] == 0, "Total rows with invalid attributes should be 0"

    def test_metadata_map_preserves_existing_entries(self):
        """Test metadata map preserves existing entries."""
        token_transformer_list = [Mock(spec=HashTokenTransformer)]
        data = {
            RecordIdAttribute: "TestRecordId",
            FirstNameAttribute: "John",
            LastNameAttribute: "Spencer"
        }

        reader = Mock(spec=PersonAttributesReader)
        writer = Mock(spec=PersonAttributesWriter)
        reader.__iter__ = Mock(return_value=iter([data]))

        metadata_map = Metadata().initialize()
        metadata_map["ExistingKey1"] = "ExistingValue1"
        metadata_map["ExistingKey2"] = "ExistingValue2"

        PersonAttributesProcessor.process(reader, writer, token_transformer_list, metadata_map)

        # Verify original entries are preserved
        assert "ExistingKey1" in metadata_map, "Metadata should preserve existing key1"
        assert "ExistingKey2" in metadata_map, "Metadata should preserve existing key2"
        assert metadata_map["ExistingKey1"] == "ExistingValue1", "Value for existing key1 should be preserved"
        assert metadata_map["ExistingKey2"] == "ExistingValue2", "Value for existing key2 should be preserved"

        # And new entries are added
        assert "TotalRows" in metadata_map, "Metadata should contain totalRows key"