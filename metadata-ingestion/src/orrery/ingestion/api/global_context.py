import contextlib
import contextvars
from typing import Iterator, Optional

from orrery.ingestion.graph.client import OrreryGraph

_graph_context = contextvars.ContextVar[Optional[OrreryGraph]]("orrery_graph_context")


def get_graph_context() -> Optional[OrreryGraph]:
    try:
        return _graph_context.get()
    except LookupError:
        return None


@contextlib.contextmanager
def set_graph_context(graph: Optional[OrreryGraph]) -> Iterator[None]:
    token = _graph_context.set(graph)
    try:
        yield
    finally:
        _graph_context.reset(token)
