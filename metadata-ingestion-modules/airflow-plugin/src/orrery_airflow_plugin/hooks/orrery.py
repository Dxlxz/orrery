import logging
import warnings
from typing import TYPE_CHECKING, Any, Dict, Iterable, List, Optional, Tuple, Union

from airflow.exceptions import AirflowException

# BaseHook moved to airflow.sdk.bases.hook in Airflow 3.1; on 3.0.x it is only at
# airflow.hooks.base. Import from the submodule (which is absent on 3.0 ->
# ModuleNotFoundError that ignore_missing_imports tolerates) rather than
# `from airflow.sdk import BaseHook` (present-but-attributeless on 3.0 -> mypy
# attr-defined). Matches the proven pattern used elsewhere in the codebase.
try:
    from airflow.sdk.bases.hook import BaseHook
except (ModuleNotFoundError, ImportError):
    from airflow.hooks.base import BaseHook  # type: ignore

from orrery.emitter.composite_emitter import CompositeEmitter
from orrery.emitter.generic_emitter import Emitter
from orrery.emitter.mcp import MetadataChangeProposalWrapper
from orrery.emitter.mcp_builder import mcps_from_mce
from orrery.errors import OrreryDeprecationWarning
from orrery.metadata.com.linkedin.pegasus2avro.mxe import (
    MetadataChangeEvent,
    MetadataChangeProposal,
)

if TYPE_CHECKING:
    from airflow.sdk import Connection

    from orrery.emitter.kafka_emitter import OrreryKafkaEmitter
    from orrery.emitter.rest_emitter import OrreryRestEmitter
    from orrery.emitter.synchronized_file_emitter import SynchronizedFileEmitter
    from orrery.ingestion.graph.client import OrreryGraph
    from orrery.ingestion.sink.orrery_kafka import KafkaSinkConfig

logger = logging.getLogger(__name__)


def _mces_as_mcps(
    items: Iterable[
        Union[
            MetadataChangeEvent, MetadataChangeProposal, MetadataChangeProposalWrapper
        ]
    ],
) -> Iterable[Union[MetadataChangeProposal, MetadataChangeProposalWrapper]]:
    """Converts MCEs to one MCP per aspect so that no MCE leaves the plugin."""
    warned = False
    for item in items:
        if isinstance(item, MetadataChangeEvent):
            if not warned:
                message = (
                    "Passing MetadataChangeEvent to Orrery hooks and OrreryEmitterOperator "
                    "is deprecated; it is sent as one MetadataChangeProposalWrapper per "
                    "aspect. Pass MetadataChangeProposalWrapper objects instead."
                )
                warnings.warn(message, OrreryDeprecationWarning, stacklevel=3)
                # Python only shows DeprecationWarning from __main__ and airflow.*
                # modules, so the warning alone never reaches the task log.
                logger.warning(message)
                warned = True
            mcps = list(mcps_from_mce(item))
            if not mcps:
                logger.warning(
                    f"MCE for {item.proposedSnapshot.urn} produced zero MCPs"
                )
            yield from mcps
        else:
            yield item


