from orrery.metadata.schema_classes import DomainsClass
from orrery.sdk import DatasetUrn, OrreryClient

client = OrreryClient.from_env()

dataset = client.entities.get(DatasetUrn(platform="snowflake", name="example_dataset"))

# Remove the dataset from its current domain by setting an empty domains list
dataset._set_aspect(DomainsClass(domains=[]))

client.entities.update(dataset)
