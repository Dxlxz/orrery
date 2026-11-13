import orrery.emitter.mce_builder as builder
import orrery.metadata.schema_classes as models
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph

gms_endpoint = "http://localhost:8080"
emitter = OrreryRestEmitter(gms_server=gms_endpoint, extra_headers={})
graph = OrreryGraph(OrreryClientConfig(server=gms_endpoint))

feature_urn = builder.make_ml_feature_urn(
    feature_table_name="user_features",
    feature_name="age",
)

owner_to_add = builder.make_user_urn("data_science_team")

current_ownership = graph.get_aspect(
    entity_urn=feature_urn, aspect_type=models.OwnershipClass
)

if current_ownership:
    if owner_to_add not in [owner.owner for owner in current_ownership.owners]:
        current_ownership.owners.append(
            models.OwnerClass(
                owner=owner_to_add,
                type=models.OwnershipTypeClass.DATAOWNER,
            )
        )
else:
    current_ownership = models.OwnershipClass(
        owners=[
            models.OwnerClass(
                owner=owner_to_add,
                type=models.OwnershipTypeClass.DATAOWNER,
            )
        ]
    )

emitter.emit(
    MetadataChangeProposalWrapper(
        entityUrn=feature_urn,
        aspect=current_ownership,
    )
)
