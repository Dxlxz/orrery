"""PostgreSQL-backed Orrery queue client (pgQueue DDL).

The psycopg2-facing modules (:mod:`orrery.pgqueue.repository`, :mod:`orrery.pgqueue.connection`)
do not import catalog ingestion sources. Emitter/consumer classes are loaded lazily so importing
those modules—or ``orrery.pgqueue``—does not pull Schema Registry / Avro helpers unless you use
the Kafka-compatible client entrypoints.
"""

from __future__ import annotations

from typing import Any

from orrery.pgqueue.config import (
    PayloadRouteKind,
    PgQueueAuthMode,
    PgQueueConnectionConfig,
    PgQueueConsumerConfig,
    PgQueueEmitterConfig,
    PgQueueTopicDefaultsConfig,
)
from orrery.pgqueue.repository import EnqueueBatchItem, PgQueueMessageHandle

__all__ = [
    "EnqueueBatchItem",
    "PgQueueMessageHandle",
    "PgQueueConsumedRecord",
    "build_pg_queue_event_meta",
    "OrreryPgQueueConsumer",
    "OrreryPgQueueEmitter",
    "PgQueueAuthMode",
    "PgQueueConnectionConfig",
    "PayloadRouteKind",
    "PgQueueConsumerConfig",
    "PgQueueEmitterConfig",
    "PgQueueTopicDefaultsConfig",
]


def __getattr__(name: str) -> Any:
    if name == "OrreryPgQueueConsumer":
        from orrery.pgqueue.consumer import OrreryPgQueueConsumer

        return OrreryPgQueueConsumer
    if name == "PgQueueConsumedRecord":
        from orrery.pgqueue.consumer import PgQueueConsumedRecord

        return PgQueueConsumedRecord
    if name == "build_pg_queue_event_meta":
        from orrery.pgqueue.consumer import build_pg_queue_event_meta

        return build_pg_queue_event_meta
    if name == "OrreryPgQueueEmitter":
        from orrery.pgqueue.emitter import OrreryPgQueueEmitter

        return OrreryPgQueueEmitter
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
