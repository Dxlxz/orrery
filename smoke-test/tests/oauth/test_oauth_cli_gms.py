"""End-to-end OAuth smoke test: SDK/CLI authenticating to GMS with a real OAuth
token fetched from an OSS IdP (Keycloak).

Run from inside the compose network (see docker-compose.oauth.yml / README.md):

    docker compose -f <quickstart> -f docker-compose.oauth.yml exec tester sh -c \\
      "pip install -e /repo/metadata-ingestion && pytest /smoke/test_oauth_cli_gms.py -v"

Required env (set on the `tester` service):
  ORRERY_GMS_URL          - internal GMS URL, e.g. http://orrery-gms:8080
  KEYCLOAK_TOKEN_ENDPOINT  - Keycloak token endpoint for the `orrery` realm
"""

import importlib.util
import json
import logging
import os
import uuid

import pytest
import requests
from requests.exceptions import HTTPError

from orrery.cli.config_utils import load_client_config
from orrery.configuration.common import OperationalError
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.graph.config import OrreryClientConfig
from orrery.ingestion.run.pipeline import Pipeline
from orrery.upgrade import upgrade

logger = logging.getLogger(__name__)

GMS = os.environ.get("ORRERY_GMS_URL")
TOKEN_ENDPOINT = os.environ.get("KEYCLOAK_TOKEN_ENDPOINT")

# Env-based auth (ORRERY_AUTH_TYPE) shipped after the auth layer itself; skip
# those tests gracefully on an older SDK instead of failing collection. The CI
# harness overlays the triggering ref's SDK source onto the released install
# (see run-oauth-ci.sh), so nothing skips there — and the CI script fails the
# run if anything does.
requires_env_auth = pytest.mark.skipif(
    importlib.util.find_spec("orrery.ingestion.auth.env") is None,
    reason="installed orrery predates env-based auth (orrery.ingestion.auth.env)",
)

# Opt-in: skipped unless a GMS configured for external OAuth AND a Keycloak realm
# are available (see README). Keeps it out of the default CI smoke run entirely —
# no Keycloak/GMS-OAuth setup means zero added CI runtime.
pytestmark = pytest.mark.skipif(
    not (GMS and TOKEN_ENDPOINT),
    reason="OAuth smoke test requires ORRERY_GMS_URL + KEYCLOAK_TOKEN_ENDPOINT (see README)",
)

# Requires the authenticated actor; a token GMS rejects (wrong audience) fails here.
_ME_QUERY = "{ me { corpUser { urn } } }"


def _graph(client_id: str, client_secret: str) -> OrreryGraph:
    config = OrreryClientConfig(
        server=GMS,
        auth={
            "type": "oidc_client_credentials",
            "config": {
                "token_endpoint": TOKEN_ENDPOINT,
                "client_id": client_id,
                "client_secret": client_secret,
            },
        },
    )
    # On an SDK without the auth layer (< 1.6.0.9) pydantic silently drops the
    # extra `auth` field, the graph sends no credentials, and the positive test
    # 401s while the negative test passes vacuously. Fail loudly instead.
    assert getattr(config, "auth", None) is not None, (
        "OrreryClientConfig dropped the 'auth' config — the installed "
        "orrery is too old for this test (needs >= 1.6.0.9)."
    )
    return OrreryGraph(config)


def test_oauth_authenticated_call_succeeds():
    # orrery-executor's token carries aud=orrery-gms, which GMS trusts.
    graph = _graph("orrery-executor", "orrery-executor-secret")
    result = graph.execute_graphql(_ME_QUERY)
    assert result["me"]["corpUser"]["urn"]


def test_wrong_audience_is_rejected():
    # orrery-wrong-aud's token carries aud=not-orrery -> GMS must reject it.
    # The 401 surfaces as a wrapped OperationalError (or raw HTTPError).
    graph = _graph("orrery-wrong-aud", "orrery-wrong-aud-secret")
    with pytest.raises((OperationalError, HTTPError)):
        graph.execute_graphql(_ME_QUERY)


def test_unauthenticated_request_is_rejected():
    # Guards the guard: if METADATA_SERVICE_AUTH_ENABLED regresses to false, GMS
    # accepts ANY request and every other test here passes vacuously. An
    # anonymous GraphQL call must be a 401.
    response = requests.post(
        f"{GMS}/api/graphql",
        json={"query": _ME_QUERY},
        timeout=30,
    )
    assert response.status_code == 401, (
        f"expected 401 for unauthenticated request, got {response.status_code} — "
        "is METADATA_SERVICE_AUTH_ENABLED=true on GMS?"
    )


def _set_env_auth(monkeypatch) -> None:
    monkeypatch.delenv("ORRERY_GMS_TOKEN", raising=False)
    monkeypatch.setenv("ORRERY_AUTH_TYPE", "oidc_client_credentials")
    monkeypatch.setenv("ORRERY_AUTH_TOKEN_ENDPOINT", TOKEN_ENDPOINT or "")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_ID", "orrery-executor")
    monkeypatch.setenv("ORRERY_AUTH_CLIENT_SECRET", "orrery-executor-secret")


@requires_env_auth
def test_env_auth_resolves_client_config(monkeypatch):
    # ORRERY_AUTH_TYPE alone (no ORRERY_GMS_TOKEN) must yield an auth-carrying
    # client config — the path the CLI, default sink, and executor recipe
    # subprocesses resolve through.
    _set_env_auth(monkeypatch)
    config = load_client_config()
    assert config.token is None
    assert config.auth is not None and config.auth.type == "oidc_client_credentials"
    result = OrreryGraph(config).execute_graphql(_ME_QUERY)
    assert result["me"]["corpUser"]["urn"]


