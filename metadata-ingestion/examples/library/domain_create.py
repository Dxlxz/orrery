import os

from orrery.emitter.mce_builder import make_domain_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import DomainPropertiesClass

# Get Orrery connection details from environment
gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")

domain_urn = make_domain_urn("marketing")
domain_properties_aspect = DomainPropertiesClass(
    name="Marketing", description="Entities related to the marketing department"
)

event: MetadataChangeProposalWrapper = MetadataChangeProposalWrapper(
    entityUrn=domain_urn,
    aspect=domain_properties_aspect,
)

rest_emitter = OrreryRestEmitter(gms_server=gms_server, token=token)
rest_emitter.emit(event)
print(f"Created domain {domain_urn}")
