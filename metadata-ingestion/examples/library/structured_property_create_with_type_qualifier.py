from orrery.api.entities.structuredproperties.structuredproperties import (
    StructuredProperties,
    TypeQualifierAllowedTypes,
)
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

# Create a Orrery client
client = OrreryGraph(OrreryClientConfig(server="http://localhost:8080"))

# Define a structured property that references Orrery entities
# This property can only reference CorpUser or CorpGroup entities
data_steward_property = StructuredProperties(
    id="io.orrery.governance.dataSteward",
    qualified_name="io.orrery.governance.dataSteward",
    display_name="Data Steward",
    type="urn",
    description="The designated data steward responsible for this asset's governance and quality",
    entity_types=["dataset", "dashboard", "chart", "dataJob"],
    cardinality="SINGLE",
    type_qualifier=TypeQualifierAllowedTypes(
        allowed_types=[
            "urn:li:entityType:orrery.corpuser",
            "urn:li:entityType:orrery.corpGroup",
        ]
    ),
)

# Emit the structured property to Orrery
for mcp in data_steward_property.generate_mcps():
    client.emit_mcp(mcp)

print(f"Created structured property: {data_steward_property.urn}")

# Example: Create a multi-value property for related datasets
related_datasets_property = StructuredProperties(
    id="io.orrery.lineage.relatedDatasets",
    qualified_name="io.orrery.lineage.relatedDatasets",
    display_name="Related Datasets",
    type="urn",
    description="Other datasets that are semantically or functionally related to this asset",
    entity_types=["dataset"],
    cardinality="MULTIPLE",
    type_qualifier=TypeQualifierAllowedTypes(
        allowed_types=["urn:li:entityType:orrery.dataset"]
    ),
)

# Emit the second structured property
for mcp in related_datasets_property.generate_mcps():
    client.emit_mcp(mcp)

print(f"Created structured property: {related_datasets_property.urn}")
