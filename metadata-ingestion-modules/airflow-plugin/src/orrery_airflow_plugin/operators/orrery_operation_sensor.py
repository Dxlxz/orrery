import datetime
from typing import Any, List, Optional, Sequence, Union

from airflow.sdk import BaseSensorOperator

from orrery.api.circuit_breaker import (
    OperationCircuitBreaker,
    OperationCircuitBreakerConfig,
)
from orrery_airflow_plugin.hooks.orrery import OrreryRestHook


class OrreryOperationCircuitBreakerSensor(BaseSensorOperator):
    r"""
    Orrery Operation Circuit Breaker Sensor.

    :param urn: The Orrery dataset unique identifier. (templated)
    :param orrery_rest_conn_id: The REST orrery connection id to communicate with Orrery
        which is set as Airflow connection.
    :param partition: The partition to check the operation.
    :param source_type: The source type to filter on. If not set it will accept any source type.
        See valid values at: https://docs.orrery.dev/docs/graphql/enums#operationsourcetype
    :param operation_type: The operation type to filter on. If not set it will accept any source type.
        See valid values at: https://docs.orrery.dev/docs/graphql/enums/#operationtype
    """

    template_fields: Sequence[str] = (
        "urn",
        "partition",
        "source_type",
        "operation_type",
    )
    circuit_breaker: OperationCircuitBreaker
    urn: Union[List[str], str]
    partition: Optional[str]
    source_type: Optional[str]
    operation_type: Optional[str]

    def __init__(  # type: ignore[no-untyped-def]
        self,
        *,
        urn: Union[List[str], str],
        orrery_rest_conn_id: Optional[str] = None,
        time_delta: Optional[datetime.timedelta] = datetime.timedelta(days=1),
        partition: Optional[str] = None,
        source_type: Optional[str] = None,
        operation_type: Optional[str] = None,
        **kwargs,
    ) -> None:
        super().__init__(**kwargs)
        hook: OrreryRestHook
        if orrery_rest_conn_id is not None:
            hook = OrreryRestHook(orrery_rest_conn_id=orrery_rest_conn_id)
        else:
            hook = OrreryRestHook()

        host, password, timeout_sec = hook._get_config()

        self.urn = urn
        self.partition = partition
        self.operation_type = operation_type
        self.source_type = source_type

        config: OperationCircuitBreakerConfig = OperationCircuitBreakerConfig(
            orrery_host=host,
            orrery_token=password,
            timeout=timeout_sec,
            time_delta=time_delta,
        )

        self.circuit_breaker = OperationCircuitBreaker(config=config)

    def poke(self, context: Any) -> bool:
        if "orrery_silence_circuit_breakers" in context["dag_run"].conf:
            self.log.info(
                "Circuit breaker is silenced because orrery_silence_circuit_breakers config is set"
            )
            return True

        self.log.info(f"Checking if dataset {self.urn} is ready to be consumed")
        if isinstance(self.urn, str):
            urns = [self.urn]
        elif isinstance(self.urn, list):
            urns = self.urn
        else:
            raise Exception(f"urn parameter has invalid type {type(self.urn)}")

        for urn in urns:
            self.log.info(f"Checking if dataset {self.urn} is ready to be consumed")
            ret = self.circuit_breaker.is_circuit_breaker_active(
                urn=urn,
                partition=self.partition,
                operation_type=self.operation_type,
                source_type=self.source_type,
            )
            if ret:
                self.log.info(f"Dataset {self.urn} is not in consumable state")
                return False

        return True
