from orrery.sdk import DatasetUrn, OrreryClient

client = OrreryClient.from_env()

dataset = client.entities.get(
    DatasetUrn(platform="hive", name="fct_users_created", env="PROD")
)

# Print the dataset domain
print(dataset.domain)
