# OpenToken Metadata Format Documentation

## Overview

OpenToken generates metadata files alongside the output files to provide information about the processed data. These metadata files contain statistical information about the processed records like the invalid attributes, number of records processed and system/runtime information.

## Metadata Format

The metadata is stored in JSON format with the extension `.metadata.json` and contains key-value pairs representing various aspects of the processing.

## Metadata Fields

| Field | Description |
|-------|-------------|
| `JavaVersion` | The version of Java used to run the processing |
| `OpenTokenVersion` | The version of the OpenToken library |
| `Platform` | The platform/environment used for processing |
| `TotalRows` | The total number of records processed |
| `TotalRowsWithInvalidAttributes` | The number of records that contained one or more invalid attributes |
| `InvalidAttributesByType` | A breakdown of invalid attributes by type, showing the count for each attribute type |

## Example

```json
{
    "Platform": "Java",
    "JavaVersion": "11.0.27",
    "OpenTokenVersion": "1.7.0",
    "TotalRows": "101",
    "TotalRowsWithInvalidAttributes": "9",
    "InvalidAttributesByType": {"SocialSecurityNumber": 2, "FirstName": 1, "PostalCode": 1, "LastName": 2, "BirthDate": 3}
}