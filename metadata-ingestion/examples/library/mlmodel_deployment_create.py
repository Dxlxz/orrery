import os

import orrery.emitter.mce_builder as builder
import orrery.metadata.schema_classes as models
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter

# Get Orrery connection details from environment
gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")

# Create a deployment URN - the unique identifier for the ML model deployment
deployment_urn = builder.make_ml_model_deployment_urn(
    platform="sagemaker",
    deployment_name="recommendation-endpoint",
    env="PROD",
)

# Define deployment properties with status and custom properties
deployment_properties = models.MLModelDeploymentPropertiesClass(
    description="Production deployment of recommendation model on SageMaker",
    customProperties={
        "instance_type": "ml.m5.xlarge",
        "instance_count": "3",
        "endpoint_config": "recommendation-endpoint-config-v1",
    },
    externalUrl="https://console.aws.amazon.com/sagemaker/home#/endpoints/recommendation-endpoint",
    status=models.DeploymentStatusClass.IN_SERVICE,
)

# Create a metadata change proposal
event = MetadataChangeProposalWrapper(
    entityUrn=deployment_urn,
    aspect=deployment_properties,
)

# Emit the metadata
rest_emitter = OrreryRestEmitter(gms_server=gms_server, token=token)
rest_emitter.emit(event)
print(f"Created ML model deployment: {deployment_urn}")
