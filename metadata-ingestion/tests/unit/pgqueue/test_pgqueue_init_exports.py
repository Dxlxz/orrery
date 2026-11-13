"""Lazy exports on :mod:`orrery.pgqueue`."""

from __future__ import annotations

import importlib

import pytest


@pytest.mark.parametrize(
    "name",
    [
        "OrreryPgQueueConsumer",
        "OrreryPgQueueEmitter",
        "PgQueueConsumedRecord",
        "build_pg_queue_event_meta",
    ],
)
def test_lazy_getattr_resolves(name: str) -> None:
    pq = importlib.import_module("orrery.pgqueue")
    assert getattr(pq, name) is not None


def test_unknown_attribute_raises() -> None:
    pq = importlib.import_module("orrery.pgqueue")
    with pytest.raises(AttributeError, match="has no attribute"):
        _ = pq.NotARealExport  # type: ignore[attr-defined]
