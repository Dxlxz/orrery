from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

# Imports for metadata model classes
from orrery.metadata.schema_classes import DataJobInputOutputClass

# Get the current lineage for a datajob
gms_endpoint = "http://localhost:8080"
graph = OrreryGraph(OrreryClientConfig(server=gms_endpoint))

urn = "urn:li:dataset:(urn:li:dataPlatform:hive,logging_events,PROD)"
result = graph.get_aspect(entity_urn=urn, aspect_type=DataJobInputOutputClass)

print(result)
