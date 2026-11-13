"""Tests for env-based auth config (ORRERY_AUTH_TYPE): parsing per provider,
its wiring into load_client_config (what ingestion default sinks and the CLI
use), the rest-sink env-merge and shared provider, the __from_env__ emitter
path, and precedence over static tokens (env, ~/.orreryenv, sink blocks)."""

import concurrent.futures
import logging
from pathlib import Path

import pytest
import yaml

import orrery.ingestion.sink.orrery_rest as sink_mod
from orrery.cli.config_utils import MissingConfigError, load_client_config
from orrery.configuration.common import ConfigurationError
from orrery.emitter.rest_emitter import OrreryRestEmitter
from orrery.emitter.token_provider import StaticTokenProvider, TokenProviderAuth
from orrery.ingestion.auth.env import build_auth_config_from_env
from orrery.ingestion.sink.orrery_rest import OrreryRestSink, OrreryRestSinkConfig

_AUTH_ENV_VARS = [
    "ORRERY_PROFILE",
    "ORRERY_SKIP_CONFIG",
    "ORRERY_AUTH_TYPE",
    "ORRERY_AUTH_TOKEN_FILE",
    "ORRERY_AUTH_AUDIENCE",
    "ORRERY_AUTH_SCOPE",
    "ORRERY_AUTH_TOKEN_ENDPOINT",
    "ORRERY_AUTH_CLIENT_ID",
    "ORRERY_AUTH_CLIENT_SECRET",
    "ORRERY_AUTH_AZURE_TENANT_ID",
    "ORRERY_AUTH_AZURE_CLIENT_ID",
    "ORRERY_AUTH_AZURE_SCOPE",
    "ORRERY_AUTH_AZURE_CLIENT_SECRET",
    "ORRERY_GMS_URL",
    "ORRERY_GMS_HOST",
    "ORRERY_GMS_PORT",
    "ORRERY_GMS_TOKEN",
]


@pytest.fixture(autouse=True)
def _clean_env(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("HOME", str(tmp_path))
    for var in _AUTH_ENV_VARS:
        monkeypatch.delenv(var, raising=False)


def test_returns_none_when_auth_type_unset():
    assert build_auth_config_from_env() is None


def test_oidc_client_credentials(monkeypatch):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")
    monkeypatch.setenv("ORRERY_AUTH_SCOPE", "openid")

    auth = build_auth_config_from_env()
    assert auth is not None and auth.type == "oidc_client_credentials"
    config = auth.config
    assert isinstance(config, dict)
    assert config["token_endpoint"] == "https://idp/token"
    assert config["client_id"] == "cid"
    assert config["scope"] == "openid"
    # Secrets are SecretStr so dumps/reprs mask them.
    assert config["client_secret"].get_secret_value() == "s3cret-value"
    assert "s3cret-value" not in repr(auth)


def test_azure_entra_secret_optional(monkeypatch):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "azure_entra")
    monkeypatch.setenv("ORRERY_AUTH_AZURE_TENANT_ID", "tid")
    monkeypatch.setenv("ORRERY_AUTH_AZURE_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_AZURE_SCOPE", "api://orrery-gms/.default")

    auth = build_auth_config_from_env()
    assert auth is not None and auth.type == "azure_entra"
    assert auth.config == {
        "tenant_id": "tid",
        "client_id": "cid",
        "scope": "api://orrery-gms/.default",
    }


def test_pat_token_file(monkeypatch, tmp_path):
    token_file = tmp_path / "token"
    token_file.write_text("pat-from-file")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "pat")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_FILE", str(token_file))

    auth = build_auth_config_from_env()
    assert auth is not None and auth.type == "pat"
    assert auth.config == {"token_file": str(token_file)}


def test_pat_falls_back_to_gms_token_without_warning(monkeypatch, caplog):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "pat")
    monkeypatch.setenv("ORRERY_GMS_TOKEN", "static-pat")

    with caplog.at_level(logging.WARNING):
        auth = build_auth_config_from_env()
    assert auth is not None and auth.type == "pat"
    config = auth.config
    assert isinstance(config, dict)
    assert config["token"].get_secret_value() == "static-pat"
    assert "static-pat" not in repr(auth)
    assert not any("ignoring the static token" in r.message for r in caplog.records)


