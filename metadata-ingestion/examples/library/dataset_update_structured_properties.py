from orrery.emitter.mce_builder import make_dataset_urn
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.specific.dataset import DatasetPatchBuilder

# Create rest emitter
rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

dataset_urn = make_dataset_urn(platform="hive", name="fct_users_created", env="PROD")

for patch_mcp in (
    DatasetPatchBuilder(dataset_urn)
    .set_structured_property("io.orrery.dataManagement.replicationSLA", 120)
    .build()
):
    rest_emitter.emit(patch_mcp)

print(f"Added cluster_name, retention_time properties to dataset {dataset_urn}")
