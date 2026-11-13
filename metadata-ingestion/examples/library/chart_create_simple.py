from orrery.metadata.urns import TagUrn
from orrery.sdk import Chart, OrreryClient

client = OrreryClient.from_env()

chart = Chart(
    name="example_chart",
    platform="looker",
    description="looker chart for production",
    tags=[TagUrn(name="production"), TagUrn(name="data_engineering")],
)

client.entities.upsert(chart)
