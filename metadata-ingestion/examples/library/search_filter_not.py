from orrery.sdk import FilterDsl as F, OrreryClient

client = OrreryClient(server="<your_server>", token="<your_token>")

# Search for charts that are not in the PROD environment.
results = client.search.get_urns(
    filter=F.and_(F.entity_type("chart"), F.not_(F.env("PROD"))),
)

print(list(results))
