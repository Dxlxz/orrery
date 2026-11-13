import logging
from typing import Iterable

from orrery.emitter.mce_builder import make_data_flow_urn, orrery_guid
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.ingestion.api.workunit import MetadataWorkUnit
from orrery.ingestion.source.matillion_dpc.config import (
    MatillionSourceConfig,
    MatillionSourceReport,
)
from orrery.ingestion.source.matillion_dpc.constants import (
    MATILLION_PLATFORM,
)
from orrery.ingestion.source.matillion_dpc.matillion_container import (
    MatillionContainerHandler,
)
from orrery.ingestion.source.matillion_dpc.models import (
    MatillionProject,
    MatillionStreamingPipeline,
)
from orrery.metadata.schema_classes import (
    DataFlowInfoClass,
    StatusClass,
)

logger = logging.getLogger(__name__)


class MatillionStreamingHandler:
    def __init__(
        self,
        config: MatillionSourceConfig,
        report: MatillionSourceReport,
        container_handler: MatillionContainerHandler,
    ):
        self.config = config
        self.report = report
        self.container_handler = container_handler

    def _make_streaming_pipeline_urn(
        self, streaming_pipeline: MatillionStreamingPipeline, project: MatillionProject
    ) -> str:
        # Use GUID to ensure URN safety with special characters in pipeline names
        flow_id = orrery_guid(
            {
                "platform": MATILLION_PLATFORM,
                "instance": self.config.platform_instance,
                "env": self.config.env,
                "project_id": project.id,
                "streaming_pipeline_name": streaming_pipeline.name,
                "streaming_pipeline_id": streaming_pipeline.streaming_pipeline_id,
            }
        )
        return make_data_flow_urn(
            orchestrator=MATILLION_PLATFORM,
            flow_id=flow_id,
            cluster=self.config.env,
            platform_instance=self.config.platform_instance or project.name,
        )

    def emit_streaming_pipeline(
        self,
        streaming_pipeline: MatillionStreamingPipeline,
        project: MatillionProject,
    ) -> Iterable[MetadataWorkUnit]:
        if not self.config.streaming_pipeline_patterns.allowed(streaming_pipeline.name):
            self.report.filtered_streaming_pipelines.append(streaming_pipeline.name)
            return

        pipeline_urn = self._make_streaming_pipeline_urn(streaming_pipeline, project)
        pipeline_id = (
            streaming_pipeline.streaming_pipeline_id or streaming_pipeline.name
        )

        custom_properties = {
            "streaming_pipeline_id": pipeline_id,
            "project_id": project.id,
            "pipeline_type": "streaming",
        }

        # The Maia console has no dedicated deep-link route for streaming pipelines,
        # so no external URL is emitted rather than a link that does not resolve.
        dataflow_info = DataFlowInfoClass(
            name=streaming_pipeline.name,
            customProperties=custom_properties,
        )

        yield MetadataChangeProposalWrapper(
            entityUrn=pipeline_urn,
            aspect=dataflow_info,
        ).as_workunit()

        status_aspect = StatusClass(removed=False)
        yield MetadataChangeProposalWrapper(
            entityUrn=pipeline_urn,
            aspect=status_aspect,
        ).as_workunit()

        if self.config.extract_projects_to_containers:
            yield from self.container_handler.add_pipeline_to_container(
                pipeline_urn, project
            )

        self.report.report_streaming_pipeline_emitted()
        logger.debug(
            f"Emitted streaming pipeline: {streaming_pipeline.name} in project {project.name}"
        )
