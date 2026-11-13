from orrery.emitter.mce_builder import make_dataset_urn

# read-modify-write requires access to the OrreryGraph (RestEmitter is not enough)
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

# Imports for metadata model classes
from orrery.metadata.schema_classes import (
    DataPlatformInstanceClass,
    DatasetKeyClass,
    StatusClass,
)

dataset_urn = make_dataset_urn(platform="hive", name="realestate_db.sales", env="PROD")

gms_endpoint = "http://localhost:8080"
graph = OrreryGraph(OrreryClientConfig(server=gms_endpoint))

# Query multiple aspects from entity
result = graph.get_aspects_for_entity(
    entity_urn=dataset_urn,
    aspects=["status", "dataPlatformInstance", "datasetKey"],
    aspect_types=[StatusClass, DataPlatformInstanceClass, DatasetKeyClass],
)

# result are typed according to their class if exist
if result is not None:
    if result["datasetKey"]:
        print(result["datasetKey"].name)
