from typing import Optional

from orrery.configuration.common import ConfigModel


class BaseAssertion(ConfigModel):
    description: Optional[str] = None
