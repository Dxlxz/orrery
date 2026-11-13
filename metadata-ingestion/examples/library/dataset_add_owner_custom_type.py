from orrery.emitter.mce_builder import (
    make_dataset_urn,
    make_ownership_type_urn,
    make_user_urn,
)
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig
from orrery.metadata.schema_classes import (
    OwnerClass,
    OwnershipClass,
    OwnershipTypeClass,
)

# Create Orrery client
graph = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))
emitter = OrreryRestEmitter("http://localhost:8080")

# Create dataset URN
dataset_urn = make_dataset_urn(platform="snowflake", name="analytics.users", env="PROD")

# Create custom ownership type URN
# This should reference a previously created custom ownership type
custom_ownership_type_urn = make_ownership_type_urn("data_quality_lead")

# Create an owner with the custom ownership type
owner = OwnerClass(
    owner=make_user_urn("jdoe"),
    type=OwnershipTypeClass.CUSTOM,  # Use CUSTOM enum for custom types
    typeUrn=custom_ownership_type_urn,  # Reference the custom ownership type entity
)

# Get existing ownership or create new
try:
    existing_ownership = graph.get_aspect(dataset_urn, OwnershipClass)
    if existing_ownership:
        # Add to existing owners
        existing_ownership.owners.append(owner)
        ownership = existing_ownership
    else:
        # Create new ownership aspect
        ownership = OwnershipClass(owners=[owner])
except Exception:
    # Create new ownership aspect if retrieval fails
    ownership = OwnershipClass(owners=[owner])

# Emit the ownership aspect
mcp = MetadataChangeProposalWrapper(
    entityUrn=str(dataset_urn),
    aspect=ownership,
)

emitter.emit_mcp(mcp)

print(
    f"Added owner {owner.owner} with custom ownership type {custom_ownership_type_urn}"
)
print(f"to dataset {dataset_urn}")
