from orrery.sdk import DatasetUrn, GlossaryTermUrn, OrreryClient

client = OrreryClient.from_env()

dataset = client.entities.get(DatasetUrn(platform="hive", name="realestate_db.sales"))
dataset.set_terms([GlossaryTermUrn("Classification.HighlyConfidential")])
client.entities.update(dataset)
