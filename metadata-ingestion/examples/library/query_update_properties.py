# metadata-ingestion/examples/library/query_update_properties.py
import time

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig
from orrery.metadata.schema_classes import (
    AuditStampClass,
    QueryPropertiesClass,
)
from orrery.metadata.urns import CorpUserUrn, QueryUrn

query_urn = QueryUrn("my-unique-query-id")

graph = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))
emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

existing_properties = graph.get_aspect(
    entity_urn=query_urn.urn(),
    aspect_type=QueryPropertiesClass,
)

if not existing_properties:
    raise SystemExit(f"Query {query_urn} does not exist or has no properties")

current_timestamp = int(time.time() * 1000)
actor_urn = CorpUserUrn("orrery")

existing_properties.name = "Updated Query Name"
existing_properties.description = "This query has been updated with new documentation"
existing_properties.lastModified = AuditStampClass(
    time=current_timestamp, actor=actor_urn.urn()
)

event = MetadataChangeProposalWrapper(
    entityUrn=query_urn.urn(),
    aspect=existing_properties,
)

emitter.emit(event)
print(f"Updated properties for query {query_urn}")
