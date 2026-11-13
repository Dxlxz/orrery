from orrery.metadata.urns import TagUrn
from orrery.sdk import DataFlow, OrreryClient

client = OrreryClient.from_env()

dataflow = DataFlow(
    name="example_dataflow",
    platform="airflow",
    description="airflow pipeline for production",
    tags=[TagUrn(name="production"), TagUrn(name="data_engineering")],
)

client.entities.upsert(dataflow)
