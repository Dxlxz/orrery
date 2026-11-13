from orrery.sdk import FilterDsl as F, OrreryClient

client = OrreryClient(server="<your_server>", token="<your_token>")

# Search for entities that are on snowflake platform
results = client.search.get_urns(filter=F.platform("snowflake"))

print(list(results))