def _write_events(tmp_path, urn: str) -> str:
    events_path = tmp_path / "events.json"
    events_path.write_text(
        json.dumps(
            [
                {
                    "entityType": "dataset",
                    "entityUrn": urn,
                    "changeType": "UPSERT",
                    "aspectName": "status",
                    "aspect": {"json": {"removed": False}},
                }
            ]
        )
    )
    return str(events_path)


def _run_file_pipeline(tmp_path, sink_config, urn: str) -> None:
    """Run a file-source ingest of one status aspect for `urn`; raise on failure.

    report_to=None disables the default run-summary reporting: it writes
    orreryIngestionSourceInfo, which a dedicated GMS aspect validator
    authorizes regardless of REST_API_AUTHORIZATION_ENABLED — and granting
    the machine principal privileges is a deployment concern, not this
    authentication canary's subject.
    """
    recipe: dict = {
        "source": {"type": "file", "config": {"path": _write_events(tmp_path, urn)}},
    }
    if sink_config is not None:
        recipe["sink"] = {"type": "orrery-rest", "config": sink_config}
    pipeline = Pipeline.create(recipe, report_to=None)
    pipeline.run()
    pipeline.raise_from_status()


def _unique_test_urn() -> str:
    return (
        "urn:li:dataset:(urn:li:dataPlatform:oauthsmoke,"
        f"oauth_smoke.{uuid.uuid4().hex[:8]},PROD)"
    )


@pytest.fixture
def cleanup_urn():
    # Idempotency: every ingest test writes a unique URN and hard-deletes it,
    # authenticating the delete with the declarative auth config.
    urn = _unique_test_urn()
    yield urn
    try:
        _graph("orrery-executor", "orrery-executor-secret").delete_entity(
            urn, hard=True
        )
    except Exception as e:
        logger.warning(f"cleanup of {urn} failed (non-fatal): {e}")


@requires_env_auth
def test_ingest_default_sink_uses_env_auth(monkeypatch, tmp_path, cleanup_urn):
    # A recipe with NO sink block resolves the default orrery-rest sink from
    # the environment — the shape executor-run recipes use. Also exercises the
    # ctx.graph derivation from the OAuth sink (from_emitter).
    _set_env_auth(monkeypatch)
    _run_file_pipeline(tmp_path, sink_config=None, urn=cleanup_urn)


@requires_env_auth
def test_ingest_explicit_credentialless_sink_inherits_env_auth(
    monkeypatch, tmp_path, cleanup_urn
):
    # An explicit `sink: orrery-rest` block WITHOUT credentials (the common
    # UI-managed recipe shape) must inherit env OAuth instead of silently
    # emitting unauthenticated.
    _set_env_auth(monkeypatch)
    _run_file_pipeline(tmp_path, sink_config={"server": GMS}, urn=cleanup_urn)


def test_connector_graph_client_from_recipe_orrery_api(tmp_path, cleanup_urn):
    # The "plain" Orrery client every connector sees (pipeline ctx.graph),
    # configured DECLARATIVELY in the recipe (orrery_api.auth + sink auth) —
    # no env vars involved. After the ingest, an authenticated connector-style
    # read through ctx.graph must see the entity. Sink mode is sync so the
    # write is immediately readable (no eventual-consistency flake).
    auth_block = {
        "type": "oidc_client_credentials",
        "config": {
            "token_endpoint": TOKEN_ENDPOINT,
            "client_id": "orrery-executor",
            "client_secret": "orrery-executor-secret",
        },
    }
    recipe = {
        "orrery_api": {"server": GMS, "auth": auth_block},
        "source": {
            "type": "file",
            "config": {"path": _write_events(tmp_path, cleanup_urn)},
        },
        "sink": {
            "type": "orrery-rest",
            "config": {"server": GMS, "auth": auth_block, "mode": "SYNC"},
        },
    }
    # report_to=None: see _run_file_pipeline — run-summary reporting needs
    # privileges this canary's machine principal deliberately doesn't have.
    pipeline = Pipeline.create(recipe, report_to=None)
    pipeline.run()
    pipeline.raise_from_status()

    graph = pipeline.ctx.graph
    assert graph is not None
    assert graph.exists(cleanup_urn), (
        "connector-facing ctx.graph could not read back the entity it ingested"
    )


@requires_env_auth
def test_ingest_explicit_static_token_wins_over_env_auth(
    monkeypatch, tmp_path, cleanup_urn
):
    # Precedence: explicit credentials in the sink block beat the environment.
    # A bogus static token must fail with 401s even though valid env OAuth is
    # available — env auth must never silently override explicit recipe config.
    _set_env_auth(monkeypatch)
    with pytest.raises(Exception) as exc_info:
        _run_file_pipeline(
            tmp_path,
            sink_config={"server": GMS, "token": "bogus-static-token"},
            urn=cleanup_urn,
        )
    assert "401" in str(exc_info.value) or "Unauthorized" in str(exc_info.value), (
        f"expected an authentication failure, got: {exc_info.value!r}"
    )


@requires_env_auth
def test_version_check_config_fetch_uses_env_auth(monkeypatch):
    # The CLI version check fetches /config through OrreryGraph
    # (upgrade.fetch_server_config_via_graph), minting a real token from the
    # env-configured provider on the way. GMS serves /config unauthenticated
    # (it's in authentication.excludedPaths), so this does NOT prove GMS
    # accepted the token — it gates that the version-check path itself
    # (env auth -> load_client_config -> OrreryGraph -> token mint -> /config)
    # works when auth= replaces a static token, instead of crashing or hanging.
    _set_env_auth(monkeypatch)
    server_config = upgrade.fetch_server_config_via_graph()
    assert server_config.service_version
