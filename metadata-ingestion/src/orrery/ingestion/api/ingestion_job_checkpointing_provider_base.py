from abc import abstractmethod
from dataclasses import dataclass
from typing import Any, Dict, NewType, Optional

from typing_extensions import Self

import orrery.emitter.mce_builder as builder
from orrery.configuration.common import ConfigModel
from orrery.ingestion.api.committable import CommitPolicy, StatefulCommittable
from orrery.ingestion.api.common import PipelineContext
from orrery.metadata.schema_classes import OrreryIngestionCheckpointClass

JobId = NewType("JobId", str)
CheckpointJobStateType = OrreryIngestionCheckpointClass
CheckpointJobStatesMap = Dict[JobId, CheckpointJobStateType]


class IngestionCheckpointingProviderConfig(ConfigModel):
    pass


@dataclass()
class IngestionCheckpointingProviderBase(StatefulCommittable[CheckpointJobStatesMap]):
    """
    The base class for all checkpointing state provider implementations.
    """

    def __init__(self, name: str, commit_policy: CommitPolicy = CommitPolicy.ALWAYS):
        # Set the initial state to an empty dict.
        super().__init__(name, commit_policy, {})

    @classmethod
    @abstractmethod
    def create(cls, config_dict: Dict[str, Any], ctx: PipelineContext) -> Self:
        pass

    @abstractmethod
    def commit(self) -> None:
        pass

    @abstractmethod
    def get_latest_checkpoint(
        self,
        pipeline_name: str,
        job_name: JobId,
    ) -> Optional[OrreryIngestionCheckpointClass]:
        pass

    @staticmethod
    def get_data_job_urn(
        orchestrator: str,
        pipeline_name: str,
        job_name: JobId,
    ) -> str:
        """
        Standardizes datajob urn minting for all ingestion job state providers.
        """
        return builder.make_data_job_urn(orchestrator, pipeline_name, job_name)
