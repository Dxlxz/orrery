import logging
from datetime import datetime, timedelta
from typing import Optional

from pydantic import Field

from orrery.api.circuit_breaker.circuit_breaker import (
    AbstractCircuitBreaker,
    CircuitBreakerConfig,
)
from orrery.api.graphql import Operation

logger: logging.Logger = logging.getLogger(__name__)


class OperationCircuitBreakerConfig(CircuitBreakerConfig):
    time_delta: timedelta = Field(
        default=(timedelta(days=1)),
        description="In what timeframe should accept an operation result if updated field is not available for the dataset",
    )


class OperationCircuitBreaker(AbstractCircuitBreaker):
    r"""
    Orrery Operation Circuit Breaker

    The circuit breaker checks if there is an operation metadata for the dataset.
    If there is no valid Operation metadata then the circuit breaker fails.
    """

    config: OperationCircuitBreakerConfig
    operation_api: Operation

    def __init__(self, config: OperationCircuitBreakerConfig):
        _token = config.orrery_token.get_secret_value() if config.orrery_token else None
        self.config = config
        self.operation_api = Operation(
            orrery_host=config.orrery_host,
            orrery_token=_token,
            orrery_auth=config.orrery_auth,
            timeout=config.timeout,
        )

    def is_circuit_breaker_active(
        self,
        urn: str,
        partition: Optional[str] = None,
        source_type: Optional[str] = None,
        operation_type: Optional[str] = None,
    ) -> bool:
        r"""
        Checks if the circuit breaker is active

        :param urn: The Orrery dataset unique identifier.
        :param orrery_rest_conn_id: The REST Orrery connection id to communicate with Orrery
            which is set as Airflow connection.
        :param partition: The partition to check the operation.
        :param source_type: The source type to filter on. If not set it will accept any source type.
            See valid types here: https://docs.orrery.dev/docs/graphql/enums#operationsourcetype
        :param operation_type: The operation type to filter on. If not set it will accept any source type.
            See valid types here: https://docs.orrery.dev/docs/graphql/enums/#operationtype
        """

        start_time_millis: int = int(
            (datetime.now() - self.config.time_delta).timestamp() * 1000
        )
        operations = self.operation_api.query_operations(
            urn,
            start_time_millis=start_time_millis,
            partition=partition,
            source_type=source_type,
            operation_type=operation_type,
        )
        logger.info(f"Operations: {operations}")
        for operation in operations:
            if (
                operation.get("lastUpdatedTimestamp")
                and operation["lastUpdatedTimestamp"] >= start_time_millis
            ):
                return False

        return True
