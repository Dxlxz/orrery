import orrery.emitter.mce_builder as builder
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.metadata.com.linkedin.pegasus2avro.datajob import DataJobInfoClass
from orrery.metadata.schema_classes import DataFlowInfoClass

# Construct the DataJobInfo aspect with the job -> flow lineage.
dataflow_urn = builder.make_data_flow_urn(
    orchestrator="airflow", flow_id="flow_old_api", cluster="prod"
)

dataflow_info = DataFlowInfoClass(name="LowLevelApiFlow")

dataflow_info_mcp = MetadataChangeProposalWrapper(
    entityUrn=dataflow_urn,
    aspect=dataflow_info,
)

datajob_info = DataJobInfoClass(name="My Job 1", type="AIRFLOW", flowUrn=dataflow_urn)

# Construct a MetadataChangeProposalWrapper object with the DataJobInfo aspect.
# NOTE: This will overwrite all of the existing dataJobInfo aspect information associated with this job.
datajob_info_mcp = MetadataChangeProposalWrapper(
    entityUrn=builder.make_data_job_urn(
        orchestrator="airflow", flow_id="flow_old_api", job_id="job1", cluster="prod"
    ),
    aspect=datajob_info,
)

# Create an emitter to the GMS REST API.
emitter = OrreryRestEmitter("http://localhost:8080")

# Emit metadata!
emitter.emit_mcp(dataflow_info_mcp)
emitter.emit_mcp(datajob_info_mcp)
