from orrery.sdk.main_client import OrreryClient
from orrery.sdk.search_filters import FilterDsl as F

client = OrreryClient.from_env()

# get upstream snowflake production datasets.
results = client.lineage.get_lineage(
    source_urn="urn:li:dataset:(platform,sales_agg,PROD)",
    direction="upstream",
    filter=F.and_(F.platform("snowflake"), F.entity_type("dataset"), F.env("PROD")),
)

print(results)
