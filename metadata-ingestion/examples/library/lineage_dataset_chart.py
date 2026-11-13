from orrery.metadata.urns import ChartUrn, DatasetUrn
from orrery.sdk import OrreryClient

client = OrreryClient.from_env()

# add dataset -> chart lineage
client.lineage.add_lineage(
    upstream=DatasetUrn(platform="hdfs", name="dataset1", env="PROD"),
    downstream=ChartUrn(dashboard_tool="looker", chart_id="chart_1"),
)
