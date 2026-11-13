"""
Relationship Entity Module

Self-contained processing for glossary term relationships:
- Extraction from RDF graphs (skos:broader, skos:narrower only)
- Conversion to Orrery AST
- MCP creation for Orrery ingestion (isRelatedTerms only - inheritance)

Note: Only broader/narrower relationships are supported.
skos:related, skos:exactMatch, skos:closeMatch are NOT extracted.

Both broader and narrower create inheritance relationships (isRelatedTerms):
- broader: child → parent → child inherits from parent
- narrower: parent → child → normalized to child → parent (child inherits from parent)
"""

from orrery.ingestion.source.rdf.entities.base import EntityMetadata
from orrery.ingestion.source.rdf.entities.glossary_term import (
    ENTITY_TYPE as GLOSSARY_TERM_ENTITY_TYPE,
)
from orrery.ingestion.source.rdf.entities.relationship.ast import (
    OrreryRelationship,
    RDFRelationship,
    RelationshipType,
)
from orrery.ingestion.source.rdf.entities.relationship.converter import (
    RelationshipConverter,
)
from orrery.ingestion.source.rdf.entities.relationship.extractor import (
    RelationshipExtractor,
)
from orrery.ingestion.source.rdf.entities.relationship.mcp_builder import (
    RelationshipMCPBuilder,
)

# Entity type constant - part of the module contract
ENTITY_TYPE = "relationship"

ENTITY_METADATA = EntityMetadata(
    entity_type=ENTITY_TYPE,
    cli_names=["relationship", "relationships"],
    rdf_ast_class=RDFRelationship,
    orrery_ast_class=OrreryRelationship,
    export_targets=["pretty_print", "file", "orrery"],
    dependencies=[
        GLOSSARY_TERM_ENTITY_TYPE
    ],  # Depends on glossary terms (relationships reference terms)
)

__all__ = [
    "ENTITY_TYPE",
    "RelationshipExtractor",
    "RelationshipConverter",
    "RelationshipMCPBuilder",
    "RDFRelationship",
    "OrreryRelationship",
    "RelationshipType",
    "ENTITY_METADATA",
]
