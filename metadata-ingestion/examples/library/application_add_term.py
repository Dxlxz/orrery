# metadata-ingestion/examples/library/application_add_term.py
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    AuditStampClass,
    GlossaryTermAssociationClass,
    GlossaryTermsClass,
)


def make_application_urn(application_id: str) -> str:
    """Create a Orrery application URN."""
    return f"urn:li:application:{application_id}"


def make_term_urn(term_name: str) -> str:
    """Create a Orrery glossary term URN."""
    return f"urn:li:glossaryTerm:{term_name}"


emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

application_urn = make_application_urn("customer-analytics-service")

term_to_add = make_term_urn("CustomerData")

terms = GlossaryTermsClass(
    terms=[
        GlossaryTermAssociationClass(urn=term_to_add),
    ],
    auditStamp=AuditStampClass(time=0, actor="urn:li:corpuser:orrery"),
)

metadata_event = MetadataChangeProposalWrapper(
    entityUrn=application_urn,
    aspect=terms,
)
emitter.emit(metadata_event)

print(f"Added term {term_to_add} to application {application_urn}")
