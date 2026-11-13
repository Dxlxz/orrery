from orrery.emitter.mce_builder import make_group_urn, make_user_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    ActorsClass,
    RoleGroupClass,
    RoleUserClass,
)

# Create the role URN
# Role URNs follow the pattern: urn:li:role:{role_id}
role_urn = "urn:li:role:snowflake_reader_role"

# Define the users and groups assigned to this role
actors = ActorsClass(
    users=[
        RoleUserClass(user=make_user_urn("john.doe")),
        RoleUserClass(user=make_user_urn("jane.smith")),
    ],
    groups=[
        RoleGroupClass(group=make_group_urn("data-analysts")),
        RoleGroupClass(group=make_group_urn("business-intelligence")),
    ],
)

# Create a metadata change proposal
mcp = MetadataChangeProposalWrapper(
    entityUrn=role_urn,
    aspect=actors,
)

# Emit the metadata change
emitter = OrreryRestEmitter(gms_server="http://localhost:8080")
emitter.emit(mcp)
print(f"Assigned users and groups to role: {role_urn}")
