# metadata-ingestion/examples/library/corpuser_create_with_groups.py

from orrery.api.entities.corpuser.corpuser import CorpUser
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig

# Create a user with group memberships
user = CorpUser(
    id="jsmith",
    display_name="Jane Smith",
    email="jsmith@company.com",
    title="Data Analyst",
    first_name="Jane",
    last_name="Smith",
    full_name="Jane Smith",
    department_name="Analytics",
    country_code="US",
    groups=["data-engineering", "analytics-team"],
)

# Create graph client
orrery_graph = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))

# Emit the user entity with group memberships
for event in user.generate_mcp():
    orrery_graph.emit(event)

print(f"Created user {user.urn} with group memberships")
