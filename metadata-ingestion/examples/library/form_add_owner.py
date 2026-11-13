from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    AuditStampClass,
    OwnerClass,
    OwnershipClass,
    OwnershipTypeClass,
)
from orrery.metadata.urns import CorpUserUrn, FormUrn

# Form URN to add owner to
form_urn = FormUrn("metadata_initiative_2024")

# Create ownership aspect
ownership = OwnershipClass(
    owners=[
        OwnerClass(
            owner=str(CorpUserUrn("governance_team")),
            type=OwnershipTypeClass.TECHNICAL_OWNER,
        )
    ],
    lastModified=AuditStampClass(
        time=0, actor="urn:li:corpuser:orrery", impersonator=None
    ),
)

# Create and emit metadata change proposal
event = MetadataChangeProposalWrapper(
    entityUrn=str(form_urn),
    aspect=ownership,
)

rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")
rest_emitter.emit(event)

print(f"Added owner to form {form_urn}")
