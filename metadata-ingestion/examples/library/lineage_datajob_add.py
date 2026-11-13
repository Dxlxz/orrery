from orrery.metadata.urns import DataFlowUrn, DataJobUrn, DatasetUrn
from orrery.sdk.main_client import OrreryClient

client = OrreryClient.from_env()

dataset_urn = DatasetUrn(platform="snowflake", name="upstream_table")
datajob_urn = DataJobUrn(
    job_id="example_datajob",
    flow=DataFlowUrn(orchestrator="airflow", flow_id="example_dag", cluster="PROD"),
)

client.lineage.add_lineage(upstream=datajob_urn, downstream=dataset_urn)
