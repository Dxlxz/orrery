from orrery.metadata.urns import DatasetUrn
from orrery.sdk import OrreryClient

client = OrreryClient.from_env()

client.lineage.add_lineage(
    upstream=DatasetUrn(platform="snowflake", name="sales_raw"),
    downstream=DatasetUrn(platform="snowflake", name="sales_cleaned"),
    column_lineage=True,  # same as "auto_fuzzy", which maps columns based on name similarity
)
