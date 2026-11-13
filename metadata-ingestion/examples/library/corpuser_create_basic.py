# metadata-ingestion/examples/library/corpuser_create_basic.py
import os

from orrery.api.entities.corpuser.corpuser import CorpUser
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig

# Create a basic user with essential information
user = CorpUser(
    id="jdoe",
    display_name="John Doe",
    email="jdoe@company.com",
    title="Senior Data Engineer",
    first_name="John",
    last_name="Doe",
    full_name="John Doe",
    department_name="Data Engineering",
    country_code="US",
)

# Create graph client
gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")
orrery_graph = OrreryGraph(OrreryGraphConfig(server=gms_server, token=token))

# Emit the user entity
for event in user.generate_mcp():
    orrery_graph.emit(event)

print(f"Created user {user.urn}")
