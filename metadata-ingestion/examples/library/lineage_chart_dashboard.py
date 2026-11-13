from orrery.metadata.urns import ChartUrn, DashboardUrn
from orrery.sdk import OrreryClient

client = OrreryClient.from_env()

# add chart to parent dashboard
client.lineage.add_lineage(
    upstream=ChartUrn(chart_id="chart_1", dashboard_tool="looker"),
    downstream=DashboardUrn(dashboard_id="dashboard_1", dashboard_tool="looker"),
)
