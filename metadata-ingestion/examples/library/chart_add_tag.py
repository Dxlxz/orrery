from orrery.sdk import ChartUrn, OrreryClient, TagUrn

client = OrreryClient.from_env()

chart = client.entities.get(ChartUrn("looker", "sales_dashboard"))

chart.add_tag(TagUrn("Important"))

client.entities.update(chart)

print(f"Added tag {TagUrn('Important')} to chart {chart.urn}")
