from typing_extensions import Literal

from orrery.configuration.common import ConfigModel


class SqlFilter(ConfigModel):
    type: Literal["sql"]
    sql: str


DatasetFilter = SqlFilter
