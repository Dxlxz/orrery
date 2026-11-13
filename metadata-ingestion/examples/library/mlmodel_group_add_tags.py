from orrery.metadata.urns import MlModelGroupUrn, TagUrn
from orrery.sdk import OrreryClient

client = OrreryClient.from_env()

group = client.entities.get(
    MlModelGroupUrn(platform="mlflow", name="recommendation-models", env="PROD")
)
group.add_tag(TagUrn("production-ready"))

client.entities.update(group)