class OrreryRestHook(BaseHook):
    """
    Creates a Orrery Rest API connection used to send metadata to Orrery.
    Takes the endpoint for your Orrery Rest API in the Server Endpoint(host) field.

    URI example: ::

        AIRFLOW_CONN_ORRERY_REST_DEFAULT='orrery-rest://rest-endpoint'

    :param orrery_rest_conn_id: Reference to the Orrery Rest connection.
    :type orrery_rest_conn_id: str
    """

    conn_name_attr = "orrery_rest_conn_id"
    default_conn_name = "orrery_rest_default"
    conn_type = "orrery-rest"
    hook_name = "Orrery REST Server"

    def __init__(self, orrery_rest_conn_id: str = default_conn_name) -> None:
        super().__init__()
        self.orrery_rest_conn_id = orrery_rest_conn_id

    @staticmethod
    def get_connection_form_widgets() -> Dict[str, Any]:
        return {}

    @staticmethod
    def get_ui_field_behaviour() -> Dict:
        """Returns custom field behavior"""
        return {
            "hidden_fields": ["port", "schema", "login"],
            "relabeling": {
                "host": "Server Endpoint",
            },
        }

    def test_connection(self) -> Tuple[bool, str]:
        try:
            emitter = self.make_emitter()
            emitter.test_connection()
        except Exception as e:
            return False, str(e)

        return True, "Successfully connected to Orrery."

    def _get_config(self) -> Tuple[str, Optional[str], Optional[int]]:
        # We have a few places in the codebase that use this method directly, despite
        # it being "private". For now, we retain backwards compatibility by keeping
        # this method around, but should stop using it in the future.
        host, token, extra_args = self._get_config_v2()
        return host, token, extra_args.get("timeout_sec")

    def _get_config_v2(self) -> Tuple[str, Optional[str], Dict]:
        conn: "Connection" = self.get_connection(self.orrery_rest_conn_id)

        host = conn.host
        if not host:
            raise AirflowException("host parameter is required")
        if conn.port:
            if ":" in host:
                raise AirflowException(
                    "host parameter should not contain a port number if the port is specified separately"
                )
            host = f"{host}:{conn.port}"
        token = conn.password

        extra_args = conn.extra_dejson
        return (host, token, extra_args)

    def make_emitter(self) -> "OrreryRestEmitter":
        import orrery.emitter.rest_emitter
        from orrery.ingestion.graph.config import ClientMode

        host, token, extra_args = self._get_config_v2()
        return orrery.emitter.rest_emitter.OrreryRestEmitter(
            host,
            token,
            client_mode=ClientMode.INGESTION,
            orrery_component="airflow-plugin",
            **extra_args,
        )

    def make_graph(self) -> "OrreryGraph":
        return self.make_emitter().to_graph()

    def emit(
        self,
        items: Iterable[
            Union[
                MetadataChangeEvent,
                MetadataChangeProposal,
                MetadataChangeProposalWrapper,
            ]
        ],
    ) -> None:
        emitter = self.make_emitter()

        for item in _mces_as_mcps(items):
            emitter.emit(item)

    # Retained for backwards compatibility.
    emit_mces = emit
    emit_mcps = emit


class OrreryKafkaHook(BaseHook):
    """
    Creates a Orrery Kafka connection used to send metadata to Orrery.
    Takes your kafka broker in the Kafka Broker(host) field.

    URI example: ::

        AIRFLOW_CONN_ORRERY_KAFKA_DEFAULT='orrery-kafka://kafka-broker'

    :param orrery_kafka_conn_id: Reference to the Orrery Kafka connection.
    :type orrery_kafka_conn_id: str
    """

    conn_name_attr = "orrery_kafka_conn_id"
    default_conn_name = "orrery_kafka_default"
    conn_type = "orrery-kafka"
    hook_name = "Orrery Kafka Sink"

    def __init__(self, orrery_kafka_conn_id: str = default_conn_name) -> None:
        super().__init__()
        self.orrery_kafka_conn_id = orrery_kafka_conn_id

    @staticmethod
    def get_connection_form_widgets() -> Dict[str, Any]:
        return {}

    @staticmethod
    def get_ui_field_behaviour() -> Dict:
        """Returns custom field behavior"""
        return {
            "hidden_fields": ["port", "schema", "login", "password"],
            "relabeling": {
                "host": "Kafka Broker",
            },
        }

    def _get_config(self) -> "KafkaSinkConfig":
        import orrery.ingestion.sink.orrery_kafka

        conn = self.get_connection(self.orrery_kafka_conn_id)
        obj = conn.extra_dejson
        obj.setdefault("connection", {})
        if conn.host is not None:
            if "bootstrap" in obj["connection"]:
                raise AirflowException(
                    "Kafka broker specified twice (present in host and extra)"
                )
            obj["connection"]["bootstrap"] = ":".join(
                map(str, filter(None, [conn.host, conn.port]))
            )
        config = orrery.ingestion.sink.orrery_kafka.KafkaSinkConfig.model_validate(obj)
        return config

    def make_emitter(self) -> "OrreryKafkaEmitter":
        import orrery.emitter.kafka_emitter

        sink_config = self._get_config()
        return orrery.emitter.kafka_emitter.OrreryKafkaEmitter(sink_config)

    def emit(
        self,
        items: Iterable[
            Union[
                MetadataChangeEvent,
                MetadataChangeProposal,
                MetadataChangeProposalWrapper,
            ]
        ],
    ) -> None:
        emitter = self.make_emitter()
        errors = []

        def callback(exc, msg):
            if exc:
                errors.append(exc)

        for item in _mces_as_mcps(items):
            emitter.emit(item, callback)

        emitter.flush()

        if errors:
            raise AirflowException(f"failed to push some metadata: {errors}")

    # Retained for backwards compatibility.
    emit_mces = emit
    emit_mcps = emit


