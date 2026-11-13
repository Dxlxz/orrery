from orrery.emitter.mce_builder import make_dataset_urn, make_term_urn
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig
from orrery.metadata.schema_classes import GlossaryTermAssociationClass
from orrery.specific.dataset import DatasetPatchBuilder

# Create Orrery Client
orrery_client = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))

# Create Dataset URN
dataset_urn = make_dataset_urn(
    platform="snowflake", name="fct_users_created", env="PROD"
)

# Create Dataset Patch to Add + Remove Term for 'profile_id' column
patch_builder = DatasetPatchBuilder(dataset_urn)
patch_builder.for_field("profile_id").add_term(
    GlossaryTermAssociationClass(make_term_urn("term-to-add-id"))
)
patch_builder.for_field("profile_id").remove_term(
    "urn:li:glossaryTerm:term-to-remove-id"
)
patch_mcps = patch_builder.build()

# Emit Dataset Patch
for patch_mcp in patch_mcps:
    orrery_client.emit(patch_mcp)
