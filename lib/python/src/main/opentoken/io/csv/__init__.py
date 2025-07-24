"""
CSV subpackage for person attribute I/O in the OpenToken project.
"""

from opentoken.io.csv.person_attributes_csv_reader import PersonAttributesCSVReader
from opentoken.io.csv.person_attributes_csv_writer import PersonAttributesCSVWriter

# Optionally, if you want to support the alternate (capitalized) filenames:
# from opentoken.io.csv.person_attributes_CSV_reader import PersonAttributesCSVReader as AltPersonAttributesCSVReader
# from opentoken.io.csv.person_attributes_CSV_writer import PersonAttributesCSVWriter as AltPersonAttributesCSVWriter