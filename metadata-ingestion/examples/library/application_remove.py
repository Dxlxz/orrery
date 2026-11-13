#!/usr/bin/env python3

from orrery.emitter.mce_builder import make_dataset_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import ApplicationsClass


def remove_application_aspect():
    emitter = OrreryRestEmitter(gms_server="http://localhost:8080", token="")
    dataset_urn = make_dataset_urn("snowflake", "database.schema.table", "PROD")

    applications_aspect = ApplicationsClass(applications=[])

    emitter.emit(
        MetadataChangeProposalWrapper(entityUrn=dataset_urn, aspect=applications_aspect)
    )

    print("Successfully removed application")


if __name__ == "__main__":
    remove_application_aspect()
