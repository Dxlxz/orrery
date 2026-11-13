# metadata-ingestion/examples/library/notebook_add_tags.py

from orrery.emitter.mce_builder import make_tag_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    GlobalTagsClass,
    TagAssociationClass,
)

emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

notebook_urn = "urn:li:notebook:(querybook,customer_analysis_2024)"

tag_to_add = make_tag_urn("production")
tag_association = TagAssociationClass(tag=tag_to_add)

global_tags = GlobalTagsClass(tags=[tag_association])

event = MetadataChangeProposalWrapper(
    entityUrn=notebook_urn,
    aspect=global_tags,
)

emitter.emit(event)
print(f"Added tag {tag_to_add} to notebook {notebook_urn}")
