# metadata-ingestion/examples/library/application_add_domain.py
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import DomainsClass


def make_application_urn(application_id: str) -> str:
    """Create a Orrery application URN."""
    return f"urn:li:application:{application_id}"


def make_domain_urn(domain_id: str) -> str:
    """Create a Orrery domain URN."""
    return f"urn:li:domain:{domain_id}"


emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

application_urn = make_application_urn("customer-analytics-service")

domain_to_add = make_domain_urn("marketing")

domains = DomainsClass(domains=[domain_to_add])

metadata_event = MetadataChangeProposalWrapper(
    entityUrn=application_urn,
    aspect=domains,
)
emitter.emit(metadata_event)

print(f"Added domain {domain_to_add} to application {application_urn}")
