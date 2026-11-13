from orrery.emitter.mce_builder import make_dataset_urn
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

graph = OrreryGraph(
    config=OrreryClientConfig(
        server="http://localhost:8080",
    )
)

dataset_urn = make_dataset_urn(name="fct_users_created", platform="hive")

# Soft-delete the dataset.
graph.delete_entity(urn=dataset_urn, hard=False)

print(f"Deleted dataset {dataset_urn}")
