from orrery.ingestion.api.registry import PluginRegistry
from orrery.ingestion.fs.fs_base import FileSystem

fs_registry = PluginRegistry[FileSystem]()
fs_registry.register_from_entrypoint("orrery.fs.plugins")
