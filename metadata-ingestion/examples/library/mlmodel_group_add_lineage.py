from orrery.emitter import mce_builder
from orrery.metadata.urns import MlModelGroupUrn
from orrery.sdk import OrreryClient

client = OrreryClient.from_env()

group_urn = MlModelGroupUrn(
    platform="mlflow",
    name="recommendation-models",
    env="PROD",
)

training_job_urn = mce_builder.make_data_job_urn(
    orchestrator="airflow",
    flow_id="train_recommendation_model",
    job_id="training_task",
)

group = client.entities.get(group_urn)

group.add_training_job(training_job_urn)

client.entities.update(group)

print(f"Added training job {training_job_urn} to ML model group {group_urn}")
