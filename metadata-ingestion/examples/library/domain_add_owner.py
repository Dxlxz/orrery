from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph
from orrery.metadata.schema_classes import (
    OwnerClass,
    OwnershipClass,
    OwnershipTypeClass,
)
from orrery.metadata.urns import CorpUserUrn, DomainUrn

graph = OrreryGraph(OrreryClientConfig(server="http://localhost:8080"))
emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

domain_urn = DomainUrn(id="marketing")

# Get existing ownership
existing_ownership = graph.get_aspect(str(domain_urn), OwnershipClass)
owner_list = (
    list(existing_ownership.owners)
    if existing_ownership and existing_ownership.owners
    else []
)

# Add new owner with the TECHNICAL_OWNER type
owner_list.append(
    OwnerClass(owner=str(CorpUserUrn("jdoe")), type=OwnershipTypeClass.TECHNICAL_OWNER)
)

# Emit ownership
emitter.emit_mcp(
    MetadataChangeProposalWrapper(
        entityUrn=str(domain_urn), aspect=OwnershipClass(owners=owner_list)
    )
)
