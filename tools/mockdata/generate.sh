#!/bin/bash

if ! pip show faker > /dev/null 2>&1; then
    pip install faker
fi

python data_generator.py 1000 0.05 test_data.csv
