# metadata-ingestion/examples/library/query_create.py
import os
import time

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    AuditStampClass,
    QueryLanguageClass,
    QueryPropertiesClass,
    QuerySourceClass,
    QueryStatementClass,
    QuerySubjectClass,
    QuerySubjectsClass,
)
from orrery.metadata.urns import CorpUserUrn, DatasetUrn, QueryUrn

query_id = "my-unique-query-id"
query_urn = QueryUrn(query_id)

current_timestamp = int(time.time() * 1000)
actor_urn = CorpUserUrn("orrery")

query_properties = QueryPropertiesClass(
    statement=QueryStatementClass(
        value="SELECT customer_id, order_total FROM orders WHERE order_date >= '2024-01-01'",
        language=QueryLanguageClass.SQL,
    ),
    source=QuerySourceClass.MANUAL,
    name="Customer Orders Q1 2024",
    description="Query to retrieve all customer orders from Q1 2024 for reporting",
    created=AuditStampClass(time=current_timestamp, actor=actor_urn.urn()),
    lastModified=AuditStampClass(time=current_timestamp, actor=actor_urn.urn()),
)

dataset_urn = DatasetUrn.from_string(
    "urn:li:dataset:(urn:li:dataPlatform:postgres,public.orders,PROD)"
)
query_subjects = QuerySubjectsClass(
    subjects=[
        QuerySubjectClass(entity=dataset_urn.urn()),
    ]
)

gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")
rest_emitter = OrreryRestEmitter(gms_server=gms_server, token=token)

mcpw_properties = MetadataChangeProposalWrapper(
    entityUrn=query_urn.urn(),
    aspect=query_properties,
)
rest_emitter.emit(mcpw_properties)

mcpw_subjects = MetadataChangeProposalWrapper(
    entityUrn=query_urn.urn(),
    aspect=query_subjects,
)
rest_emitter.emit(mcpw_subjects)

print(f"Created query {query_urn}")
