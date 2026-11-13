from __future__ import annotations

from typing import TYPE_CHECKING, Optional, overload

from orrery.errors import SdkUsageError
from orrery.ingestion.graph.client import OrreryGraph, get_default_graph
from orrery.ingestion.graph.config import ClientMode, OrreryClientConfig
from orrery.sdk.entity_client import EntityClient
from orrery.sdk.lineage_client import LineageClient
from orrery.sdk.search_client import SearchClient

if TYPE_CHECKING:
    from orrery.sdk.resolver_client import ResolverClient


class OrreryClient:
    """Main client for interacting with Orrery.

    This class provides the primary interface for interacting with Orrery,
    including entity management, search, and resolution capabilities.

    The client can be initialized in three ways:
    1. With a server URL and optional token
    2. With a OrreryClientConfig object
    3. With an existing (legacy) :py:class:`OrreryGraph` instance
    """

    @overload
    def __init__(self, *, server: str, token: Optional[str] = None): ...
    @overload
    def __init__(self, *, config: OrreryClientConfig): ...
    @overload
    def __init__(self, *, graph: OrreryGraph): ...
    def __init__(
        self,
        *,
        server: Optional[str] = None,
        token: Optional[str] = None,
        graph: Optional[OrreryGraph] = None,
        config: Optional[OrreryClientConfig] = None,
    ):
        """Initialize a new OrreryClient instance.

        Args:
            server: The URL of the Orrery server (e.g. "http://localhost:8080").
            token: Optional authentication token.
            graph: An existing OrreryGraph instance to use.
            config: A OrreryClientConfig object with connection details.

        Raises:
            SdkUsageError: If invalid combinations of arguments are provided.
        """
        if server is not None:
            if config is not None:
                raise SdkUsageError("Cannot specify both server and config")
            if graph is not None:
                raise SdkUsageError("Cannot specify both server and graph")
            graph = OrreryGraph(config=OrreryClientConfig(server=server, token=token))
        elif config is not None:
            if graph is not None:
                raise SdkUsageError("Cannot specify both config and graph")
            graph = OrreryGraph(config=config)
        elif graph is None:
            raise SdkUsageError("Must specify either server, config, or graph")

        self._graph = graph

    def test_connection(self) -> None:
        self._graph.test_connection()

    @classmethod
    def from_env(
        cls,
        *,
        client_mode: ClientMode = ClientMode.SDK,
        orrery_component: Optional[str] = None,
    ) -> "OrreryClient":
        """Initialize a OrreryClient from the environment variables or ~/.orreryenv file.

        This will first check ORRERY_GMS_URL and ORRERY_GMS_TOKEN. If not present,
        it will read credentials from ~/.orreryenv. That file can be created using
        the `orrery init` command.

        If you're looking to specify the server/token in code, use the
        OrreryClient(server=..., token=...) constructor instead.

        Args:
            client_mode: [internal] The client mode to use. Defaults to "SDK".
            orrery_component: [internal] The Orrery component name to include in the user agent.

        Returns:
            A OrreryClient instance.
        """

        # Inspired by the DockerClient.from_env() method.
        # TODO: This one also reads from ~/.orreryenv, so the "from_env" name might be a bit confusing.
        # That file is part of the "environment", but is not a traditional "env variable".
        graph = get_default_graph(
            client_mode=client_mode,
            orrery_component=orrery_component,
        )

        return cls(graph=graph)

    @property
    def entities(self) -> EntityClient:
        return EntityClient(self)

    @property
    def resolve(self) -> "ResolverClient":
        try:
            from orrery_orrery_cloud.sdk import (  # type: ignore[import-not-found]
                ResolverClient,
            )
        except ImportError:
            # If the client is not installed, use the one from the SDK.
            from orrery.sdk.resolver_client import (  # type: ignore[assignment]
                ResolverClient,
            )
        return ResolverClient(self)

    @property
    def search(self) -> SearchClient:
        return SearchClient(self)

    @property
    def lineage(self) -> LineageClient:
        return LineageClient(self)

    @property
    def assertions(self):
        try:
            from orrery_orrery_cloud.sdk import (  # type: ignore[import-not-found]
                AssertionsClient,
            )

            return AssertionsClient(self)
        except ImportError as e:
            # Fallback only when the cloud package is missing. Re-raise other
            # ImportErrors (broken deps inside an installed cloud package).
            if "orrery_orrery_cloud" not in str(e):
                raise
            from orrery.sdk.assertion_client import AssertionClient

            return AssertionClient(self)

    @property
    def subscriptions(self):  # type: ignore[report-untyped-call]  # Not available due to circular import issues
        try:
            from orrery_orrery_cloud.sdk import SubscriptionClient
        except ImportError as e:
            if "orrery_orrery_cloud" in str(e):
                raise SdkUsageError(
                    "SubscriptionClient is not installed, please install it with `pip install orrery-cloud`"
                ) from e
            else:
                raise e
        return SubscriptionClient(self)
