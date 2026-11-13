import os

import orrery.emitter.mce_builder as builder
import orrery.metadata.schema_classes as models
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter

# Create an emitter to Orrery over REST
gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")
emitter = OrreryRestEmitter(gms_server=gms_server, token=token)

dataset_urn = builder.make_dataset_urn(
    name="fct_users_created", platform="hive", env="PROD"
)
primary_key_urn = builder.make_ml_primary_key_urn(
    feature_table_name="users_feature_table",
    primary_key_name="user_id",
)

#  Create feature
metadata_change_proposal = MetadataChangeProposalWrapper(
    entityUrn=primary_key_urn,
    aspect=models.MLPrimaryKeyPropertiesClass(
        description="Represents the id of the user the other features relate to.",
        # attaching a source to a ml primary key creates lineage between the feature
        # and the upstream dataset. This is how lineage between your data warehouse
        # and machine learning ecosystem is established.
        sources=[dataset_urn],
        dataType="TEXT",
    ),
)

# Emit metadata!
emitter.emit_mcp(metadata_change_proposal)
print(f"Created ML primary key: {primary_key_urn}")
