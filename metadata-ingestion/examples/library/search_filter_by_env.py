from orrery.sdk import OrreryClient
from orrery.sdk.search_filters import FilterDsl as F

# search for all assets in the production environment
client = OrreryClient(server="<your_server>", token="<your_token>")
results = client.search.get_urns(filter=F.env("PROD"))
