from orrery.emitter.mce_builder import make_dataset_urn
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig
from orrery.specific.dataset import DatasetPatchBuilder

# Create Orrery Client
orrery_client = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))

# Create Dataset URN
dataset_urn = make_dataset_urn(platform="hive", name="fct_users_created", env="PROD")

# Create Dataset Patch to Add Custom Properties
patch_builder = DatasetPatchBuilder(dataset_urn)
patch_builder.add_custom_property("cluster_name", "orreryproject.orrery.dev")
patch_builder.add_custom_property("retention_time", "2 years")
patch_mcps = patch_builder.build()

# Emit Dataset Patch
for patch_mcp in patch_mcps:
    orrery_client.emit(patch_mcp)
