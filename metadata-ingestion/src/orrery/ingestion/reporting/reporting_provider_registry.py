from orrery.ingestion.api.pipeline_run_listener import PipelineRunListener
from orrery.ingestion.api.registry import PluginRegistry

reporting_provider_registry = PluginRegistry[PipelineRunListener]()
reporting_provider_registry.register_from_entrypoint(
    "orrery.ingestion.reporting_provider.plugins"
)
