"""
AST classes for Glossary Term entity.

Defines Orrery AST representation for glossary terms.
Extractor now returns Orrery AST directly, eliminating the RDF AST layer.
"""

from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional


@dataclass
class OrreryGlossaryTerm:
    """Internal representation of a Orrery glossary term."""

    urn: str  # Use string for now since GlossaryTermUrn doesn't exist
    name: str
    definition: Optional[str] = None
    source: Optional[str] = None
    custom_properties: Dict[str, Any] = field(default_factory=dict)
    path_segments: List[str] = field(default_factory=list)  # Hierarchical path from IRI
