import os

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import RolePropertiesClass

# Create the role URN
# Role URNs follow the pattern: urn:li:role:{role_id}
role_urn = "urn:li:role:snowflake_reader_role"

# Define the role properties
role_properties = RolePropertiesClass(
    name="Snowflake Reader Role",
    description="Provides read-only access to analytics datasets in Snowflake",
    type="READ",
    requestUrl="https://mycompany.okta.com/access/request/snowflake-reader",
)

# Create a metadata change proposal
mcp = MetadataChangeProposalWrapper(
    entityUrn=role_urn,
    aspect=role_properties,
)

# Emit the metadata change
gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")
emitter = OrreryRestEmitter(gms_server=gms_server, token=token)
emitter.emit(mcp)
print(f"Created role: {role_urn}")
