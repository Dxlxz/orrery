from orrery.sdk import DatasetUrn, OrreryClient

client = OrreryClient.from_env()

dataset = client.entities.get(
    DatasetUrn(platform="hive", name="realestate_db.sales", env="PROD")
)

print(dataset.tags)
