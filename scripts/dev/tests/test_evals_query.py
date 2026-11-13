#!/usr/bin/env python3
"""
Pillar 4 — Query: agent eval suite for Orrery Common Operations CLI.

Tests that agents correctly use `orrery init`, `orrery graphql`, and
`orrery search` rather than ad-hoc alternatives (raw curl, manual config
files, source-grepping).

Usage:
    python3 scripts/dev/tests/test_evals_query.py              # all evals
    python3 scripts/dev/tests/test_evals_query.py --eval 11    # specific eval
    python3 scripts/dev/tests/test_evals_query.py --runs 3     # repeat N times
    python3 scripts/dev/tests/test_evals_query.py --json       # JSON output
    python3 scripts/dev/tests/test_evals_query.py --dry-run    # print prompts only
"""

import sys
from typing import Dict, Tuple

from _eval_lib import (
    Eval,
    SessionOutput,
    _bash,
    _fmt,
    _read,
    make_main,
)

# ---------------------------------------------------------------------------
# Graders
# ---------------------------------------------------------------------------


def _grade_init(s: SessionOutput) -> Tuple[bool, float, Dict[str, bool], str]:
    checks: Dict[str, bool] = {
        "used `orrery init`": _bash(s, r"orrery\s+init"),
        "did NOT manually write ~/.orreryenv": not _bash(
            s, r"(echo|cat|>>|>|Write|Edit).*orreryenv"
        ),
        "did NOT just export env vars without calling init": not (
            _bash(s, r"export\s+ORRERY_GMS") and not _bash(s, r"orrery\s+init")
        ),
    }
    score = sum(checks.values()) / len(checks)
    passed = checks["used `orrery init`"]
    return passed, score, checks, _fmt(checks)


def _grade_graphql_discovery(
    s: SessionOutput,
) -> Tuple[bool, float, Dict[str, bool], str]:
    checks: Dict[str, bool] = {
        "used `orrery graphql --list-operations`": _bash(
            s, r"orrery\s+graphql.*--list-operations"
        ),
        "used `--format json` for machine-readable output": _bash(
            s, r"orrery\s+graphql.*--format\s+json|--format\s+json.*orrery\s+graphql"
        ),
        "did NOT grep/find source .graphql files": not _bash(
            s, r"grep.*\.graphql|find.*\.graphql|rg.*\.graphql"
        ),
        "did NOT curl introspection endpoint manually": not _bash(
            s, r"curl.*__schema|curl.*introspect"
        ),
    }
    score = sum(checks.values()) / len(checks)
    passed = checks["used `orrery graphql --list-operations`"]
    return passed, score, checks, _fmt(checks)


def _grade_search_agent_context(
    s: SessionOutput,
) -> Tuple[bool, float, Dict[str, bool], str]:
    checks: Dict[str, bool] = {
        "discovered agent context (--agent-context, --help, or read file)": (
            _bash(
                s, r"orrery\s+search\s+--agent-context", r"orrery\s+search\s+--help"
            )
            or _read(s, "SEARCH_AGENT_CONTEXT.md")
        ),
        "used --dry-run before executing": _bash(s, r"--dry-run"),
        "used --filter for platform": _bash(s, r"--filter\s+platform=snowflake"),
        "used --urns-only or --format urns": _bash(s, r"--urns-only|--format\s+urns"),
        "set --limit explicitly": _bash(s, r"--limit\s+\d+"),
    }
    score = sum(checks.values()) / len(checks)
    return score >= 0.8, score, checks, _fmt(checks)


def _grade_graphql_mutation_dry_run(
    s: SessionOutput,
) -> Tuple[bool, float, Dict[str, bool], str]:
    checks: Dict[str, bool] = {
        "used `orrery graphql --query` or `--operation`": _bash(
            s, r"orrery\s+graphql.*--(query|operation)"
        ),
        "used `--dry-run` before the live mutation": _bash(s, r"--dry-run"),
        "used `--format json`": _bash(s, r"--format\s+json"),
        "did NOT use raw curl to hit /api/v2/graphql": not _bash(
            s, r"curl.*graphql|curl.*api/v2"
        ),
    }
    score = sum(checks.values()) / len(checks)
    # Dry-run discipline is the core check here — it's the whole point of this eval
    passed = (
        checks["used `orrery graphql --query` or `--operation`"]
        and checks["used `--dry-run` before the live mutation"]
    )
    return passed, score, checks, _fmt(checks)


# ---------------------------------------------------------------------------
# Eval definitions
# ---------------------------------------------------------------------------

DEFINED_EVALS = [
    Eval(
        id=11,
        name="init: use `orrery init`, not manual config or env vars",
        prompt=(
            "Configure the orrery CLI to connect to my local Orrery instance "
            "(running at http://localhost:8080 with username 'orrery' and password 'orrery')."
        ),
        grade=_grade_init,
    ),
    Eval(
        id=12,
        name="graphql discovery: use --list-operations, not source grep or curl",
        prompt=(
            "What GraphQL operations are available on my local Orrery instance? "
            "Give me a machine-readable list."
        ),
        grade=_grade_graphql_discovery,
    ),
    Eval(
        id=13,
        name="graphql mutation: --dry-run before executing a mutation with variables",
        prompt=(
            "Use the Orrery graphql CLI to add the tag `urn:li:tag:PII` to the dataset "
            "`urn:li:dataset:(urn:li:dataPlatform:snowflake,mydb.mytable,PROD)`. "
            "Verify the request before executing it."
        ),
        grade=_grade_graphql_mutation_dry_run,
    ),
    Eval(
        id=14,
        name="search CLI: discover --agent-context, then use best practices",
        prompt=(
            "Use the Orrery CLI to find all Snowflake datasets in the PROD "
            "environment. I only need the URNs. "
            "Start by reading the search command's agent context, then verify "
            "your query with a dry run before executing."
        ),
        grade=_grade_search_agent_context,
    ),
]

# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

main = make_main(
    DEFINED_EVALS,
    description="Pillar 4 — Query: agent evals for Orrery Common Operations CLI",
)

if __name__ == "__main__":
    sys.exit(main())
