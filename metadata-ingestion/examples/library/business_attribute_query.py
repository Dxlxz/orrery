from orrery.ingestion.graph.client import get_default_graph
from orrery.metadata.schema_classes import BusinessAttributeInfoClass, OwnershipClass
from orrery.metadata.urns import BusinessAttributeUrn

graph = get_default_graph()

business_attribute_urn = BusinessAttributeUrn("customer_id")

info = graph.get_aspect(
    entity_urn=str(business_attribute_urn), aspect_type=BusinessAttributeInfoClass
)
if info is None:
    raise SystemExit(f"Business attribute not found: {business_attribute_urn}")

print(f"Business Attribute: {business_attribute_urn}")
print(f"Name: {info.name}")
print(f"Description: {info.description}")
print(f"Type: {info.type}")

ownership = graph.get_aspect(
    entity_urn=str(business_attribute_urn), aspect_type=OwnershipClass
)
if ownership is not None:
    print(f"Owners: {[owner.owner for owner in ownership.owners]}")
