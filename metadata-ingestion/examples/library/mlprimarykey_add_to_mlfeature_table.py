import orrery.emitter.mce_builder as builder
import orrery.metadata.schema_classes as models
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph
from orrery.metadata.schema_classes import MLFeatureTablePropertiesClass

gms_endpoint = "http://localhost:8080"
# Create an emitter to Orrery over REST
emitter = OrreryRestEmitter(gms_server=gms_endpoint, extra_headers={})

feature_table_urn = builder.make_ml_feature_table_urn(
    feature_table_name="users_feature_table", platform="feast"
)
primary_key_urns = [
    builder.make_ml_primary_key_urn(
        feature_table_name="users_feature_table",
        primary_key_name="user_id",
    ),
]

# This code concatenates the new primary keys with the existing primary keys in the feature table.
# If you want to replace all existing primary keys with only the new ones, you can comment out this line.
graph = OrreryGraph(OrreryClientConfig(server=gms_endpoint))
feature_table_properties = graph.get_aspect(
    entity_urn=feature_table_urn, aspect_type=MLFeatureTablePropertiesClass
)
if feature_table_properties:
    current_primary_keys = feature_table_properties.mlPrimaryKeys
    print("current_primary_keys:", current_primary_keys)
    if current_primary_keys:
        primary_key_urns += current_primary_keys

feature_table_properties = models.MLFeatureTablePropertiesClass(
    mlPrimaryKeys=primary_key_urns
)
# MCP creation
metadata_change_proposal = MetadataChangeProposalWrapper(
    entityUrn=feature_table_urn,
    aspect=feature_table_properties,
)

# Emit metadata! This is a blocking call
emitter.emit(metadata_change_proposal)
