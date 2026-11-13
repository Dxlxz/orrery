from orrery.api.entities.corpuser.corpuser import CorpUser, CorpUserGenerationConfig
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig

user_email = "bar@orrery.dev"

user: CorpUser = CorpUser(
    id=user_email,
    display_name="The Bar",
    email=user_email,
    title="Software Engineer",
    first_name="The",
    last_name="Bar",
    full_name="The Bar",
)

# Create graph client
orrery_graph = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))
for event in user.generate_mcp(
    generation_config=CorpUserGenerationConfig(override_editable=False)
):
    orrery_graph.emit(event)
print(f"Upserted user {user.urn}")
