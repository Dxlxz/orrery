# metadata-ingestion/examples/library/tag_add_ownership.py
from orrery.metadata.urns import CorpUserUrn
from orrery.sdk import OrreryClient, Tag

client = OrreryClient.from_env()

# Create a tag with ownership
tag = Tag(
    name="data_quality",
    owners=[
        CorpUserUrn("data_steward"),
    ],
)

# Upsert the tag
client.entities.upsert(tag)

print(f"Created tag with ownership: {tag.urn}")
