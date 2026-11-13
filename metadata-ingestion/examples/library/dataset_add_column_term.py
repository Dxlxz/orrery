from orrery.sdk import DatasetUrn, GlossaryTermUrn, OrreryClient

client = OrreryClient.from_env()

dataset = client.entities.get(
    DatasetUrn(platform="hive", name="realestate_db.sales", env="PROD")
)

dataset["address.zipcode"].add_term(GlossaryTermUrn("Classification.Location"))

client.entities.update(dataset)
