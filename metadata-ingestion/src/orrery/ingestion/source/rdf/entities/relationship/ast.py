"""
AST classes for Relationship entity.

Defines RDF and Orrery AST representations for relationships.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict


class RelationshipType(Enum):
    """Types of relationships between entities."""

    BROADER = "broader"
    NARROWER = "narrower"


@dataclass
class RDFRelationship:
    """Represents a relationship between RDF entities."""

    source_uri: str
    target_uri: str
    relationship_type: RelationshipType
    properties: Dict[str, Any] = field(default_factory=dict)


@dataclass
class OrreryRelationship:
    """Internal representation of a Orrery relationship."""

    source_urn: str
    target_urn: str
    relationship_type: RelationshipType
    properties: Dict[str, Any] = field(default_factory=dict)
