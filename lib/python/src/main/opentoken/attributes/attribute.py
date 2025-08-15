from abc import ABC, abstractmethod
from typing import List


class Attribute(ABC):
    """Base interface for all attributes."""

    @abstractmethod
    def get_name(self) -> str:
        """Get the name of the attribute."""
        pass

    @abstractmethod
    def get_aliases(self) -> List[str]:
        """Get the aliases for the attribute."""
        pass

    @abstractmethod
    def normalize(self, value: str) -> str:
        """Normalize the attribute value."""
        pass

    @abstractmethod
    def validate(self, value: str) -> bool:
        """Validate the attribute value."""
        pass
