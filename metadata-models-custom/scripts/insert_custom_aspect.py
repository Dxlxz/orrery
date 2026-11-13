import json

from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    ChangeTypeClass,
    GenericAspectClass,
    MetadataChangeProposalClass,
)

dq_aspect = {
    "rules": [
        {
            "field": "my_event_data",
            "isFieldLevel": False,
            "type": "isNull",
            "checkDefinition": "n/a",
            "url": "https://github.com/Dxlxz/orrery/blob/master/checks/nonNull.sql",
        },
        {
            "field": "timestamp",
            "isFieldLevel": True,
            "type": "increasing",
            "checkDefinition": "n/a",
            "url": "https://github.com/Dxlxz/orrery/blob/master/checks/increasing.sql",
        },
    ]
}

emitter: OrreryRestEmitter = OrreryRestEmitter(gms_server="http://localhost:8080")

dataset_urn = "urn:li:dataset:(urn:li:dataPlatform:hive,logging_events,PROD)"
mcp_raw: MetadataChangeProposalClass = MetadataChangeProposalClass(
    entityUrn=dataset_urn,
    changeType=ChangeTypeClass.UPSERT,
    aspectName="customDataQualityRules",
    aspect=GenericAspectClass(
        contentType="application/json",
        value=json.dumps(dq_aspect).encode("utf-8"),
    ),
)

try:
    emitter.emit(mcp_raw)
    print("Successfully wrote to Orrery")
except Exception as e:
    print("Failed to write to Orrery")
    raise e