class SynchronizedFileHook(BaseHook):
    conn_name_attr = "orrery_file_conn_id"
    default_conn_name = "orrery_file_default"
    conn_type = "orrery-file"
    hook_name = "Orrery File Sink"

    def __init__(self, orrery_conn_id: str = default_conn_name) -> None:
        super().__init__()
        self.orrery_conn_id = orrery_conn_id

    @staticmethod
    def get_connection_form_widgets() -> Dict[str, Any]:
        return {}

    @staticmethod
    def get_ui_field_behaviour() -> Dict:
        """Returns custom field behavior"""
        return {
            "hidden_fields": ["port", "schema", "login", "password", "extra"],
            "relabeling": {
                "host": "Output File Path",
            },
        }

    def make_emitter(self) -> "SynchronizedFileEmitter":
        from orrery.emitter.synchronized_file_emitter import SynchronizedFileEmitter

        conn = self.get_connection(self.orrery_conn_id)
        filename = conn.host
        if not filename:
            raise AirflowException("filename parameter is required")

        return SynchronizedFileEmitter(filename=filename)

    def emit(
        self,
        items: Iterable[
            Union[
                MetadataChangeEvent,
                MetadataChangeProposal,
                MetadataChangeProposalWrapper,
            ]
        ],
    ) -> None:
        emitter = self.make_emitter()

        for item in _mces_as_mcps(items):
            emitter.emit(item)


class OrreryGenericHook(BaseHook):
    """
    Emits metadata using either the OrreryRestHook or the
    OrreryKafkaHook. Set up a Orrery Rest or Kafka connection to use.

    :param orrery_conn_id: Reference to the Orrery connection.
    :type orrery_conn_id: str
    """

    def __init__(self, orrery_conn_id: str) -> None:
        super().__init__()
        self.orrery_conn_id = orrery_conn_id

    def get_underlying_hook(
        self,
    ) -> Union[OrreryRestHook, OrreryKafkaHook, SynchronizedFileHook]:
        conn = self.get_connection(self.orrery_conn_id)

        # We need to figure out the underlying hook type. First check the
        # conn_type. If that fails, attempt to guess using the conn id name.
        if (
            conn.conn_type == OrreryRestHook.conn_type
            or conn.conn_type == OrreryRestHook.conn_type.replace("-", "_")
        ):
            return OrreryRestHook(self.orrery_conn_id)
        elif (
            conn.conn_type == OrreryKafkaHook.conn_type
            or conn.conn_type == OrreryKafkaHook.conn_type.replace("-", "_")
        ):
            return OrreryKafkaHook(self.orrery_conn_id)
        elif (
            conn.conn_type == SynchronizedFileHook.conn_type
            or conn.conn_type == SynchronizedFileHook.conn_type.replace("-", "_")
        ):
            return SynchronizedFileHook(self.orrery_conn_id)
        elif "rest" in self.orrery_conn_id:
            return OrreryRestHook(self.orrery_conn_id)
        elif "kafka" in self.orrery_conn_id:
            return OrreryKafkaHook(self.orrery_conn_id)
        else:
            raise AirflowException(
                f"Orrery cannot handle conn_type {conn.conn_type} in {conn}"
            )

    def make_emitter(self) -> Emitter:
        return self.get_underlying_hook().make_emitter()

    def emit(
        self,
        items: Iterable[
            Union[
                MetadataChangeEvent,
                MetadataChangeProposal,
                MetadataChangeProposalWrapper,
            ]
        ],
    ) -> None:
        return self.get_underlying_hook().emit(items)

    # Retained for backwards compatibility.
    emit_mces = emit


class OrreryCompositeHook(BaseHook):
    """
    A hook that can emit metadata to multiple Orrery instances.

    :param orrery_conn_ids: References to the Orrery connections.
    :type orrery_conn_ids: List[str]
    """

    def __init__(self, orrery_conn_ids: List[str]) -> None:
        self.orrery_conn_ids = orrery_conn_ids

    def make_emitter(self) -> CompositeEmitter:
        return CompositeEmitter(
            [
                self._get_underlying_hook(conn_id).make_emitter()
                for conn_id in self.orrery_conn_ids
            ]
        )

    def emit(
        self,
        items: Iterable[
            Union[
                MetadataChangeEvent,
                MetadataChangeProposal,
                MetadataChangeProposalWrapper,
            ]
        ],
    ) -> None:
        emitter = self.make_emitter()

        for item in _mces_as_mcps(items):
            emitter.emit(item)

    def _get_underlying_hook(self, conn_id: str) -> OrreryGenericHook:
        return OrreryGenericHook(conn_id)
