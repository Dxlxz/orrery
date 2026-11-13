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
    feature_name="age",
)

metadata_change_proposal = MetadataChangeProposalWrapper(
    entityUrn=feature_urn,
    aspect=models.MLFeaturePropertiesClass(
        description="Age of the user in years, calculated as the difference between current date and birth date. "
        "This feature is commonly used for demographic segmentation and age-based personalization. "
        "Values range from 18-100 for registered users (age verification required).",
        dataType="CONTINUOUS",
    ),
)

emitter.emit(metadata_change_proposal)
