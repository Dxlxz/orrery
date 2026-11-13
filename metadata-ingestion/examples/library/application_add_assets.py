# metadata-ingestion/examples/library/application_add_assets.py
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import ApplicationsClass


def make_application_urn(application_id: str) -> str:
    """Create a Orrery application URN."""
    return f"urn:li:application:{application_id}"


def make_dataset_urn(platform: str, name: str, env: str) -> str:
    """Create a Orrery dataset URN."""
    return f"urn:li:dataset:(urn:li:dataPlatform:{platform},{name},{env})"


emitter = OrreryRestEmitter(gms_server="http://localhost:8080")

application_urn = make_application_urn("customer-analytics-service")

dataset_urns = [
    make_dataset_urn("snowflake", "prod.marketing.customer_events", "PROD"),
    make_dataset_urn("snowflake", "prod.marketing.customer_profiles", "PROD"),
    make_dataset_urn("kafka", "customer-events-stream", "PROD"),
]

for dataset_urn in dataset_urns:
    applications_aspect = ApplicationsClass(applications=[application_urn])

    metadata_event = MetadataChangeProposalWrapper(
        entityUrn=dataset_urn,
        aspect=applications_aspect,
    )
    emitter.emit(metadata_event)

    print(f"Associated {dataset_urn} with application {application_urn}")

print(f"\nSuccessfully associated {len(dataset_urns)} assets with application")
