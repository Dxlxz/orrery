from orrery.metadata.urns import DomainUrn, MlModelGroupUrn
from orrery.sdk import OrreryClient

client = OrreryClient.from_env()

mlmodel_group = client.entities.get(
    MlModelGroupUrn(platform="mlflow", name="recommendation-models", env="PROD")
)

# If you don't know the domain urn, you can look it up:
# domain_urn = client.resolve.domain(name="marketing")

# NOTE: This will overwrite the existing domain
mlmodel_group.set_domain(DomainUrn(id="marketing"))

client.entities.update(mlmodel_group)
