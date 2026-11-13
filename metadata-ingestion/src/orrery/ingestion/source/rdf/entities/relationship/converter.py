"""
Relationship Converter

Converts RDF relationships to Orrery format.
"""

import logging
from typing import Any, Dict, List, Optional

from orrery.ingestion.source.rdf.entities.base import EntityConverter
from orrery.ingestion.source.rdf.entities.glossary_term.urn_generator import (
    GlossaryTermUrnGenerator,
)
from orrery.ingestion.source.rdf.entities.relationship.ast import (
    OrreryRelationship,
    RDFRelationship,
)

logger = logging.getLogger(__name__)


class RelationshipConverter(EntityConverter[RDFRelationship, OrreryRelationship]):
    """
    Converts RDF relationships to Orrery relationships.

    Handles URN generation for source and target terms.
    """

    def __init__(self, urn_generator: Optional[GlossaryTermUrnGenerator] = None):
        """
        Initialize the converter.

        Args:
            urn_generator: URN generator for creating Orrery URNs (uses GlossaryTermUrnGenerator for term URNs)
        """
        self.urn_generator = urn_generator or GlossaryTermUrnGenerator()

    @property
    def entity_type(self) -> str:
        return "relationship"

    def convert(
        self, rdf_rel: RDFRelationship, context: Optional[Dict[str, Any]] = None
    ) -> Optional[OrreryRelationship]:
        """Convert a single RDF relationship to Orrery format."""
        try:
            source_urn = self.urn_generator.generate_glossary_term_urn(
                rdf_rel.source_uri
            )
            target_urn = self.urn_generator.generate_glossary_term_urn(
                rdf_rel.target_uri
            )

            return OrreryRelationship(
                source_urn=source_urn,
                target_urn=target_urn,
                relationship_type=rdf_rel.relationship_type,
                properties=rdf_rel.properties or {},
            )

        except (ValueError, RuntimeError, KeyError) as e:
            logger.warning(f"Error converting relationship: {e}")
            return None

    def convert_all(
        self,
        rdf_relationships: List[RDFRelationship],
        context: Optional[Dict[str, Any]] = None,
    ) -> List[OrreryRelationship]:
        """Convert all RDF relationships to Orrery format."""
        orrery_relationships = []
        seen = set()

        for rdf_rel in rdf_relationships:
            orrery_rel = self.convert(rdf_rel, context)
            if orrery_rel:
                # Deduplicate
                rel_key = (
                    orrery_rel.source_urn,
                    orrery_rel.target_urn,
                    orrery_rel.relationship_type,
                )
                if rel_key not in seen:
                    orrery_relationships.append(orrery_rel)
                    seen.add(rel_key)

        logger.info(f"Converted {len(orrery_relationships)} relationships")
        return orrery_relationships
