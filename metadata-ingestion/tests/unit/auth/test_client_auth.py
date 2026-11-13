import pytest

from orrery.cli.datapack.loader import _build_datapack_sink_config
from orrery.configuration.common import ConfigurationError
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.emitter.token_provider import StaticTokenProvider, TokenProviderAuth
from orrery.ingestion.auth.registry import AuthConfig
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.graph.config import OrreryClientConfig
from orrery.ingestion.sink.orrery_rest import OrreryRestSink, OrreryRestSinkConfig


def test_graph_builds_session_auth_from_auth_config():
    config = OrreryClientConfig(
        server="http://gms",
        auth={"type": "static", "config": {"token": "abc"}},
    )
    graph = OrreryGraph(config)
    assert graph._session.auth is not None
    assert "Authorization" not in graph._session.headers


def test_token_and_auth_mutually_exclusive():
    with pytest.raises((ConfigurationError, ValueError)):
        OrreryClientConfig(
            server="http://gms",
            token="x",
            auth={"type": "static", "config": {"token": "y"}},
        )


def test_rest_sink_emitter_carries_auth():
    # Regression: OrreryRestSink._make_emitter must forward config.auth to the
    # emitter, or the default ingestion sink silently emits unauthenticated in
    # OAuth mode even though the sink config carries a resolved auth provider.
    auth = AuthConfig(type="static", config={"token": "t"})
    sink_config = OrreryRestSinkConfig(server="http://gms:8080", auth=auth)
    emitter = OrreryRestSink._make_emitter(
        sink_config, auth=OrreryRestSink._resolve_auth(sink_config)
    )
    session_auth = emitter._session.auth
    assert isinstance(session_auth, TokenProviderAuth)
    # Assert the token from *this* config reached the emitter, not merely that
    # some TokenProviderAuth was installed (which would pass even if the config
    # were ignored).
    assert session_auth._provider.get_token().token == "t"


def test_datapack_sink_config_carries_auth():
    # Regression: the datapack loader builds its own sink config from the client
    # config; dropping auth there made demo-data / datapack loads emit
    # unauthenticated in OAuth mode.
    auth = AuthConfig(type="static", config={"token": "t"})
    client_config = OrreryClientConfig(server="http://gms:8080", auth=auth)
    sink_config = _build_datapack_sink_config(client_config)
    assert sink_config["auth"] is client_config.auth
    assert "token" not in sink_config


def test_from_emitter_carries_session_auth():
    # Regression: OrreryGraph.from_emitter rebuilds the client config with only
    # the static token; the resolved requests auth object must be carried onto
    # the new graph's session or an OAuth emitter's graph loses credentials.
    auth = TokenProviderAuth(StaticTokenProvider("tok"))
    emitter = OrreryRestEmitter("http://gms:8080", auth=auth)
    graph = OrreryGraph.from_emitter(emitter)
    assert graph._session.auth is auth
