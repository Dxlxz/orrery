from orrery.sdk import OrreryClient
from orrery.sdk.search_filters import FilterDsl as F

# search for all dashboards
client = OrreryClient(server="<your_server>", token="<your_token>")
results = client.search.get_urns(filter=F.entity_type("dashboard"))
