"""
Generate superhero-themed datasets for PPRL demonstration.

This script creates two datasets:
1. Hospital dataset: 100 super heroes
2. Pharmacy dataset: 120 super heroes with 40% overlap from hospital dataset

Both datasets share the same person attributes (name, birthdate, SSN, etc.) for overlapping records.
"""
import csv
import random
from datetime import datetime, timedelta
import uuid
import sys

# Super Hero names for thematic data
SUPERHERO_FIRST_NAMES = [
    "Bruce", "Clark", "Diana", "Peter", "Tony", "Steve", "Natasha", "Thor", 
    "Bruce", "Barry", "Hal", "Oliver", "Arthur", "Victor", "Kara", "Barbara",
    "Wade", "Logan", "Scott", "Jean", "Ororo", "Kurt", "Hank", "Bobby",
    "Carol", "Monica", "Jessica", "Luke", "Danny", "Matt", "Frank", "Clint",
    "Wanda", "Pietro", "Vision", "Rhodey", "Sam", "Bucky", "T'Challa", "Shuri",
    "Stephen", "Wong", "Groot", "Rocket", "Gamora", "Drax", "Mantis", "Nebula",
    "Thanos", "Loki", "Hela", "Odin", "Frigga", "Heimdall", "Sif", "Valkyrie",
    "Nick", "Maria", "Phil", "Peggy", "Howard", "Hank", "Janet", "Hope",
    "Scott", "Luis", "Dave", "Kurt", "Jimmy", "Darcy", "Erik", "Jane",
    "Pepper", "Happy", "Rhodey", "May", "Ned", "MJ", "Flash", "Betty",
    "Gwen", "Harry", "Norman", "Otto", "Curt", "Max", "Eddie", "Cletus",
    "Reed", "Sue", "Johnny", "Ben", "Franklin", "Valeria", "Alicia", "Wyatt",
    "Kamala", "Iman", "Bruno", "Nakia", "Muneeba", "Yusuf", "Aamir", "Tyesha"
]

SUPERHERO_LAST_NAMES = [
    "Wayne", "Kent", "Prince", "Parker", "Stark", "Rogers", "Romanoff", "Odinson",
    "Banner", "Allen", "Jordan", "Queen", "Curry", "Stone", "Danvers", "Gordon",
    "Wilson", "Howlett", "Summers", "Grey", "Munroe", "Wagner", "McCoy", "Drake",
    "Danvers", "Rambeau", "Jones", "Cage", "Rand", "Murdock", "Castle", "Barton",
    "Maximoff", "Maximoff", "Vision", "Rhodes", "Wilson", "Barnes", "T'Challa", "Udaku",
    "Strange", "Wong", "Groot", "Rocket", "Gamora", "Drax", "Mantis", "Nebula",
    "Titan", "Laufeyson", "Odinsdottir", "Borson", "Frigga", "Heimdall", "Sif", "Valkyrie",
    "Fury", "Hill", "Coulson", "Carter", "Stark", "Pym", "VanDyne", "VanDyne",
    "Lang", "Luis", "Dave", "Kurt", "Woo", "Lewis", "Selvig", "Foster",
    "Potts", "Hogan", "Rhodes", "Parker", "Leeds", "Watson", "Thompson", "Brant",
    "Stacy", "Osborn", "Osborn", "Octavius", "Connors", "Dillon", "Brock", "Kasady",
    "Richards", "Storm", "Storm", "Grimm", "Storm", "Richards", "Masters", "Wingfoot",
    "Khan", "Vellani", "Carrelli", "Bahadir", "Khan", "Khan", "Khan", "Hillman"
]

# Additional hospital-specific data
HOSPITAL_DEPARTMENTS = [
    "Emergency Department",
    "Intensive Care Unit",
    "Trauma Center",
    "Surgery Department",
    "Super Powers Rehabilitation",
    "Mutation Specialist Unit",
    "Cosmic Ray Treatment Center",
    "Alien Biology Department"
]

HOSPITAL_VISIT_REASONS = [
    "Power Malfunction",
    "Cosmic Ray Exposure",
    "Alien Parasite Removal",
    "Strength Enhancement Surgery",
    "Flight Control Issue",
    "Telepathy Headache",
    "Laser Vision Calibration",
    "Routine Super Physical",
    "Battle Injury",
    "Villain Encounter Trauma"
]

# Pharmacy-specific data
PHARMACY_MEDICATION_TYPES = [
    "Power Suppressant",
    "Super Serum",
    "Mutation Stabilizer",
    "Energy Boost Formula",
    "Healing Accelerator",
    "Strength Enhancer",
    "Telepathy Blocker",
    "Invisibility Serum",
    "Flight Stabilizer",
    "Super Vitamin Complex"
]

PHARMACY_PRESCRIPTION_TYPES = [
    "Daily Maintenance",
    "Emergency Use",
    "Pre-Mission Prep",
    "Post-Battle Recovery",
    "Chronic Power Management",
    "Acute Power Surge",
    "Preventive Care",
    "Experimental Treatment"
]


def generate_ssn():
    """Generate a valid-looking SSN that passes OpenToken validation."""
    # Avoid 000, 666, and 900-999 for area
    area = random.choice([str(i).zfill(3) for i in range(1, 900) if i != 666])
    # Avoid 00 for group
    group = str(random.randint(1, 99)).zfill(2)
    # Avoid 0000 for serial
    serial = str(random.randint(1, 9999)).zfill(4)
    return f"{area}-{group}-{serial}"


