from orrery.sdk import OrreryClient
from orrery.sdk.mlmodelgroup import MLModelGroup

client = OrreryClient.from_env()

mlmodel_group = client.entities.get(
    MLModelGroup.get_urn_type()(
        platform="mlflow", name="recommendation-models", env="PROD"
    )
)

doc_url = "https://wiki.example.com/ml/recommendation-models"
doc_description = "Model architecture and training documentation"

mlmodel_group.add_link((doc_url, doc_description))

client.entities.update(mlmodel_group)
