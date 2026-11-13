"""Ensure pgQueue's psycopg2 stack stays isolated from ingestion *sources*."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


def test_pgqueue_import_graph_isolated_from_ingestion_sources() -> None:
    """Fresh interpreter: package init + repository avoid ingestion sources; lazy exports work."""
    root = Path(__file__).resolve().parents[2]
    code = f"""
import sys
sys.path.insert(0, {root.as_posix()!r})

import orrery.pgqueue as pq
assert "orrery.pgqueue.consumer" not in sys.modules
assert "orrery.pgqueue.emitter" not in sys.modules

import orrery.pgqueue.repository  # noqa: F401
bad = [k for k in sys.modules if k.startswith("orrery.ingestion.source")]
assert not bad, "unexpectedly loaded: " + ", ".join(sorted(bad))

_ = pq.OrreryPgQueueConsumer
assert "orrery.pgqueue.consumer" in sys.modules
"""
    subprocess.run(
        [sys.executable, "-c", code],
        check=True,
        cwd=root,
    )
