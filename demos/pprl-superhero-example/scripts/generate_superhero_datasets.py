#!/usr/bin/env python3
"""
Data Generation Script for PPRL Superhero Demo
Creates two datasets (hospital and pharmacy) with controlled overlap.
"""
import pandas as pd
import numpy as np
import sys
import os
import random

def generate_superhero_datasets(hospital_path, pharmacy_path, overlap_pct=0.4, seed=42):
    np.random.seed(seed)
    random.seed(seed)
    # Superhero names
    names = [
        "Clark Kent", "Bruce Wayne", "Diana Prince", "Barry Allen", "Hal Jordan",
        "Arthur Curry", "Victor Stone", "Oliver Queen", "Billy Batson", "Kara Zor-El",
        "Selina Kyle", "Pamela Isley", "Harleen Quinzel", "Lex Luthor", "Lois Lane",
        "Jimmy Olsen", "Alfred Pennyworth", "Lucius Fox", "Commissioner Gordon", "Amanda Waller"
    ]
    n_hospital = 100
    n_pharmacy = 120
    n_overlap = int(n_hospital * overlap_pct)
    # Overlap records
    overlap_records = []
    for i in range(n_overlap):
        name = random.choice(names)
        birthdate = f"19{random.randint(50,99)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}"
        ssn = f"{random.randint(100,999)}-{random.randint(10,99)}-{random.randint(1000,9999)}"
        sex = random.choice(["M", "F"])
        zip_code = f"{random.randint(10000,99999)}"
        overlap_records.append({
            "name": name,
            "birthdate": birthdate,
            "ssn": ssn,
            "sex": sex,
            "zip_code": zip_code
        })
    # Unique hospital records
    hospital_records = overlap_records.copy()
    for i in range(n_hospital - n_overlap):
        name = random.choice(names)
        birthdate = f"19{random.randint(50,99)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}"
        ssn = f"{random.randint(100,999)}-{random.randint(10,99)}-{random.randint(1000,9999)}"
        sex = random.choice(["M", "F"])
        zip_code = f"{random.randint(10000,99999)}"
        hospital_records.append({
            "name": name,
            "birthdate": birthdate,
            "ssn": ssn,
            "sex": sex,
            "zip_code": zip_code
        })
    # Unique pharmacy records
    pharmacy_records = overlap_records.copy()
    for i in range(n_pharmacy - n_overlap):
        name = random.choice(names)
        birthdate = f"19{random.randint(50,99)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}"
        ssn = f"{random.randint(100,999)}-{random.randint(10,99)}-{random.randint(1000,9999)}"
        sex = random.choice(["M", "F"])
        zip_code = f"{random.randint(10000,99999)}"
        pharmacy_records.append({
            "name": name,
            "birthdate": birthdate,
            "ssn": ssn,
            "sex": sex,
            "zip_code": zip_code
        })
    pd.DataFrame(hospital_records).to_csv(hospital_path, index=False)
    pd.DataFrame(pharmacy_records).to_csv(pharmacy_path, index=False)

if __name__ == "__main__":
    hospital_path = sys.argv[1] if len(sys.argv) > 1 else "../hospital.csv"
    pharmacy_path = sys.argv[2] if len(sys.argv) > 2 else "../pharmacy.csv"
    generate_superhero_datasets(hospital_path, pharmacy_path)
    print(f"Datasets generated: {hospital_path}, {pharmacy_path}")
