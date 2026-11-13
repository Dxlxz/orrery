# metadata-ingestion/examples/library/corpuser_add_tag.py

from orrery.emitter.mce_builder import make_tag_urn, make_user_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig
from orrery.metadata.schema_classes import GlobalTagsClass, TagAssociationClass

# User to add tag to
user_urn = make_user_urn("jdoe")

# Tag to add
tag_urn = make_tag_urn("DataEngineering")

# Create graph client
orrery_graph = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))

# Read current tags
current_tags = orrery_graph.get_aspect(entity_urn=user_urn, aspect_type=GlobalTagsClass)

# Initialize tags if they don't exist
if current_tags is None:
    current_tags = GlobalTagsClass(tags=[])

# Check if tag already exists
tag_exists = any(tag.tag == tag_urn for tag in current_tags.tags)

if not tag_exists:
    # Add the new tag
    new_tag = TagAssociationClass(tag=tag_urn)
    current_tags.tags.append(new_tag)

    # Create MCP to update the tags
    mcp = MetadataChangeProposalWrapper(
        entityUrn=user_urn,
        aspect=current_tags,
    )

    # Emit the change
    orrery_graph.emit(mcp)
    print(f"Added tag {tag_urn} to user {user_urn}")
else:
    print(f"Tag {tag_urn} already exists on user {user_urn}")
