import logging
from typing import Dict, List, Optional, Union

from orrery.ingestion.auth.env import build_auth_config_from_env
from orrery.ingestion.auth.registry import AuthConfig
from orrery.ingestion.graph.client import OrreryGraph
from orrery.ingestion.graph.config import OrreryClientConfig

logger = logging.getLogger(__name__)


class BaseApi:
    graph: OrreryGraph

    def __init__(
        self,
        orrery_host: Optional[str] = None,
        orrery_token: Optional[str] = None,
        timeout: Optional[int] = None,
        graph: Optional[OrreryGraph] = None,
        orrery_auth: Optional[AuthConfig] = None,
    ):
        if graph:
            self.graph = graph
            return

        assert orrery_host is not None
        source = "static token" if orrery_token else None
        if orrery_token is None and orrery_auth is None:
            # Env-based OAuth (ORRERY_AUTH_TYPE) for an explicitly passed host.
            # Resolved here because the emitter only does it for the
            # "__from_env__" sentinel, and the Airflow circuit-breaker operators
            # have no other route: they build their config from
            # OrreryRestHook._get_config(), which cannot express an AuthConfig.
            orrery_auth = build_auth_config_from_env()
            source = f"env OAuth ({orrery_auth.type})" if orrery_auth else None
        elif orrery_auth is not None:
            source = f"declarative auth ({orrery_auth.type})"

        # With no credentials here, the emitter falls back to the
        # ORRERY_SYSTEM_CLIENT_* pair when it is set — an unauthenticated caller
        # silently acquiring system-user privileges is worth one log line.
        logger.info(
            "GraphQL client for %s authenticating with: %s",
            orrery_host,
            source
            or "no explicit credentials (system client if configured, else unauthenticated)",
        )

        self.graph = OrreryGraph(
            OrreryClientConfig(
                server=orrery_host,
                token=orrery_token,
                auth=orrery_auth,
                timeout_sec=timeout,
            )
        )

    def gen_filter(
        self, filters: Dict[str, Optional[str]]
    ) -> Optional[Dict[str, List[Dict[str, Union[str, List[str]]]]]]:
        filter_expression: Optional[
            Dict[str, List[Dict[str, Union[str, List[str]]]]]
        ] = None
        if not filters:
            return None

        filter_list: List[Dict[str, Union[str, List[str]]]] = []
        for key, value in filters.items():
            if value is None:
                continue
            filter_list.append({"field": key, "values": [value]})

        filter_expression = {"and": filter_list}
        return filter_expression
