# metadata-ingestion/examples/library/corpuser_update_profile.py

from orrery.api.entities.corpuser.corpuser import CorpUser, CorpUserGenerationConfig
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig

# Update a user's editable profile information
user = CorpUser(
    id="jdoe",
    email="jdoe@company.com",
    description="Passionate about data quality and building reliable data pipelines. "
    "10+ years of experience in data engineering.",
    slack="@jdoe",
    phone="+1-555-0123",
    picture_link="https://company.com/photos/jdoe.jpg",
)

# Create graph client
orrery_graph = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))

# Emit with override_editable=True to update editable fields
for event in user.generate_mcp(
    generation_config=CorpUserGenerationConfig(override_editable=True)
):
    orrery_graph.emit(event)

print(f"Updated profile for user {user.urn}")
