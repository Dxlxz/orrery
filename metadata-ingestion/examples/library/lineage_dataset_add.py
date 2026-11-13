from orrery.metadata.urns import DatasetUrn
from orrery.sdk.main_client import OrreryClient

client = OrreryClient.from_env()


upstream_urn = DatasetUrn(platform="snowflake", name="sales_raw")
downstream_urn = DatasetUrn(platform="snowflake", name="sales_cleaned")
client.lineage.add_lineage(upstream=upstream_urn, downstream=downstream_urn)