def generate_birthdate():
    """Generate a birthdate between 1910 and today (valid for OpenToken)."""
    start_date = datetime(1950, 1, 1)
    end_date = datetime.now() - timedelta(days=365 * 18)  # At least 18 years old
    random_days = random.randint(0, (end_date - start_date).days)
    birth_date = start_date + timedelta(days=random_days)
    return birth_date.strftime("%Y-%m-%d")


def generate_zipcode():
    """Generate a valid US ZIP code."""
    # Avoid placeholder values like 00000, 11111, etc.
    valid_zipcodes = []
    for i in range(10000, 99999):
        zip_str = str(i)
        if zip_str not in ['11111', '12345', '54321', '98765'] and not zip_str.startswith('000'):
            valid_zipcodes.append(zip_str)
    return random.choice(valid_zipcodes[:1000])  # Use a subset for efficiency


def generate_person():
    """Generate a person record with all required attributes."""
    person = {
        'RecordId': str(uuid.uuid4()),
        'FirstName': random.choice(SUPERHERO_FIRST_NAMES),
        'LastName': random.choice(SUPERHERO_LAST_NAMES),
        'Sex': random.choice(['Male', 'Female']),
        'BirthDate': generate_birthdate(),
        'SocialSecurityNumber': generate_ssn(),
        'PostalCode': generate_zipcode()
    }
    return person


def generate_hospital_dataset(num_records, output_file):
    """Generate hospital dataset with additional hospital-specific columns."""
    print(f"Generating hospital dataset with {num_records} records...")
    
    with open(output_file, 'w', newline='') as csvfile:
        # Hospital includes department and visit reason
        fieldnames = ['RecordId', 'FirstName', 'LastName', 'Sex', 'BirthDate', 
                     'SocialSecurityNumber', 'PostalCode', 'Department', 'VisitReason']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        
        records = []
        for i in range(num_records):
            person = generate_person()
            person['Department'] = random.choice(HOSPITAL_DEPARTMENTS)
            person['VisitReason'] = random.choice(HOSPITAL_VISIT_REASONS)
            writer.writerow(person)
            records.append(person)
            
            if (i + 1) % 20 == 0:
                print(f"  Generated {i + 1} hospital records...")
    
    print(f"Hospital dataset saved to {output_file}")
    return records


def generate_pharmacy_dataset(num_records, overlap_records, output_file):
    """Generate pharmacy dataset with overlapping records from hospital."""
    print(f"Generating pharmacy dataset with {num_records} records (including {len(overlap_records)} overlapping)...")
    
    num_unique = num_records - len(overlap_records)
    
    with open(output_file, 'w', newline='') as csvfile:
        # Pharmacy includes medication type and prescription type
        fieldnames = ['RecordId', 'FirstName', 'LastName', 'Sex', 'BirthDate', 
                     'SocialSecurityNumber', 'PostalCode', 'MedicationType', 'PrescriptionType']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        
        # Write overlapping records first (same person attributes, different RecordId and org columns)
        for i, person in enumerate(overlap_records):
            pharmacy_person = {
                'RecordId': str(uuid.uuid4()),  # New RecordId for pharmacy system
                'FirstName': person['FirstName'],
                'LastName': person['LastName'],
                'Sex': person['Sex'],
                'BirthDate': person['BirthDate'],
                'SocialSecurityNumber': person['SocialSecurityNumber'],
                'PostalCode': person['PostalCode'],
                'MedicationType': random.choice(PHARMACY_MEDICATION_TYPES),
                'PrescriptionType': random.choice(PHARMACY_PRESCRIPTION_TYPES)
            }
            writer.writerow(pharmacy_person)
            
            if (i + 1) % 20 == 0:
                print(f"  Written {i + 1} overlapping records...")
        
        # Generate unique pharmacy records
        for i in range(num_unique):
            person = generate_person()
            person['MedicationType'] = random.choice(PHARMACY_MEDICATION_TYPES)
            person['PrescriptionType'] = random.choice(PHARMACY_PRESCRIPTION_TYPES)
            writer.writerow(person)
            
            if (i + 1) % 20 == 0:
                print(f"  Generated {i + 1} unique pharmacy records...")
    
    print(f"Pharmacy dataset saved to {output_file}")


def main():
    """Main function to generate both datasets."""
    # Configuration
    num_hospital = 100
    num_pharmacy = 120
    overlap_percentage = 0.40
    num_overlap = int(num_hospital * overlap_percentage)  # 40 records
    
    print("=" * 60)
    print("Superhero PPRL Dataset Generator")
    print("=" * 60)
    print(f"Hospital records: {num_hospital}")
    print(f"Pharmacy records: {num_pharmacy}")
    print(f"Overlap: {num_overlap} records ({overlap_percentage * 100}%)")
    print("=" * 60)
    print()
    
    # Set random seed for reproducibility
    random.seed(42)
    
    # Generate hospital dataset
    hospital_records = generate_hospital_dataset(
        num_hospital,
        '../datasets/hospital_superhero_data.csv'
    )
    
    print()
    
    # Select random records from hospital for overlap
    overlap_records = random.sample(hospital_records, num_overlap)
    
    # Generate pharmacy dataset with overlap
    generate_pharmacy_dataset(
        num_pharmacy,
        overlap_records,
        '../datasets/pharmacy_superhero_data.csv'
    )
    
    print()
    print("=" * 60)
    print("Dataset generation complete!")
    print("=" * 60)
    print(f"Expected matching records after tokenization: {num_overlap}")
    print("Note: All 5 tokens (T1-T5) must match for a record to be considered a match")


if __name__ == '__main__':
    main()
