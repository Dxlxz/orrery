from orrery.metadata.urns import DatasetUrn
from orrery.sdk.main_client import OrreryClient

client = OrreryClient.from_env()

downstream_lineage = client.lineage.get_lineage(
    source_urn=DatasetUrn(platform="snowflake", name="sales_summary"),
    direction="downstream",
    max_hops=2,
)

print(downstream_lineage)
