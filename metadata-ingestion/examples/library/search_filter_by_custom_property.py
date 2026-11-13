from orrery.sdk import OrreryClient
from orrery.sdk.search_filters import FilterDsl as F

client = OrreryClient(server="<your_server>", token="<your_token>")
# search for all assets with a custom property "my_custom_property" set to "my_value"
results = client.search.get_urns(
    filter=F.has_custom_property("my_custom_property", "my_value")
)
