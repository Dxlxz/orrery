from orrery.metadata.urns import MlModelGroupUrn
from orrery.sdk import OrreryClient
from orrery.sdk.mlmodel import MLModel

client = OrreryClient.from_env()

model = MLModel(
    id="my-recommendations-model",
    platform="mlflow",
)

model.set_model_group(
    MlModelGroupUrn(
        platform="mlflow",
        name="my-recommendations-model-group",
    )
)

client.entities.upsert(model)
