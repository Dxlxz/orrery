import os
import time

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    AuditStampClass,
    BusinessAttributeInfoClass,
    NumberTypeClass,
    SchemaFieldDataTypeClass,
)

business_attribute_urn = "urn:li:businessAttribute:customer_id"

business_attribute_info = BusinessAttributeInfoClass(
    fieldPath="customer_id",
    name="Customer ID",
    description="Primary customer identifier field. This attribute should be applied to fields that contain the main customer identifier, typically an integer or long type.",
    type=SchemaFieldDataTypeClass(type=NumberTypeClass()),
    created=AuditStampClass(
        time=int(time.time() * 1000), actor="urn:li:corpuser:orrery"
    ),
    lastModified=AuditStampClass(
        time=int(time.time() * 1000), actor="urn:li:corpuser:orrery"
    ),
)

event: MetadataChangeProposalWrapper = MetadataChangeProposalWrapper(
    entityUrn=business_attribute_urn,
    aspect=business_attribute_info,
)

gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")
rest_emitter = OrreryRestEmitter(gms_server=gms_server, token=token)
rest_emitter.emit(event)
print(f"Created business attribute {business_attribute_urn}")
