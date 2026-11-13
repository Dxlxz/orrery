from orrery.sdk import OrreryClient
from orrery.sdk.search_filters import FilterDsl as F

# search for all assets associated with a specific glossary term
client = OrreryClient(server="<your_server>", token="<your_token>")
results = client.search.get_urns(
    filter=F.glossary_term("urn:li:glossaryTerm:AccountBalance")
)
