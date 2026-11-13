from orrery.sdk import CorpUserUrn, DatasetUrn, OrreryClient

client = OrreryClient.from_env()

dataset = client.entities.get(DatasetUrn(platform="hive", name="realestate_db.sales"))

# Add owner with the TECHNICAL_OWNER type
dataset.add_owner(CorpUserUrn("jdoe"))

client.entities.update(dataset)
