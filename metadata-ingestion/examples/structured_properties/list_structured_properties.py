# Usage: python3 list_structured_properties.py
# Expected Output: List of structured properties
# This script lists all structured properties in Orrery
from orrery.api.entities.structuredproperties.structuredproperties import (
    StructuredProperties,
)
from orrery.ingestion.graph.client import get_default_graph

with get_default_graph() as graph:
    structuredproperties = StructuredProperties.list(graph)
    for structuredproperty in structuredproperties:
        print(structuredproperty.dict())
