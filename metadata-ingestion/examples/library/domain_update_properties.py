from orrery.emitter.mce_builder import make_domain_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import DomainPropertiesClass

# Update an existing domain's properties
domain_urn = make_domain_urn("marketing")

# Create updated properties
domain_properties_aspect = DomainPropertiesClass(
    name="Marketing and Sales",  # Updated name
    description="Updated description: This domain includes all marketing and sales data assets, "
    "including campaigns, leads, opportunities, and customer analytics.",
    parentDomain="urn:li:domain:revenue",  # Move to different parent
)

event: MetadataChangeProposalWrapper = MetadataChangeProposalWrapper(
    entityUrn=domain_urn,
    aspect=domain_properties_aspect,
)

rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")
rest_emitter.emit(event)
print(f"Updated domain properties for {domain_urn}")
