# metadata-ingestion/examples/library/datacontract_add_schema_contract.py

from orrery.emitter.mce_builder import make_assertion_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph
from orrery.metadata.schema_classes import (
    DataContractPropertiesClass,
    SchemaContractClass,
)

contract_urn = "urn:li:dataContract:purchases-contract"

graph = OrreryGraph(config=OrreryClientConfig(server="http://localhost:8080"))

contract_properties = graph.get_aspect(
    entity_urn=contract_urn,
    aspect_type=DataContractPropertiesClass,
)

if not contract_properties:
    raise SystemExit(f"Contract {contract_urn} not found")

new_schema_assertion_urn = make_assertion_urn("new-schema-assertion")

existing_schema = contract_properties.schema or []
existing_schema.append(SchemaContractClass(assertion=new_schema_assertion_urn))

contract_properties.schema = existing_schema

event = MetadataChangeProposalWrapper(
    entityUrn=contract_urn,
    aspect=contract_properties,
)

rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")
rest_emitter.emit(event)

print(f"Added schema contract to {contract_urn}")
