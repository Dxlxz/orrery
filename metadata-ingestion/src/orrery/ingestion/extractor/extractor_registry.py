from orrery.ingestion.api.registry import PluginRegistry
from orrery.ingestion.api.source import Extractor
from orrery.ingestion.extractor.mce_extractor import WorkUnitRecordExtractor

extractor_registry = PluginRegistry[Extractor]()

# Add a defaults to extractor registry.
extractor_registry.register("generic", WorkUnitRecordExtractor)
