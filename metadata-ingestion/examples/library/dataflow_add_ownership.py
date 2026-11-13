# metadata-ingestion/examples/library/dataflow_add_ownership.py
from orrery.metadata.urns import CorpGroupUrn, CorpUserUrn, DataFlowUrn
from orrery.sdk import OrreryClient

client = OrreryClient.from_env()

# Get the existing DataFlow
dataflow_urn = DataFlowUrn("airflow", "daily_sales_pipeline", "prod")
dataflow = client.entities.get(dataflow_urn)

# Add individual owners
dataflow.add_owner((CorpUserUrn("alice"), "DATAOWNER"))
dataflow.add_owner((CorpUserUrn("bob"), "DEVELOPER"))

# Add group owner
dataflow.add_owner((CorpGroupUrn("analytics-team"), "DATAOWNER"))

# Save changes
client.entities.upsert(dataflow)

print(f"Updated DataFlow: {dataflow.urn}")
print(f"Owners: {dataflow.owners}")
