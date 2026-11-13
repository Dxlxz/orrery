from typing import Dict, Type

from orrery.api.entities.assertion.compiler_interface import AssertionCompiler
from orrery.integrations.assertion.snowflake.compiler import SnowflakeAssertionCompiler

ASSERTION_PLATFORMS: Dict[str, Type[AssertionCompiler]] = {
    "snowflake": SnowflakeAssertionCompiler
}
