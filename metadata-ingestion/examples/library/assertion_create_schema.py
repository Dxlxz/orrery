# metadata-ingestion/examples/library/assertion_schema.py
import os
import time

import orrery.emitter.mce_builder as builder
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    AssertionInfoClass,
    AssertionTypeClass,
    AuditStampClass,
    NumberTypeClass,
    SchemaAssertionCompatibilityClass,
    SchemaAssertionInfoClass,
    SchemaFieldClass,
    SchemaFieldDataTypeClass,
    SchemalessClass,
    SchemaMetadataClass,
    StringTypeClass,
)

emitter = OrreryRestEmitter(
    gms_server=os.getenv("ORRERY_GMS_URL", "http://localhost:8080"),
    token=os.getenv("ORRERY_GMS_TOKEN"),
)

dataset_urn = builder.make_dataset_urn(platform="kafka", name="prod.user_events")

current_timestamp = int(time.time() * 1000)
audit_stamp = AuditStampClass(
    time=current_timestamp,
    actor="urn:li:corpuser:orrery",
)

expected_schema = SchemaMetadataClass(
    schemaName="user_events",
    platform=builder.make_data_platform_urn("kafka"),
    version=0,
    created=audit_stamp,
    lastModified=audit_stamp,
    fields=[
        SchemaFieldClass(
            fieldPath="user_id",
            type=SchemaFieldDataTypeClass(type=StringTypeClass()),
            nativeDataType="string",
            lastModified=audit_stamp,
        ),
        SchemaFieldClass(
            fieldPath="event_type",
            type=SchemaFieldDataTypeClass(type=StringTypeClass()),
            nativeDataType="string",
            lastModified=audit_stamp,
        ),
        SchemaFieldClass(
            fieldPath="timestamp",
            type=SchemaFieldDataTypeClass(type=NumberTypeClass()),
            nativeDataType="long",
            lastModified=audit_stamp,
        ),
        SchemaFieldClass(
            fieldPath="properties",
            type=SchemaFieldDataTypeClass(type=StringTypeClass()),
            nativeDataType="string",
            lastModified=audit_stamp,
        ),
    ],
    hash="",
    platformSchema=SchemalessClass(),
)

schema_assertion_info = SchemaAssertionInfoClass(
    entity=dataset_urn,
    schema=expected_schema,
    compatibility=SchemaAssertionCompatibilityClass.SUPERSET,
)

assertion_info = AssertionInfoClass(
    type=AssertionTypeClass.DATA_SCHEMA,
    schemaAssertion=schema_assertion_info,
    description="User events stream must have required schema fields (can include additional fields)",
)

assertion_urn = builder.make_assertion_urn(
    builder.orrery_guid({"entity": dataset_urn, "type": "schema-check"})
)

assertion_info_mcp = MetadataChangeProposalWrapper(
    entityUrn=assertion_urn,
    aspect=assertion_info,
)

emitter.emit_mcp(assertion_info_mcp)
print(f"Created schema assertion: {assertion_urn}")
