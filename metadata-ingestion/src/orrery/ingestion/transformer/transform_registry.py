from orrery.ingestion.api.registry import PluginRegistry
from orrery.ingestion.api.transform import Transformer

transform_registry = PluginRegistry[Transformer]()
transform_registry.register_from_entrypoint("orrery.ingestion.transformer.plugins")
