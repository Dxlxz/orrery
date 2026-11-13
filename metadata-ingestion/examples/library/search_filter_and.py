from orrery.sdk import FilterDsl as F, OrreryClient

client = OrreryClient(server="<your_server>", token="<your_token>")

# Search snowflake datasets that are in PROD environment
results = client.search.get_urns(filter=F.and_(F.platform("snowflake"), F.env("PROD")))

print(list(results))
