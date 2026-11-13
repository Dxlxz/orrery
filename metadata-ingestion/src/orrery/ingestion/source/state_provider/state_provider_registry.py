from orrery.ingestion.api.ingestion_job_checkpointing_provider_base import (
    IngestionCheckpointingProviderBase,
)
from orrery.ingestion.api.registry import PluginRegistry

ingestion_checkpoint_provider_registry = PluginRegistry[
    IngestionCheckpointingProviderBase
]()
ingestion_checkpoint_provider_registry.register_from_entrypoint(
    "orrery.ingestion.checkpointing_provider.plugins"
)
