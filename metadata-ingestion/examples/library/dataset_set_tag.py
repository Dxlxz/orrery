from orrery.sdk import DatasetUrn, OrreryClient, TagUrn

client = OrreryClient.from_env()

dataset = client.entities.get(DatasetUrn(platform="hive", name="realestate_db.sales"))
dataset.set_tags([TagUrn("purchase")])
client.entities.update(dataset)
