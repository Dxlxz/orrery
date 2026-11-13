from orrery.sdk import CorpUserUrn, DashboardUrn, OrreryClient

client = OrreryClient.from_env()

dashboard = client.entities.get(DashboardUrn("looker", "dashboards.999999"))

dashboard.add_owner(CorpUserUrn("jdoe"))

client.entities.update(dashboard)

print(f"Added owner {CorpUserUrn('jdoe')} to dashboard {dashboard.urn}")
