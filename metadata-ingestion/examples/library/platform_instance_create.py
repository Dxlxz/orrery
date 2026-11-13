# metadata-ingestion/examples/library/platform_instance_create.py
import os

import orrery.emitter.mce_builder as builder
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.schema_classes import DataPlatformInstancePropertiesClass

# Create the platform instance URN
platform_instance_urn = builder.make_dataplatform_instance_urn(
    platform="mysql", instance="production-mysql-cluster"
)

# Define properties for the platform instance
platform_properties = DataPlatformInstancePropertiesClass(
    name="Production MySQL Cluster",
    description="Primary MySQL database cluster serving production workloads in US West region",
    customProperties={
        "region": "us-west-2",
        "environment": "production",
        "cluster_size": "3-node",
        "version": "8.0.35",
    },
    externalUrl="https://cloud.mysql.com/console/clusters/prod-cluster",
)

# Create metadata change proposal
platform_instance_mcp = MetadataChangeProposalWrapper(
    entityUrn=platform_instance_urn,
    aspect=platform_properties,
)

# Emit metadata to Orrery
gms_server = os.getenv("ORRERY_GMS_URL", "http://localhost:8080")
token = os.getenv("ORRERY_GMS_TOKEN")
emitter = OrreryRestEmitter(gms_server=gms_server, token=token)
emitter.emit_mcp(platform_instance_mcp)

print(f"Created platform instance: {platform_instance_urn}")
