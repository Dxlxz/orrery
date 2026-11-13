# metadata-ingestion/examples/library/datacontract_add_freshness_contract.py

from orrery.emitter.mce_builder import make_assertion_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph
from orrery.metadata.schema_classes import (
    DataContractPropertiesClass,
    FreshnessContractClass,
)

contract_urn = "urn:li:dataContract:purchases-contract"

graph = OrreryGraph(config=OrreryClientConfig(server="http://localhost:8080"))

contract_properties = graph.get_aspect(
    entity_urn=contract_urn,
    aspect_type=DataContractPropertiesClass,
)

if not contract_properties:
    raise SystemExit(f"Contract {contract_urn} not found")

new_freshness_assertion_urn = make_assertion_urn("new-freshness-assertion")

existing_freshness = contract_properties.freshness or []
existing_freshness.append(FreshnessContractClass(assertion=new_freshness_assertion_urn))

contract_properties.freshness = existing_freshness

event = MetadataChangeProposalWrapper(
    entityUrn=contract_urn,
    aspect=contract_properties,
)

rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")
rest_emitter.emit(event)

print(f"Added freshness contract to {contract_urn}")
