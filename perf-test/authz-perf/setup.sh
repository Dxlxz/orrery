#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
VENV="${ROOT}/venv"
python3 -m venv "${VENV}"
"${VENV}/bin/pip" install -U pip
"${VENV}/bin/pip" install -r "${ROOT}/requirements.txt"
BENCHMARKS="${ROOT}/fixture/benchmarks.json"
test -f "${BENCHMARKS}" || {
  echo "missing ${BENCHMARKS}" >&2
  exit 1
}
"${VENV}/bin/python" -c "from orrery.utilities.graphql_query_adapter import QueryProjector" \
  || {
  echo "orrery>=1.6.0 required (graphql_query_adapter missing)" >&2
  exit 1
}
INSTALLED="$("${VENV}/bin/pip" show orrery | sed -n 's/^Version: //p')"
echo "[authz-perf] venv ready at ${VENV} (orrery ${INSTALLED})"
echo "[authz-perf] query specs: fixture/benchmarks.json#query_specs"
