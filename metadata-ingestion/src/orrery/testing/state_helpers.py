from typing import Optional, cast

from orrery.ingestion.run.pipeline import Pipeline
from orrery.ingestion.source.state.checkpoint import Checkpoint
from orrery.ingestion.source.state.entity_removal_state import GenericCheckpointState
from orrery.ingestion.source.state.stale_entity_removal_handler import (
    StaleEntityRemovalHandler,
)
from orrery.ingestion.source.state.stateful_ingestion_base import (
    StatefulIngestionSourceBase,
)


def get_current_checkpoint_from_pipeline(
    pipeline: Pipeline,
) -> Optional[Checkpoint[GenericCheckpointState]]:
    """
    Helper method to retrieve the current checkpoint from a pipeline.
    """
    stateful_source = cast(StatefulIngestionSourceBase, pipeline.source)
    return stateful_source.state_provider.get_current_checkpoint(
        StaleEntityRemovalHandler.compute_job_id(
            getattr(stateful_source, "platform", "default")
        )
    )
