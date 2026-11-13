# metadata-ingestion/examples/library/assertion_freshness.py
import os

import orrery.emitter.mce_builder as builder
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    AssertionInfoClass,
    AssertionTypeClass,
    FreshnessAssertionInfoClass,
    FreshnessAssertionScheduleClass,
    FreshnessAssertionScheduleTypeClass,
    FreshnessAssertionTypeClass,
    FreshnessCronScheduleClass,
)

emitter = OrreryRestEmitter(
    gms_server=os.getenv("ORRERY_GMS_URL", "http://localhost:8080"),
    token=os.getenv("ORRERY_GMS_TOKEN"),
)

dataset_urn = builder.make_dataset_urn(
    platform="redshift", name="prod.analytics.daily_metrics"
)

freshness_assertion_info = FreshnessAssertionInfoClass(
    type=FreshnessAssertionTypeClass.DATASET_CHANGE,
    entity=dataset_urn,
    schedule=FreshnessAssertionScheduleClass(
        type=FreshnessAssertionScheduleTypeClass.CRON,
        cron=FreshnessCronScheduleClass(
            cron="0 9 * * *",
            timezone="America/Los_Angeles",
            windowStartOffsetMs=None,
        ),
    ),
)

assertion_info = AssertionInfoClass(
    type=AssertionTypeClass.FRESHNESS,
    freshnessAssertion=freshness_assertion_info,
    description="Daily metrics table must be updated every day by 9 AM Pacific Time",
)

assertion_urn = builder.make_assertion_urn(
    builder.orrery_guid({"entity": dataset_urn, "type": "freshness-daily-9am"})
)

assertion_info_mcp = MetadataChangeProposalWrapper(
    entityUrn=assertion_urn,
    aspect=assertion_info,
)

emitter.emit_mcp(assertion_info_mcp)
print(f"Created freshness assertion: {assertion_urn}")
