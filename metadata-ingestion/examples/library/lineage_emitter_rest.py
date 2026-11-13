import orrery.emitter.mce_builder as builder
from orrery.emitter.rest_emitter import OrreryRestEmitter

# Construct a lineage object.
lineage_mce = builder.make_lineage_mce(
    [
        builder.make_dataset_urn("hive", "fct_users_deleted"),  # Upstream
    ],
    builder.make_dataset_urn("hive", "logging_events"),  # Downstream
)

# Create an emitter to the GMS REST API.
emitter = OrreryRestEmitter("http://localhost:8080")

# Emit metadata!
emitter.emit_mce(lineage_mce)
