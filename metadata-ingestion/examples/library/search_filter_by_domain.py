from orrery.sdk import OrreryClient
from orrery.sdk.search_filters import FilterDsl as F

# search for all assets in the marketing domain
client = OrreryClient.from_env()
results = client.search.get_urns(filter=F.domain("urn:li:domain:marketing"))
