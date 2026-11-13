import pytest

from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.emitter.token_provider import StaticTokenProvider, TokenProviderAuth
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.graph.config import OrreryClientConfig


def test_emitter_installs_session_auth_and_skips_static_header():
    auth = TokenProviderAuth(StaticTokenProvider("tok"), retry_on_401=False)
    emitter = OrreryRestEmitter(gms_server="http://gms", auth=auth)
    assert emitter._session.auth is auth
    # The static Authorization header must NOT be baked in when auth is used.
    assert "Authorization" not in emitter._session.headers


def test_emitter_static_token_still_works():
    emitter = OrreryRestEmitter(gms_server="http://gms", token="tok")
    assert emitter._session.headers.get("Authorization") == "Bearer tok"
    assert emitter._session.auth is None


@pytest.fixture
def system_credentials(monkeypatch: pytest.MonkeyPatch) -> None:
    for var in (
        "ORRERY_GMS_TOKEN",
        "ORRERY_AUTH_TYPE",
        "ORRERY_GMS_HOST",
        "ORRERY_GMS_PORT",
        "ORRERY_GMS_PROTOCOL",
    ):
        monkeypatch.delenv(var, raising=False)
    monkeypatch.setenv("ORRERY_SYSTEM_CLIENT_ID", "system")
    monkeypatch.setenv("ORRERY_SYSTEM_CLIENT_SECRET", "secret")
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")


@pytest.mark.usefixtures("system_credentials")
@pytest.mark.parametrize(
    "server", ["http://gms:8080", "http://gms:8080/", "http://GMS:8080"]
)
def test_system_credentials_sent_to_configured_server(server: str) -> None:
    emitter = OrreryRestEmitter(gms_server=server)
    assert emitter._session.headers.get("Authorization") == "Basic system:secret"


@pytest.mark.usefixtures("system_credentials")
@pytest.mark.parametrize(
    "server", ["http://other:8080", "http://gms:9090", "https://other.example"]
)
def test_system_credentials_not_sent_to_other_server(server: str) -> None:
    emitter = OrreryRestEmitter(gms_server=server)
    assert "Authorization" not in emitter._session.headers


@pytest.mark.usefixtures("system_credentials")
def test_system_credentials_not_sent_without_configured_server(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.delenv("ORRERY_GMS_URL")
    emitter = OrreryRestEmitter(gms_server="http://gms:8080")
    assert "Authorization" not in emitter._session.headers


@pytest.mark.usefixtures("system_credentials")
def test_explicit_token_sent_to_any_server() -> None:
    emitter = OrreryRestEmitter(gms_server="http://other:8080", token="tok")
    assert emitter._session.headers.get("Authorization") == "Bearer tok"


@pytest.mark.usefixtures("system_credentials")
def test_server_from_env_gets_system_credentials() -> None:
    emitter = OrreryRestEmitter(gms_server="__from_env__")
    assert emitter._session.headers.get("Authorization") == "Basic system:secret"


@pytest.mark.usefixtures("system_credentials")
def test_graph_follows_the_same_rule() -> None:
    graph = OrreryGraph(OrreryClientConfig(server="http://gms:8080"))
    assert graph._session.headers.get("Authorization") == "Basic system:secret"
    other = OrreryGraph(OrreryClientConfig(server="http://other:8080"))
    assert "Authorization" not in other._session.headers
