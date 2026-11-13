# metadata-ingestion/examples/library/datacontract_add_quality_contract.py

from orrery.emitter.mce_builder import make_assertion_urn
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.ingestion.graph.client import OrreryClientConfig, OrreryGraph
from orrery.metadata.schema_classes import (
    DataContractPropertiesClass,
    DataQualityContractClass,
)

contract_urn = "urn:li:dataContract:purchases-contract"

graph = OrreryGraph(config=OrreryClientConfig(server="http://localhost:8080"))

contract_properties = graph.get_aspect(
    entity_urn=contract_urn,
    aspect_type=DataContractPropertiesClass,
)

if not contract_properties:
    raise SystemExit(f"Contract {contract_urn} not found")

new_quality_assertion_urn_1 = make_assertion_urn("completeness-check-user-id")
new_quality_assertion_urn_2 = make_assertion_urn("validity-check-email-format")

existing_quality = contract_properties.dataQuality or []
existing_quality.extend(
    [
        DataQualityContractClass(assertion=new_quality_assertion_urn_1),
        DataQualityContractClass(assertion=new_quality_assertion_urn_2),
    ]
)

contract_properties.dataQuality = existing_quality

event = MetadataChangeProposalWrapper(
    entityUrn=contract_urn,
    aspect=contract_properties,
)

rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")
rest_emitter.emit(event)

print(f"Added data quality contracts to {contract_urn}")
