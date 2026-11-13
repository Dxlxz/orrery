import os

import orrery.emitter.mce_builder as builder
import orrery.metadata.schema_classes as models
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter

gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")
emitter = OrreryRestEmitter(gms_server=gms_server, token=token)

feature_urn = builder.make_ml_feature_urn(
    feature_table_name="user_features",
    feature_name="total_spend",
)

dataset_urn = builder.make_dataset_urn(
    name="analytics.orders",
    platform="snowflake",
    env="PROD",
)

metadata_change_proposal = MetadataChangeProposalWrapper(
    entityUrn=feature_urn,
    aspect=models.MLFeaturePropertiesClass(
        description="Total amount spent by user across all orders. "
        "Version 2.0 now includes refunds and returns, providing net spend instead of gross. "
        "Changed from gross spend calculation in v1.0.",
        dataType="CONTINUOUS",
        version=models.VersionTagClass(versionTag="2.0"),
        sources=[dataset_urn],
    ),
)

emitter.emit(metadata_change_proposal)
