"""
Copyright (c) Truveta. All rights reserved.
"""

import os
import tempfile

import pytest

from opentoken_cli.io.parquet.token_parquet_writer import TokenParquetWriter
from opentoken_cli.io.parquet.token_parquet_reader import TokenParquetReader
from opentoken_cli.processor.token_constants import TokenConstants


class TestTokenParquetWriter:
    """Test cases for TokenParquetWriter."""

    def setup_method(self):
        """Set up test fixtures before each test method."""
        self.temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.parquet', delete=False)
        self.temp_file_path = self.temp_file.name
        self.temp_file.close()
        self.writer = TokenParquetWriter(self.temp_file_path)

    def teardown_method(self):
        """Clean up after each test method."""
        if self.writer:
            self.writer.close()
        if os.path.exists(self.temp_file_path):
            os.unlink(self.temp_file_path)

    def test_write_single_token(self):
        """Test writing a single token to Parquet."""
        data = {
            TokenConstants.RULE_ID: "T1",
            TokenConstants.TOKEN: "abc123token",
            TokenConstants.RECORD_ID: "rec-001"
        }

        self.writer.write_token(data)
        self.writer.close()

        reader = TokenParquetReader(self.temp_file_path)
        record = next(reader)

        assert record[TokenConstants.RULE_ID] == "T1"
        assert record[TokenConstants.TOKEN] == "abc123token"
        assert record[TokenConstants.RECORD_ID] == "rec-001"

        with pytest.raises(StopIteration):
            next(reader)

        reader.close()

    def test_write_multiple_tokens(self):
        """Test writing multiple tokens to Parquet."""
        data1 = {
            TokenConstants.RULE_ID: "T1",
            TokenConstants.TOKEN: "token1",
            TokenConstants.RECORD_ID: "rec-001"
        }

        data2 = {
            TokenConstants.RULE_ID: "T2",
            TokenConstants.TOKEN: "token2",
            TokenConstants.RECORD_ID: "rec-002"
        }

        self.writer.write_token(data1)
        self.writer.write_token(data2)
        self.writer.close()

        reader = TokenParquetReader(self.temp_file_path)
        
        record1 = next(reader)
        assert record1[TokenConstants.RULE_ID] == "T1"
        assert record1[TokenConstants.TOKEN] == "token1"
        assert record1[TokenConstants.RECORD_ID] == "rec-001"

        record2 = next(reader)
        assert record2[TokenConstants.RULE_ID] == "T2"
        assert record2[TokenConstants.TOKEN] == "token2"
        assert record2[TokenConstants.RECORD_ID] == "rec-002"

        with pytest.raises(StopIteration):
            next(reader)

        reader.close()

    def test_write_token_with_blank_value(self):
        """Test writing a token with blank value."""
        data = {
            TokenConstants.RULE_ID: "T1",
            TokenConstants.TOKEN: "",
            TokenConstants.RECORD_ID: "rec-001"
        }

        self.writer.write_token(data)
        self.writer.close()

        reader = TokenParquetReader(self.temp_file_path)
        record = next(reader)

        assert record[TokenConstants.RULE_ID] == "T1"
        assert record[TokenConstants.TOKEN] == ""
        assert record[TokenConstants.RECORD_ID] == "rec-001"

        reader.close()

    def test_write_token_with_missing_fields(self):
        """Test writing a token with missing fields uses empty defaults."""
        data = {
            TokenConstants.RULE_ID: "T1"
            # TOKEN and RECORD_ID missing
        }

        self.writer.write_token(data)
        self.writer.close()

        reader = TokenParquetReader(self.temp_file_path)
        record = next(reader)

        assert record[TokenConstants.RULE_ID] == "T1"
        assert record[TokenConstants.TOKEN] == ""
        assert record[TokenConstants.RECORD_ID] == ""

        reader.close()

    def test_close_with_no_rows_logs_warning(self, caplog):
        """Test that closing without writing any rows logs a warning."""
        import logging
        with caplog.at_level(logging.WARNING):
            self.writer.close()
        
        assert "No rows to write to Parquet file" in caplog.text

    def test_context_manager_usage(self):
        """Test using the writer as a context manager."""
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix='.parquet', delete=False)
        temp_path = temp_file.name
        temp_file.close()

        try:
            with TokenParquetWriter(temp_path) as writer:
                data = {
                    TokenConstants.RULE_ID: "T1",
                    TokenConstants.TOKEN: "ctx_token",
                    TokenConstants.RECORD_ID: "rec-ctx"
                }
                writer.write_token(data)
            
            # Verify file was written
            reader = TokenParquetReader(temp_path)
            record = next(reader)
            assert record[TokenConstants.TOKEN] == "ctx_token"
            reader.close()
        finally:
            if os.path.exists(temp_path):
                os.unlink(temp_path)

    def test_write_error_handling(self):
        """Test error handling when write fails."""
        # Use an invalid path to trigger write error
        invalid_path = "/invalid/directory/that/does/not/exist/file.parquet"
        
        # Override directory creation to simulate the error at write time
        import unittest.mock as mock
        with mock.patch('os.makedirs'):
            writer = TokenParquetWriter(invalid_path)
            writer.write_token({
                TokenConstants.RULE_ID: "T1",
                TokenConstants.TOKEN: "token",
                TokenConstants.RECORD_ID: "rec"
            })
            
            # Closing should raise IOError
            with pytest.raises(IOError) as exc_info:
                writer.close()
            
            assert "Failed to write Parquet file" in str(exc_info.value)

    def test_creates_output_directory(self):
        """Test that the writer creates the output directory if it doesn't exist."""
        temp_dir = tempfile.mkdtemp()
        nested_path = os.path.join(temp_dir, "subdir", "nested", "output.parquet")
        
        try:
            writer = TokenParquetWriter(nested_path)
            data = {
                TokenConstants.RULE_ID: "T1",
                TokenConstants.TOKEN: "token",
                TokenConstants.RECORD_ID: "rec-001"
            }
            writer.write_token(data)
            writer.close()
            
            # Verify directory was created
            assert os.path.exists(os.path.dirname(nested_path))
            assert os.path.exists(nested_path)
            
            # Verify file content
            reader = TokenParquetReader(nested_path)
            record = next(reader)
            assert record[TokenConstants.TOKEN] == "token"
            reader.close()
        finally:
            import shutil
            if os.path.exists(temp_dir):
                shutil.rmtree(temp_dir)
