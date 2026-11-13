from typing import Optional

from dagster._core.storage.db_io_manager import DbIOManager
from dagster_snowflake.snowflake_io_manager import SnowflakeDbClient
from dagster_snowflake_pandas import SnowflakePandasIOManager
from pydantic import Field

from orrery_dagster_plugin.modules.storage.orrery_db_io_manager import OrreryDbIoManager


class OrrerySnowflakePandasIOManager(SnowflakePandasIOManager):
    orrery_env: Optional[str] = Field(
        default=None,
        description="The Orrery env where the materialized assets belongs to.",
    )
    orrery_base_url: Optional[str] = Field(
        default=None,
        description="The Orrery base url for generated Orrery url in asset metadata.",
    )

    def create_io_manager(self, context) -> DbIOManager:  # type: ignore[no-untyped-def]
        return OrreryDbIoManager(
            db_client=SnowflakeDbClient(),
            io_manager_name="OrrerySnowflakeIOManager",
            database=self.database,
            schema=self.schema_,
            type_handlers=self.type_handlers(),
            default_load_type=self.default_load_type(),
            orrery_env=self.orrery_env,
            orrery_base_url=self.orrery_base_url,
        )
