# src/opentoken/attributes/attribute_loader.py
from typing import Set
from opentoken.attributes.attribute import Attribute
from opentoken.attributes.person.first_name_attribute import FirstNameAttribute
from opentoken.attributes.person.last_name_attribute import LastNameAttribute
from opentoken.attributes.person.birth_date_attribute import BirthDateAttribute
from opentoken.attributes.person.sex_attribute import SexAttribute
from opentoken.attributes.person.social_security_number_attribute import SocialSecurityNumberAttribute
from opentoken.attributes.person.postal_code_attribute import PostalCodeAttribute
from opentoken.attributes.general.record_id_attribute import RecordIdAttribute

class AttributeLoader:
    """Loads all available attribute implementations."""
    
    @staticmethod
    def load() -> Set[Attribute]:
        """Load all attribute implementations."""
        return {
            RecordIdAttribute(),
            FirstNameAttribute(),
            LastNameAttribute(),
            BirthDateAttribute(), 
            SexAttribute(),
            SocialSecurityNumberAttribute(),
            PostalCodeAttribute()
        }