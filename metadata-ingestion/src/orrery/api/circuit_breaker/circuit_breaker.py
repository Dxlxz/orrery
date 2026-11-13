import logging
from abc import abstractmethod
from typing import Optional

from pydantic import Field

from orrery.configuration.common import ConfigModel, TransparentSecretStr
from orrery.ingestion.auth.registry import AuthConfig

logger = logging.getLogger(__name__)


class CircuitBreakerConfig(ConfigModel):
    orrery_host: str = Field(description="Url of the Orrery instance")
    orrery_token: Optional[TransparentSecretStr] = Field(
        default=None, description="The orrery token"
    )
    orrery_auth: Optional[AuthConfig] = Field(
        default=None,
        description="Declarative auth (e.g. OAuth client credentials), as an alternative to a static token. Mutually exclusive with orrery_token. The Airflow operators build their config from the connection, which cannot express an AuthConfig — set the ORRERY_AUTH_TYPE environment variables there instead.",
    )
    timeout: Optional[int] = Field(
        default=None,
        description="The number of seconds to wait for your client to establish a connection to a remote machine",
    )


class AbstractCircuitBreaker:
    @abstractmethod
    def is_circuit_breaker_active(self, urn: str) -> bool:
        pass
