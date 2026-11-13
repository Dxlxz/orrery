from orrery.metadata.urns import TagUrn
from orrery.sdk import Dashboard, OrreryClient

client = OrreryClient.from_env()

dashboard = Dashboard(
    name="example_dashboard",
    platform="looker",
    description="looker dashboard for production",
    tags=[TagUrn(name="production"), TagUrn(name="data_engineering")],
)

client.entities.upsert(dashboard)
