"""
AST classes for Domain entity.

Defines Orrery AST representation for domains.
"""

from dataclasses import dataclass, field
from typing import TYPE_CHECKING, List, Optional

# Orrery SDK imports
from orrery.utilities.urns.domain_urn import DomainUrn

# Forward references to avoid circular imports
if TYPE_CHECKING:
    from orrery.ingestion.source.rdf.entities.glossary_term.ast import (
        OrreryGlossaryTerm,
    )


@dataclass
class OrreryDomain:
    """Internal representation of a Orrery domain (for glossary terms)."""

    path_segments: List[str]  # Hierarchical path segments from IRI
    urn: DomainUrn  # Orrery domain URN
    name: str  # Domain name (last segment)
    parent_domain_urn: Optional[DomainUrn] = None  # Parent domain URN for hierarchy
    glossary_terms: List["OrreryGlossaryTerm"] = field(default_factory=list)
    subdomains: List["OrreryDomain"] = field(default_factory=list)
