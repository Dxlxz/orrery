# metadata-ingestion/examples/library/assertion_field_uniqueness.py
import os

import orrery.emitter.mce_builder as builder
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    AssertionInfoClass,
    AssertionStdOperatorClass,
    AssertionTypeClass,
    FieldAssertionInfoClass,
    FieldAssertionTypeClass,
    FieldMetricAssertionClass,
    FieldMetricTypeClass,
    SchemaFieldSpecClass,
)

emitter = OrreryRestEmitter(
    gms_server=os.getenv("ORRERY_GMS_URL", "http://localhost:8080"),
    token=os.getenv("ORRERY_GMS_TOKEN"),
)

dataset_urn = builder.make_dataset_urn(platform="snowflake", name="mydb.myschema.users")

field_assertion_info = FieldAssertionInfoClass(
    type=FieldAssertionTypeClass.FIELD_METRIC,
    entity=dataset_urn,
    fieldMetricAssertion=FieldMetricAssertionClass(
        field=SchemaFieldSpecClass(
            path="user_id",
            type="VARCHAR",
            nativeType="VARCHAR",
        ),
        metric=FieldMetricTypeClass.UNIQUE_COUNT,
        operator=AssertionStdOperatorClass.EQUAL_TO,
        parameters=None,
    ),
)

assertion_info = AssertionInfoClass(
    type=AssertionTypeClass.FIELD,
    fieldAssertion=field_assertion_info,
    description="User ID must be unique across all rows",
)

assertion_urn = builder.make_assertion_urn(
    builder.orrery_guid(
        {"entity": dataset_urn, "field": "user_id", "type": "uniqueness"}
    )
)

assertion_info_mcp = MetadataChangeProposalWrapper(
    entityUrn=assertion_urn,
    aspect=assertion_info,
)

emitter.emit_mcp(assertion_info_mcp)
print(f"Created field uniqueness assertion: {assertion_urn}")