def test_pat_token_file_wins_over_gms_token_and_warns(monkeypatch, tmp_path, caplog):
    token_file = tmp_path / "token"
    token_file.write_text("pat-from-file")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "pat")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_FILE", str(token_file))
    monkeypatch.setenv("ORRERY_GMS_TOKEN", "static-pat")

    with caplog.at_level(logging.WARNING):
        auth = build_auth_config_from_env()
    assert auth is not None
    assert auth.config == {"token_file": str(token_file)}
    assert any("ignoring the static token" in r.message for r in caplog.records)


def test_pat_without_any_source_names_both_vars(monkeypatch):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "pat")

    with pytest.raises(ConfigurationError) as exc:
        build_auth_config_from_env()
    assert "ORRERY_AUTH_TOKEN_FILE" in str(exc.value)
    assert "ORRERY_GMS_TOKEN" in str(exc.value)


def test_oauth_type_warns_when_static_token_ignored(monkeypatch, caplog):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "k8s_oidc")
    monkeypatch.setenv("ORRERY_GMS_TOKEN", "static-pat")

    with caplog.at_level(logging.WARNING):
        auth = build_auth_config_from_env()
    assert auth is not None
    assert any("ignoring the static token" in r.message for r in caplog.records)


def test_k8s_oidc_all_vars_optional(monkeypatch):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "k8s_oidc")

    auth = build_auth_config_from_env()
    assert auth is not None and auth.type == "k8s_oidc"
    assert auth.config == {}


def test_missing_required_vars_named_in_error(monkeypatch):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")

    with pytest.raises(ConfigurationError) as exc:
        build_auth_config_from_env()
    assert "ORRERY_AUTH_CLIENT_ID" in str(exc.value)
    assert "ORRERY_AUTH_CLIENT_SECRET" in str(exc.value)


def test_unknown_auth_type_raises(monkeypatch):
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "bogus")
    with pytest.raises(ConfigurationError):
        build_auth_config_from_env()


def test_load_client_config_uses_env_auth(monkeypatch):
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "k8s_oidc")

    config = load_client_config()
    assert config.server == "http://gms:8080"
    assert config.auth is not None and config.auth.type == "k8s_oidc"
    assert config.token is None


def test_load_client_config_env_auth_wins_over_static_token(monkeypatch):
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_GMS_TOKEN", "static-pat")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "k8s_oidc")

    config = load_client_config()
    assert config.auth is not None
    assert config.token is None


def test_load_client_config_static_token_without_auth_type(monkeypatch):
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_GMS_TOKEN", "static-pat")

    config = load_client_config()
    assert config.token == "static-pat"
    assert config.auth is None


def test_rest_sink_emitter_carries_auth(monkeypatch):
    # Regression: OrreryRestSink._make_emitter must forward config.auth to the
    # emitter, or the default ingestion sink silently emits unauthenticated in
    # OAuth mode even though load_client_config resolved auth correctly.
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")

    sink_config = OrreryRestSinkConfig(
        server="http://gms:8080", auth=build_auth_config_from_env()
    )
    emitter = OrreryRestSink._make_emitter(
        sink_config, auth=OrreryRestSink._resolve_auth(sink_config)
    )
    assert isinstance(emitter._session.auth, TokenProviderAuth)


def test_explicit_sink_without_credentials_inherits_env_auth(monkeypatch):
    # A recipe with an explicit `sink: orrery-rest` block that carries no
    # credentials must not silently bypass env OAuth — the sink inherits
    # ORRERY_AUTH_TYPE just like the default (sink-less) path does, provided
    # it points at the env-configured server.
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")

    sink_config = OrreryRestSinkConfig(server="http://gms:8080")
    assert isinstance(OrreryRestSink._resolve_auth(sink_config), TokenProviderAuth)


def test_explicit_sink_env_auth_requires_matching_server(monkeypatch, caplog):
    # Env OAuth applies only to the env-configured server: a credential-less
    # sink pointing at a different origin must not receive env-minted bearer
    # tokens (audience scoping limits use, not disclosure).
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")

    sink_config = OrreryRestSinkConfig(server="http://other-host:8080")
    with caplog.at_level(logging.WARNING):
        assert OrreryRestSink._resolve_auth(sink_config) is None
    assert any("http://other-host:8080" in r.message for r in caplog.records)


