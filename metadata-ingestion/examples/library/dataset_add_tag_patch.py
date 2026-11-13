from orrery.emitter.mce_builder import make_tag_urn
from orrery.metadata.schema_classes import TagAssociationClass
from orrery.sdk import DatasetUrn, OrreryClient
from orrery.specific.dataset import DatasetPatchBuilder

client = OrreryClient.from_env()

# Create the Dataset updater.
patch_builder = DatasetPatchBuilder(
    DatasetUrn(platform="snowflake", name="fct_users_created", env="PROD")
)
patch_builder.add_tag(TagAssociationClass(make_tag_urn("tag-to-add-id")))
patch_builder.remove_tag("urn:li:tag:tag-to-remove-id")

# Do the update.
client.entities.update(patch_builder)
