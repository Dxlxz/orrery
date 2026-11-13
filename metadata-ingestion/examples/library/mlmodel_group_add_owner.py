from orrery.sdk import CorpUserUrn, MlModelGroupUrn, OrreryClient

client = OrreryClient.from_env()

group = client.entities.get(
    MlModelGroupUrn(platform="mlflow", name="recommendation-models", env="PROD")
)

group.add_owner(CorpUserUrn("data_science_team"))

client.entities.update(group)
