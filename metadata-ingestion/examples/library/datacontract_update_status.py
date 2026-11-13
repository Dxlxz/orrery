# metadata-ingestion/examples/library/datacontract_update_status.py

from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import (
    DataContractStateClass,
    DataContractStatusClass,
)

contract_urn = "urn:li:dataContract:purchases-contract"

contract_status_aspect = DataContractStatusClass(state=DataContractStateClass.ACTIVE)

event = MetadataChangeProposalWrapper(
    entityUrn=contract_urn,
    aspect=contract_status_aspect,
)

rest_emitter = OrreryRestEmitter(gms_server="http://localhost:8080")
rest_emitter.emit(event)

print(f"Updated status of data contract {contract_urn} to ACTIVE")
