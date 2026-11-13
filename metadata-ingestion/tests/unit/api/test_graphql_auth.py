import pytest
from pydantic import ValidationError

from orrery.api.circuit_breaker import (
    AssertionCircuitBreaker,
    AssertionCircuitBreakerConfig,
    OperationCircuitBreaker,
    OperationCircuitBreakerConfig,
)
from orrery.api.graphql.base import BaseApi
from orrery.emitter.token_provider import TokenProviderAuth
from orrery.ingestion.auth.registry import AuthConfig

OAUTH = AuthConfig(
    type="oidc_client_credentials",
    config={
        "token_endpoint": "http://idp/token",
        "client_id": "cid",
        "client_secret": "csecret",
    },
)

# A static provider keeps the token assertions offline — the OIDC config above
# would need a live token endpoint to prove which credentials arrived.
STATIC = AuthConfig(type="static", config={"token": "cb-token"})


@pytest.fixture(autouse=True)
def _no_ambient_credentials(monkeypatch):
    for var in (
        "ORRERY_AUTH_TYPE",
        "ORRERY_GMS_TOKEN",
        "ORRERY_SYSTEM_CLIENT_ID",
        "ORRERY_SYSTEM_CLIENT_SECRET",
    ):
        monkeypatch.delenv(var, raising=False)


def _set_env_oauth(monkeypatch):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "http://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "csecret")


def test_static_token_bakes_header():
    session = BaseApi(orrery_host="http://gms", orrery_token="tok").graph._session
    assert session.headers.get("Authorization") == "Bearer tok"
    assert session.auth is None


def test_declarative_auth_installs_token_provider():
    session = BaseApi(orrery_host="http://gms", orrery_auth=OAUTH).graph._session
    assert isinstance(session.auth, TokenProviderAuth)
    assert "Authorization" not in session.headers


def test_env_oauth_installs_token_provider(monkeypatch):
    _set_env_oauth(monkeypatch)
    session = BaseApi(orrery_host="http://gms").graph._session
    assert isinstance(session.auth, TokenProviderAuth)


def test_static_token_beats_env_oauth(monkeypatch):
    _set_env_oauth(monkeypatch)
    session = BaseApi(orrery_host="http://gms", orrery_token="tok").graph._session
    assert session.auth is None
    assert session.headers.get("Authorization") == "Bearer tok"


def test_no_credentials_is_unauthenticated():
    session = BaseApi(orrery_host="http://gms").graph._session
    assert session.auth is None
    assert "Authorization" not in session.headers


def test_token_and_auth_together_are_rejected():
    with pytest.raises(ValidationError):
        BaseApi(orrery_host="http://gms", orrery_token="tok", orrery_auth=OAUTH)


def test_assertion_circuit_breaker_authenticates_the_live_client():
    breaker = AssertionCircuitBreaker(
        AssertionCircuitBreakerConfig(orrery_host="http://gms", orrery_auth=STATIC)
    )
    auth = breaker.assertion_api.graph._session.auth
    # Assert the token from *this* config reached the client, not merely that
    # some TokenProviderAuth was installed — the latter passes even when the
    # config is dropped on the floor, which is the defect this PR fixes.
    assert isinstance(auth, TokenProviderAuth)
    assert auth._provider.get_token().token == "cb-token"


def test_operation_circuit_breaker_authenticates_the_live_client():
    breaker = OperationCircuitBreaker(
        OperationCircuitBreakerConfig(orrery_host="http://gms", orrery_auth=STATIC)
    )
    auth = breaker.operation_api.graph._session.auth
    assert isinstance(auth, TokenProviderAuth)
    assert auth._provider.get_token().token == "cb-token"


def test_circuit_breaker_static_token_still_bakes_header():
    breaker = AssertionCircuitBreaker(
        AssertionCircuitBreakerConfig(orrery_host="http://gms", orrery_token="tok")
    )
    session = breaker.assertion_api.graph._session
    assert session.headers.get("Authorization") == "Bearer tok"
    assert session.auth is None
