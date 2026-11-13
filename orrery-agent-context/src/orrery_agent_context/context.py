"""Context management for Orrery tools.

This module provides a context manager pattern for managing OrreryClient instances
across tool calls without explicit parameter passing.
"""

import contextvars
from typing import TYPE_CHECKING, Optional

if TYPE_CHECKING:
    from orrery.ingestion.graph.client import OrreryGraph
    from orrery.sdk.main_client import OrreryClient

# Context variable to store the current OrreryClient instance
_client_context: contextvars.ContextVar[Optional["OrreryClient"]] = (
    contextvars.ContextVar("orrery_client", default=None)
)


def get_orrery_client() -> "OrreryClient":
    """Get the current OrreryClient from context.

    Returns:
        OrreryClient instance from context

    Raises:
        RuntimeError: If no client is set in context
    """
    client = _client_context.get()
    if client is None:
        raise RuntimeError(
            "No OrreryClient in context. "
            "Make sure to use OrreryContext context manager or set_client() before calling tools."
        )
    return client


def get_graph() -> "OrreryGraph":
    """Get the current OrreryGraph from context (convenience method).

    Returns:
        OrreryGraph instance from the client in context

    Raises:
        RuntimeError: If no client is set in context
    """
    return get_orrery_client()._graph


def set_client(client: "OrreryClient") -> contextvars.Token:
    """Set the OrreryClient in context.

    Args:
        client: OrreryClient instance to set

    Returns:
        Token that can be used to reset the context
    """
    return _client_context.set(client)


def reset_client(token: contextvars.Token) -> None:
    """Reset the OrreryClient context to its previous value.

    Args:
        token: Token returned by set_client()
    """
    _client_context.reset(token)


class OrreryContext:
    """Context manager for Orrery tool execution.

    This context manager sets the OrreryClient in context for the duration
    of the with block, allowing tools to access it without explicit parameter passing.

    Example:
        from orrery.sdk.main_client import OrreryClient
        from orrery_agent_context.context import OrreryContext
        from orrery_agent_context.mcp_tools import search

        client = OrreryClient(...)

        with OrreryContext(client):
            results = search(query="users")  # No client parameter needed!
    """

    def __init__(self, client: "OrreryClient"):
        """Initialize the context manager.

        Args:
            client: OrreryClient instance to use in this context
        """
        self.client = client
        self._token: Optional[contextvars.Token] = None

    def __enter__(self) -> "OrreryClient":
        """Enter the context and set the client.

        Returns:
            The OrreryClient instance
        """
        self._token = set_client(self.client)
        return self.client

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        """Exit the context and reset the client."""
        if self._token is not None:
            reset_client(self._token)
            self._token = None
