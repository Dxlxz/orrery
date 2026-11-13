from orrery.metadata.urns import GlossaryTermUrn
from orrery.sdk import DashboardUrn, OrreryClient

client = OrreryClient.from_env()

dashboard = client.entities.get(DashboardUrn("looker", "dashboards.999999"))

dashboard.add_term(GlossaryTermUrn("SalesMetrics"))

client.entities.update(dashboard)

print(f"Added term {GlossaryTermUrn('SalesMetrics')} to dashboard {dashboard.urn}")
