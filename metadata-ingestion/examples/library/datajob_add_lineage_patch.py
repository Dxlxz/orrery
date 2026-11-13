from orrery.emitter.mce_builder import (
    make_data_job_urn,
    make_dataset_urn,
    make_schema_field_urn,
)
from orrery.ingestion.graph.client import OrreryGraph, OrreryGraphConfig
from orrery.metadata.schema_classes import (
    FineGrainedLineageClass as FineGrainedLineage,
    FineGrainedLineageDownstreamTypeClass as FineGrainedLineageDownstreamType,
    FineGrainedLineageUpstreamTypeClass as FineGrainedLineageUpstreamType,
)
from orrery.specific.datajob import DataJobPatchBuilder

# Create Orrery Client
orrery_client = OrreryGraph(OrreryGraphConfig(server="http://localhost:8080"))

# Create DataJob URN
datajob_urn = make_data_job_urn(
    orchestrator="airflow", flow_id="dag_abc", job_id="task_456"
)

# Create DataJob Patch to Add Lineage
patch_builder = DataJobPatchBuilder(datajob_urn)
patch_builder.add_input_dataset(
    make_dataset_urn(platform="kafka", name="SampleKafkaDataset", env="PROD")
)
patch_builder.add_output_dataset(
    make_dataset_urn(platform="hive", name="SampleHiveDataset", env="PROD")
)
patch_builder.add_input_datajob(
    make_data_job_urn(orchestrator="airflow", flow_id="dag_abc", job_id="task_123")
)
patch_builder.add_input_dataset_field(
    make_schema_field_urn(
        parent_urn=make_dataset_urn(
            platform="hive", name="fct_users_deleted", env="PROD"
        ),
        field_path="user_id",
    )
)
patch_builder.add_output_dataset_field(
    make_schema_field_urn(
        parent_urn=make_dataset_urn(
            platform="hive", name="fct_users_created", env="PROD"
        ),
        field_path="user_id",
    )
)

# Update column-level lineage through the Data Job
lineage1 = FineGrainedLineage(
    upstreamType=FineGrainedLineageUpstreamType.FIELD_SET,
    upstreams=[
        make_schema_field_urn(make_dataset_urn("postgres", "raw_data.users"), "user_id")
    ],
    downstreamType=FineGrainedLineageDownstreamType.FIELD,
    downstreams=[
        make_schema_field_urn(
            make_dataset_urn("postgres", "analytics.user_metrics"),
            "user_id",
        )
    ],
    transformOperation="IDENTITY",
    confidenceScore=1.0,
)
patch_builder.add_fine_grained_lineage(lineage1)
patch_builder.remove_fine_grained_lineage(lineage1)
# Replaces all existing fine-grained lineages
patch_builder.set_fine_grained_lineages([lineage1])

patch_mcps = patch_builder.build()

# Emit DataJob Patch
for patch_mcp in patch_mcps:
    orrery_client.emit(patch_mcp)
