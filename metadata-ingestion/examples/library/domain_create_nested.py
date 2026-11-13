import os

from orrery.emitter.mce_builder import make_domain_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import DomainPropertiesClass

domain_urn = make_domain_urn("verticals")
domain_properties_aspect = DomainPropertiesClass(
    name="Verticals",
    description="Entities related to the verticals sub-domain",
    parentDomain="urn:li:domain:marketing",
)

event: MetadataChangeProposalWrapper = MetadataChangeProposalWrapper(
    entityUrn=domain_urn,
    aspect=domain_properties_aspect,
)

# Get Orrery connection details from environment
gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")

rest_emitter = OrreryRestEmitter(gms_server=gms_server, token=token)
rest_emitter.emit(event)
print(f"Created domain {domain_urn}")