def test_explicit_sink_env_auth_skipped_without_env_server(monkeypatch, caplog):
    # Without ORRERY_GMS_URL there is no env-configured server to match, so
    # env OAuth is not merged into an explicit sink — loudly.
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")

    sink_config = OrreryRestSinkConfig(server="http://gms:8080")
    with caplog.at_level(logging.WARNING):
        assert OrreryRestSink._resolve_auth(sink_config) is None
    assert any("ORRERY_GMS_URL" in r.message for r in caplog.records)


def test_explicit_sink_token_wins_over_env_auth(monkeypatch):
    # Explicit credentials in the sink block beat the environment (config beats
    # env). This also prevents env OAuth tokens from being sent to a sink whose
    # server points somewhere other than the env-configured GMS.
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "k8s_oidc")

    sink_config = OrreryRestSinkConfig(server="http://gms:8080", token="recipe-pat")
    assert OrreryRestSink._resolve_auth(sink_config) is None  # static-token path


def test_sink_resolves_one_provider_shared_across_thread_emitters(monkeypatch):
    # One token provider per sink, shared by all worker-thread emitters — a
    # provider per thread would each hit the IdP independently (max_threads
    # token requests at startup and per refresh window).
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")

    calls = []
    real_build = sink_mod.build_token_provider

    def counting_build(auth):
        calls.append(1)
        return real_build(auth)

    monkeypatch.setattr(sink_mod, "build_token_provider", counting_build)

    sink_config = sink_mod.OrreryRestSinkConfig(server="http://gms:8080")
    resolved = sink_mod.OrreryRestSink._resolve_auth(sink_config)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        emitters = list(
            pool.map(
                lambda _: sink_mod.OrreryRestSink._make_emitter(
                    sink_config, auth=resolved
                ),
                range(4),
            )
        )
    assert len(calls) == 1  # provider resolved exactly once, not per thread
    assert all(e._session.auth is resolved for e in emitters)


def test_from_env_emitter_resolves_env_auth(monkeypatch):
    # The `__from_env__` construction path (components that defer all config to
    # env vars) must honor ORRERY_AUTH_TYPE, not just GMS_URL/GMS_TOKEN.
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_GMS_TOKEN", "static-pat")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")

    emitter = OrreryRestEmitter("__from_env__")
    assert isinstance(emitter._session.auth, TokenProviderAuth)
    # Env auth wins over the static env token — no baked Authorization header.
    assert "Authorization" not in emitter._session.headers


def test_from_env_server_resolved_when_auth_passed_in(monkeypatch):
    # A sink that resolved env auth itself passes auth= into the emitter; the
    # `__from_env__` server sentinel must still resolve instead of being fed
    # literally into fixup_gms_url.
    monkeypatch.setenv("ORRERY_GMS_URL", "http://gms:8080")
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", "https://idp/token")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "cid")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "s3cret-value")

    resolved = TokenProviderAuth(StaticTokenProvider("provider-token"))
    emitter = OrreryRestEmitter("__from_env__", auth=resolved)
    assert emitter._gms_server == "http://gms:8080"
    # The explicitly passed auth wins; the env static token is not baked in.
    assert emitter._session.auth is resolved
    assert "Authorization" not in emitter._session.headers


def test_load_client_config_file_branch_env_auth_overrides_stored_token(
    tmp_path, monkeypatch, caplog
):
    # The ~/.orreryenv branch of load_client_config must apply env auth over a
    # stored static token, and say so — silently rerouting credentials would
    # make auth-source debugging miserable.
    path = tmp_path / ".orreryenv"
    path.write_text(
        yaml.safe_dump({"gms": {"server": "http://gms:8080", "token": "stored-pat"}})
    )
    monkeypatch.setenv("HOME", str(tmp_path))
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "k8s_oidc")

    with caplog.at_level(logging.WARNING):
        config = load_client_config()
    assert config.token is None
    assert config.auth is not None and config.auth.type == "k8s_oidc"
    assert any("ignoring the static token" in r.message for r in caplog.records)


def test_missing_server_error_names_gms_url_when_env_auth_set(tmp_path, monkeypatch):
    # A fully env-configured OAuth container missing only ORRERY_GMS_URL should
    # be told exactly that, not to run `orrery init`.
    monkeypatch.setenv("HOME", str(tmp_path))
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "k8s_oidc")

    with pytest.raises(MissingConfigError, match="ORRERY_GMS_URL"):
        load_client_config()
