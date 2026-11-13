from orrery.ingestion.api.registry import PluginRegistry
from orrery.lite.lite_local import OrreryLiteLocal

lite_registry = PluginRegistry[OrreryLiteLocal]()

# SQLite is the default because it ships with Python. DuckDB needs the
# `orrery[duckdb]` extra, and the registry surfaces that itself when the
# import fails.
lite_registry.register_lazy("sqlite", "orrery.lite.sqlite_lite:SqliteLite")
lite_registry.register_lazy("duckdb", "orrery.lite.duckdb_lite:DuckDBLite")
