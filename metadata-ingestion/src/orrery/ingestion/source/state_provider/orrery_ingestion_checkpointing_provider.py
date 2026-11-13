import logging
import time
from datetime import datetime
from typing import Any, Dict, Optional

from orrery.configuration.common import ConfigurationError
from orrery.emitter.mce_builder import SYSTEM_ACTOR
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.ingestion.api.common import PipelineContext
from orrery.ingestion.api.ingestion_job_checkpointing_provider_base import (
    IngestionCheckpointingProviderBase,
    IngestionCheckpointingProviderConfig,
    JobId,
)
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.graph.config import OrreryClientConfig
from orrery.metadata.schema_classes import (
    AuditStampClass,
    OrreryIngestionCheckpointClass,
    StatusClass,
    SystemMetadataClass,
)

logger = logging.getLogger(__name__)


class OrreryIngestionStateProviderConfig(IngestionCheckpointingProviderConfig):
    orrery_api: Optional[OrreryClientConfig] = None


class OrreryIngestionCheckpointingProvider(IngestionCheckpointingProviderBase):
    orchestrator_name: str = "orrery"

    def __init__(
        self,
        graph: OrreryGraph,
    ):
        super().__init__(self.__class__.__name__)
        self.graph = graph
        if not self._is_server_stateful_ingestion_capable():
            raise ConfigurationError(
                "Orrery server is not capable of supporting stateful ingestion. "
                "Please consider upgrading to the latest server version to use this feature."
            )

    @classmethod
    def create(
        cls, config_dict: Dict[str, Any], ctx: PipelineContext
    ) -> "OrreryIngestionCheckpointingProvider":
        config = OrreryIngestionStateProviderConfig.model_validate(config_dict)
        if config.orrery_api is not None:
            return cls(OrreryGraph(config.orrery_api))
        elif ctx.graph:
            # Use the pipeline-level graph if set.
            return cls(ctx.graph)
        else:
            raise ValueError(
                "A graph instance is required. Either pass one in the pipeline context, or set it explicitly in the stateful ingestion provider config."
            )

    def _is_server_stateful_ingestion_capable(self) -> bool:
        server_config = self.graph.get_config() if self.graph else None
        if server_config and server_config.get("statefulIngestionCapable"):
            return True
        return False

    def get_latest_checkpoint(
        self,
        pipeline_name: str,
        job_name: JobId,
    ) -> Optional[OrreryIngestionCheckpointClass]:
        logger.debug(
            f"Querying for the latest ingestion checkpoint for pipelineName:'{pipeline_name}',"
            f" job_name:'{job_name}'"
        )

        data_job_urn = self.get_data_job_urn(
            self.orchestrator_name, pipeline_name, job_name
        )

        latest_checkpoint: Optional[OrreryIngestionCheckpointClass] = (
            self.graph.get_latest_timeseries_value(
                entity_urn=data_job_urn,
                aspect_type=OrreryIngestionCheckpointClass,
                filter_criteria_map={
                    "pipelineName": pipeline_name,
                },
            )
        )
        if latest_checkpoint:
            logger.debug(
                f"The last committed ingestion checkpoint for pipelineName:'{pipeline_name}',"
                f" job_name:'{job_name}' found with start_time:"
                f" {datetime.utcfromtimestamp(latest_checkpoint.timestampMillis / 1000)}"
            )
            return latest_checkpoint
        else:
            logger.debug(
                f"No committed ingestion checkpoint for pipelineName:'{pipeline_name}',"
                f" job_name:'{job_name}' found"
            )

        return None

    def commit(self) -> None:
        if not self.state_to_commit:
            logger.warning(f"No state available to commit for {self.name}")
            return None

        for job_name, checkpoint in self.state_to_commit.items():
            # Emit the ingestion state for each job
            logger.debug(
                f"Committing ingestion checkpoint for pipeline:'{checkpoint.pipelineName}', "
                f"job:'{job_name}'"
            )

            self.committed = False

            datajob_urn = self.get_data_job_urn(
                self.orchestrator_name,
                checkpoint.pipelineName,
                job_name,
            )

            # We soft-delete checkpoint dataJobs so they don't appear in search,
            # while still allowing timeseries checkpoint queries.
            # We include lifecycleLastUpdated with a fresh timestamp so the server
            # sees a content change on every run and refreshes createdOn. Without
            # this, repeated identical `removed=True` writes are detected as no-ops,
            # createdOn never refreshes, and GC's soft_deleted_entity_cleanup
            # eventually hard-deletes the entity (destroying checkpoint data).
            now_millis = int(time.time() * 1000)
            self.graph.emit_mcp(
                MetadataChangeProposalWrapper(
                    entityUrn=datajob_urn,
                    aspect=StatusClass(
                        removed=True,
                        lifecycleLastUpdated=AuditStampClass(
                            time=now_millis,
                            actor=SYSTEM_ACTOR,
                        ),
                    ),
                    systemMetadata=SystemMetadataClass(
                        lastObserved=now_millis,
                    ),
                )
            )
            self.graph.emit_mcp(
                MetadataChangeProposalWrapper(
                    entityUrn=datajob_urn,
                    aspect=checkpoint,
                )
            )

            self.committed = True

            logger.debug(
                f"Committed ingestion checkpoint for pipeline:'{checkpoint.pipelineName}', "
                f"job:'{job_name}'"
            )
