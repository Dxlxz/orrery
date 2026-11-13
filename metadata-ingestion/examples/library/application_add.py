#!/usr/bin/env python3
from orrery.emitter.mce_builder import make_dataset_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import ApplicationsClass


# Utility function for creating application URNs (not yet in SDK)
def make_application_urn(application_id: str) -> str:
    """Create a Orrery application URN."""
    return f"urn:li:application:{application_id}"


def add_application_aspect():
    emitter = OrreryRestEmitter(gms_server="http://localhost:8080", token="")
    dataset_urn = make_dataset_urn("snowflake", "database.schema.table", "PROD")

    application_urn = make_application_urn("my_application")
    applications_aspect = ApplicationsClass(applications=[application_urn])

    emitter.emit(
        MetadataChangeProposalWrapper(entityUrn=dataset_urn, aspect=applications_aspect)
    )

    print(f"Successfully added application: {application_urn}")


if __name__ == "__main__":
    add_application_aspect()
