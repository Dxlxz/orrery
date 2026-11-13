import orrery.metadata.schema_classes as models
from orrery.metadata.urns import MlFeatureUrn, MlModelGroupUrn
from orrery.sdk import OrreryClient
from orrery.sdk.mlmodel import MLModel

client = OrreryClient.from_env()

mlmodel = MLModel(
    id="my-recommendations-model",
    name="My Recommendations Model",
    platform="mlflow",
    model_group=MlModelGroupUrn(
        platform="mlflow", name="my-recommendations-model-group"
    ),
    custom_properties={
        "framework": "pytorch",
    },
    extra_aspects=[
        models.MLModelPropertiesClass(
            mlFeatures=[
                str(
                    MlFeatureUrn(
                        feature_namespace="users_feature_table", name="user_signup_date"
                    )
                ),
                str(
                    MlFeatureUrn(
                        feature_namespace="users_feature_table",
                        name="user_last_active_date",
                    )
                ),
            ]
        )
    ],
    training_metrics={
        "accuracy": "1.0",
        "precision": "0.95",
        "recall": "0.90",
        "f1_score": "0.92",
    },
    hyper_params={
        "learning_rate": "0.01",
        "num_epochs": "100",
        "batch_size": "32",
    },
)

client.entities.upsert(mlmodel)
