from datetime import datetime

import orrery.metadata.schema_classes as models
from orrery.sdk import MlModelGroupUrn, OrreryClient

client = OrreryClient.from_env()

group_urn = MlModelGroupUrn(
    platform="mlflow",
    name="legacy-recommendation-models",
    env="PROD",
)

mlmodel_group = client.entities.get(group_urn)

deprecation_aspect = models.DeprecationClass(
    deprecated=True,
    note="This model group has been replaced by the new transformer-based recommendation models",
    decommissionTime=int(datetime.now().timestamp() * 1000),
    actor="urn:li:corpuser:orrery",
)

mlmodel_group._set_aspect(deprecation_aspect)

client.entities.update(mlmodel_group)
print(f"Deprecated ML model group: {group_urn}")
