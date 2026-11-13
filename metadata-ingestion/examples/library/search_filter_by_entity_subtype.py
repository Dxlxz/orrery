from orrery.sdk import OrreryClient
from orrery.sdk.search_filters import FilterDsl as F

# search for all mlflow assets of subtype "ML Experiment"
client = OrreryClient(server="<your_server>", token="<your_token>")
results = client.search.get_urns(
    filter=F.and_(F.platform("mlflow"), F.entity_subtype("ML Experiment"))
)
