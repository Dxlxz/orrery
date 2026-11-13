from unittest.mock import Mock

import pytest

from orrery.errors import ItemNotFoundError, MultipleItemsFoundError, SdkUsageError
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.graph.config import OrreryClientConfig
from orrery.metadata.urns import CorpUserUrn
from orrery.sdk.main_client import OrreryClient


@pytest.fixture
def mock_graph() -> Mock:
    graph = Mock(spec=OrreryGraph)
    graph.exists.return_value = False
    return graph


def test_client_creation(mock_graph: Mock) -> None:
    assert OrreryClient(graph=mock_graph)
    assert OrreryClient(server="https://example.com", token="token")


def test_client_init_errors(mock_graph: Mock) -> None:
    config = OrreryClientConfig(server="https://example.com", token="token")

    with pytest.raises(SdkUsageError):
        OrreryClient(server="https://example.com", graph=mock_graph)  # type: ignore
    with pytest.raises(SdkUsageError):
        OrreryClient(server="https://example.com", config=config)  # type: ignore
    with pytest.raises(SdkUsageError):
        OrreryClient(config=config, graph=mock_graph)  # type: ignore
    with pytest.raises(SdkUsageError):
        OrreryClient()  # type: ignore


def test_resolve_user(mock_graph: Mock) -> None:
    client = OrreryClient(graph=mock_graph)

    # This test doesn't really validate the graphql query or vars.
    # It probably makes more sense to test via smoke-tests.

    mock_graph.get_urns_by_filter.return_value = []
    with pytest.raises(ItemNotFoundError):
        client.resolve.user(name="User")

    mock_graph.get_urns_by_filter.return_value = ["urn:li:corpuser:user"]
    assert client.resolve.user(name="User") == CorpUserUrn("urn:li:corpuser:user")

    mock_graph.get_urns_by_filter.return_value = [
        "urn:li:corpuser:user",
        "urn:li:corpuser:user2",
    ]
    with pytest.raises(MultipleItemsFoundError):
        client.resolve.user(name="User")
