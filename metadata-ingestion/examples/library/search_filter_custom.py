from orrery.sdk import FilterDsl as F, OrreryClient

client = OrreryClient(server="<your_server>", token="<your_token>")

# Search for datasets that have "example_dataset" in the urn
results = client.search.get_urns(
    filter=F.custom_filter(field="urn", condition="CONTAIN", values=["example_dataset"])
)

print(list(results))
